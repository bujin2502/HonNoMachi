import { initializeApp } from "firebase-admin/app";
import {
  DocumentReference,
  FieldValue,
  Timestamp,
  Transaction,
  getFirestore,
} from "firebase-admin/firestore";
import { logger } from "firebase-functions";
import { defineSecret } from "firebase-functions/params";
import { onDocumentUpdated } from "firebase-functions/v2/firestore";
import { HttpsError, onCall, onRequest } from "firebase-functions/v2/https";
import { onSchedule } from "firebase-functions/v2/scheduler";
import { BookData, sendBuyerOrderEmail, sendSellerOrderEmail } from "./emailService";
import { createHash } from "node:crypto";
import Stripe from "stripe";

initializeApp();

const db = getFirestore();
const stripeSecretKey = defineSecret("STRIPE_SECRET_KEY");
const stripeWebhookSecret = defineSecret("STRIPE_WEBHOOK_SECRET");
const stripeWalletWebhookSecret = defineSecret("STRIPE_WALLET_WEBHOOK_SECRET");
const gmailPass = defineSecret("GMAIL_PASS");

const DEFAULT_RESERVATION_TTL_MINUTES = 15;
const MAX_RESERVATION_TTL_MINUTES = 60;
const MAX_CART_SIZE = 20;
const CENTS_MULTIPLIER = 100;
// Exchange rates: units of target currency per 1 unit of source currency.
// Kept in sync with the Android app's USD_TO_EUR_RATE constant (1 EUR = 1.18 USD).
const EXCHANGE_RATE_TABLE: Record<string, Record<string, number>> = {
  eur: { usd: 1.18, eur: 1.0 },
  usd: { eur: 1.0 / 1.18, usd: 1.0 },
};

function convertMinorBetweenCurrencies(amount: number, from: string, to: string): number {
  if (from === to) return amount;
  const rate = EXCHANGE_RATE_TABLE[from]?.[to];
  if (!rate) return 0;
  return Math.floor(amount * rate);
}
const MIN_WALLET_TOPUP_MINOR = 100;
const MAX_WALLET_TOPUP_MINOR = 100000;
const WALLET_DEFAULT_CURRENCY = "eur";
const WALLET_TOPUP_PURPOSE = "wallet_topup";
const CHECKOUT_PAYMENT_PURPOSE = "checkout_payment";
const MAX_STRIPE_IDEMPOTENCY_KEY_LENGTH = 255;

const CHECKOUT_STATUS_PENDING = "PENDING";
const CHECKOUT_STATUS_COMPLETED = "COMPLETED";
const CHECKOUT_STATUS_EXPIRED = "EXPIRED";
const CHECKOUT_STATUS_CANCELED = "CANCELED";
const CHECKOUT_FLOW_PAYMENT_INTENT = "PAYMENT_INTENT";
const CHECKOUT_FLOW_WALLET_ONLY = "WALLET_ONLY";

const RESERVATION_STATUS_PENDING = "PENDING";
const RESERVATION_STATUS_CONFIRMED = "CONFIRMED";
const RESERVATION_STATUS_EXPIRED = "EXPIRED";
const RESERVATION_STATUS_CANCELED = "CANCELED";

type CheckoutReleaseStatus =
  | typeof CHECKOUT_STATUS_EXPIRED
  | typeof CHECKOUT_STATUS_CANCELED;

const WALLET_TOPUP_STATUS_PENDING = "PENDING";
const WALLET_TOPUP_STATUS_SUCCEEDED = "SUCCEEDED";
const WALLET_TOPUP_STATUS_FAILED = "FAILED";
const WALLET_TOPUP_STATUS_CANCELED = "CANCELED";
const WALLET_TOPUP_STATUS_REFUNDED = "REFUNDED";

type ReservationDraft = {
  reservationRef: DocumentReference;
  reservationId: string;
  bookRef: DocumentReference;
  bookId: string;
  sellerUid: string;
  title: string;
  unitAmount: number;
  currency: string;
};

export const createCheckoutPaymentIntent = onCall(
  {
    region: "us-central1",
    secrets: [stripeSecretKey],
    timeoutSeconds: 60,
    memory: "256MiB",
  },
  async (request) => {
    const buyerUid = request.auth?.uid;
    if (!buyerUid) {
      throw new HttpsError("unauthenticated", "User must be authenticated.");
    }

    if (request.auth?.token?.email_verified !== true) {
      throw new HttpsError(
        "permission-denied",
        "Email must be verified before checkout.",
      );
    }

    const payload = toRecord(request.data);
    const reservationTtlMinutes = readReservationTtlMinutes(payload);

    const cartSnapshot = await db
      .collection("users")
      .doc(buyerUid)
      .collection("cart")
      .get();

    if (cartSnapshot.empty) {
      throw new HttpsError("failed-precondition", "Cart is empty.");
    }

    const cartBookIds = Array.from(
      new Set(
        cartSnapshot.docs
          .map((doc) => asString(doc.data().bookId))
          .filter((bookId): bookId is string => Boolean(bookId)),
      ),
    );

    if (cartBookIds.length === 0) {
      throw new HttpsError("failed-precondition", "Cart contains no valid books.");
    }

    const transactionResult = await validateAndExtendCartReservations(
      buyerUid,
      cartBookIds,
      reservationTtlMinutes,
    );

    const totalAmountMinor = transactionResult.reservations.reduce(
      (total, reservation) => total + reservation.unitAmount,
      0,
    );
    const currency = transactionResult.reservations[0]?.currency ?? null;
    if (!currency || totalAmountMinor <= 0) {
      await rollbackReservationState(transactionResult.reservations);
      throw new HttpsError(
        "failed-precondition",
        "Unable to create payment intent for empty or invalid cart total.",
      );
    }

    const stripe = new Stripe(stripeSecretKey.value());
    let createdPaymentIntentId: string | null = null;
    let walletContributionMinor = 0;
    let walletDeductionMinor = 0;
    let persistedCheckoutSessionId: string | null = null;

    try {
      const walletResult = await reserveWalletContributionForCheckout({
        userId: buyerUid,
        currency,
        requestedAmountMinor: totalAmountMinor,
      });
      walletContributionMinor = walletResult.contributionMinor;
      walletDeductionMinor = walletResult.walletDeductionMinor;

      const remainingStripeAmountMinor = totalAmountMinor - walletContributionMinor;

      if (remainingStripeAmountMinor <= 0) {
        const checkoutId = db.collection("checkoutSessions").doc().id;
        persistedCheckoutSessionId = checkoutId;

        await persistCheckoutSession({
          buyerUid,
          checkoutId,
          paymentIntentId: null,
          reservations: transactionResult.reservations,
          expiresAt: transactionResult.expiresAt,
          totalAmountMinor,
          stripeAmountMinor: 0,
          walletContributionMinor,
          walletDeductionMinor,
          currency,
          checkoutFlow: CHECKOUT_FLOW_WALLET_ONLY,
        });

        await finalizeCheckoutSession(
          {
            id: checkoutId,
            metadata: {
              buyerUid,
              reservationIds: transactionResult.reservationIds.join(","),
            },
            payment_status: "wallet_settled",
            payment_intent: null,
          } as unknown as Stripe.Checkout.Session,
        );

        return {
          checkoutId,
          paymentIntentId: null,
          clientSecret: null,
          amountMinor: 0,
          totalAmountMinor,
          walletContributionMinor,
          requiresPaymentSheet: false,
          checkoutCompleted: true,
          currency,
          expiresAt: transactionResult.expiresAt.toDate().toISOString(),
          reservationIds: transactionResult.reservationIds,
        };
      }

      const paymentIntent = await stripe.paymentIntents.create({
        amount: remainingStripeAmountMinor,
        currency,
        automatic_payment_methods: {
          enabled: true,
        },
        metadata: {
          purpose: CHECKOUT_PAYMENT_PURPOSE,
          buyerUid,
          reservationIds: transactionResult.reservationIds.join(","),
          walletContributionMinor: String(walletContributionMinor),
        },
      });
      createdPaymentIntentId = paymentIntent.id;

      if (!paymentIntent.client_secret) {
        throw new HttpsError(
          "internal",
          "Stripe PaymentIntent is missing client_secret.",
        );
      }

      persistedCheckoutSessionId = paymentIntent.id;
      await persistCheckoutSession({
        buyerUid,
        checkoutId: paymentIntent.id,
        paymentIntentId: paymentIntent.id,
        reservations: transactionResult.reservations,
        expiresAt: transactionResult.expiresAt,
        totalAmountMinor,
        stripeAmountMinor: remainingStripeAmountMinor,
        walletContributionMinor,
        walletDeductionMinor,
        currency,
        checkoutFlow: CHECKOUT_FLOW_PAYMENT_INTENT,
      });

      return {
        checkoutId: paymentIntent.id,
        paymentIntentId: paymentIntent.id,
        clientSecret: paymentIntent.client_secret,
        amountMinor: remainingStripeAmountMinor,
        totalAmountMinor,
        walletContributionMinor,
        requiresPaymentSheet: true,
        checkoutCompleted: false,
        currency,
        expiresAt: transactionResult.expiresAt.toDate().toISOString(),
        reservationIds: transactionResult.reservationIds,
      };
    } catch (error) {
      if (persistedCheckoutSessionId) {
        try {
          await releaseCheckoutSession({
            sessionId: persistedCheckoutSessionId,
            releaseStatus: CHECKOUT_STATUS_CANCELED,
            fallbackBuyerUid: buyerUid,
            fallbackReservationIds: transactionResult.reservationIds,
          });
        } catch (releaseError) {
          logger.error("Failed to release checkout session after checkout error.", {
            sessionId: persistedCheckoutSessionId,
            releaseError,
          });
        }
      } else if (walletContributionMinor > 0) {
        try {
          await refundWalletContributionForCheckout({
            userId: buyerUid,
            amountMinor: walletDeductionMinor,
          });
        } catch (walletRollbackError) {
          logger.error(
            "Failed to rollback wallet contribution after checkout error.",
            walletRollbackError,
          );
        }
      }

      if (createdPaymentIntentId) {
        await cancelStripePaymentIntentSafely(stripe, createdPaymentIntentId);
      }

      throw toHttpsError(error);
    }
  },
);

export const addToCartAndReserve = onCall(
  {
    region: "us-central1",
    timeoutSeconds: 30,
    memory: "256MiB",
  },
  async (request) => {
    const buyerUid = request.auth?.uid;
    if (!buyerUid) {
      throw new HttpsError("unauthenticated", "User must be authenticated.");
    }

    const payload = toRecord(request.data);
    const bookId = asString(payload.bookId);
    if (!bookId) {
      throw new HttpsError("invalid-argument", "Missing bookId.");
    }

    const expiresAt = await db.runTransaction(async (transaction) => {
      const now = Timestamp.now();
      const newExpiresAt = Timestamp.fromMillis(
        now.toMillis() + DEFAULT_RESERVATION_TTL_MINUTES * 60_000,
      );

      const bookRef = db.collection("books").doc(bookId);
      const bookSnapshot = await transaction.get(bookRef);
      if (!bookSnapshot.exists) {
        throw new HttpsError("not-found", "Book not found.");
      }

      const bookData = toRecord(bookSnapshot.data());
      const sellerUid = asString(bookData.userID);
      if (!sellerUid) {
        throw new HttpsError("failed-precondition", "Book has no valid seller.");
      }
      if (sellerUid === buyerUid) {
        throw new HttpsError("failed-precondition", "Cannot purchase your own book.");
      }

      const status = asString(bookData.status);
      const reservedByUid = asString(bookData.reservedByUid);
      const alreadyReservedByBuyer =
        status === "RESERVED" && reservedByUid === buyerUid;

      if (!alreadyReservedByBuyer && status !== "AVAILABLE") {
        throw new HttpsError("failed-precondition", "Book is not available.");
      }

      const price = asNumber(bookData.price);
      if (price === null || price <= 0) {
        throw new HttpsError("failed-precondition", "Book has invalid price.");
      }
      const unitAmount = Math.round(price * CENTS_MULTIPLIER);
      if (unitAmount <= 0) {
        throw new HttpsError("failed-precondition", "Book has invalid amount.");
      }

      const currencyRaw = asString(bookData.priceCurrency) ?? "EUR";
      const currency = currencyRaw.toUpperCase();
      const reservationCurrency = currencyRaw.toLowerCase();
      const title = asString(bookData.title) ?? "Book";
      const authors = bookData.authors;
      const author =
        Array.isArray(authors) ? asString(authors[0]) ?? "Unknown Author" : "Unknown Author";
      const imageUrls = bookData.imageUrls;
      const imageUrl =
        Array.isArray(imageUrls) ? asString(imageUrls[0]) ?? null : null;

      // Read existing cart items
      const cartRef = db.collection("users").doc(buyerUid).collection("cart");
      const cartSnapshot = await transaction.get(cartRef);

      const existingCartBookIds = cartSnapshot.docs
        .map((doc) => doc.id)
        .filter((id) => id !== bookId);

      if (!alreadyReservedByBuyer && existingCartBookIds.length >= MAX_CART_SIZE) {
        throw new HttpsError(
          "failed-precondition",
          `Cart cannot contain more than ${MAX_CART_SIZE} items.`,
        );
      }

      // Read existing cart books for TTL refresh
      const existingBooks: Array<{
        bookRef: DocumentReference;
        reservationId: string | null;
      }> = [];
      for (const existingBookId of existingCartBookIds) {
        const existingBookRef = db.collection("books").doc(existingBookId);
        const existingBookSnap = await transaction.get(existingBookRef);
        if (existingBookSnap.exists) {
          const existingBookData = toRecord(existingBookSnap.data());
          if (asString(existingBookData.reservedByUid) === buyerUid) {
            existingBooks.push({
              bookRef: existingBookRef,
              reservationId: asString(existingBookData.reservationId),
            });
          }
        }
      }

      // Writes
      if (!alreadyReservedByBuyer) {
        const reservationRef = db.collection("reservations").doc();
        transaction.set(reservationRef, {
          bookId,
          buyerUid,
          sellerUid,
          status: RESERVATION_STATUS_PENDING,
          createdAt: now,
          expiresAt: newExpiresAt,
          checkoutSessionId: null,
          unitAmount,
          currency: reservationCurrency,
        });

        transaction.update(bookRef, {
          status: "RESERVED",
          reservedByUid: buyerUid,
          reservationId: reservationRef.id,
          reservationExpiresAt: newExpiresAt,
        });

        const cartItemRef = cartRef.doc(bookId);
        transaction.set(cartItemRef, {
          bookId,
          title,
          author,
          price,
          currency,
          imageUrl,
          addedAt: now,
        });
      } else {
        // Idempotent re-add: refresh TTL on this book
        const existingReservationId = asString(bookData.reservationId);
        transaction.update(bookRef, {
          reservationExpiresAt: newExpiresAt,
        });
        if (existingReservationId) {
          const existingReservationRef = db
            .collection("reservations")
            .doc(existingReservationId);
          transaction.update(existingReservationRef, {
            expiresAt: newExpiresAt,
          });
        }
      }

      // Refresh TTL on ALL existing cart items
      for (const existing of existingBooks) {
        transaction.update(existing.bookRef, {
          reservationExpiresAt: newExpiresAt,
        });
        if (existing.reservationId) {
          const reservationRef = db
            .collection("reservations")
            .doc(existing.reservationId);
          transaction.update(reservationRef, {
            expiresAt: newExpiresAt,
          });
        }
      }

      return newExpiresAt;
    });

    return {
      success: true,
      expiresAt: expiresAt.toDate().toISOString(),
    };
  },
);

export const removeFromCartAndRelease = onCall(
  {
    region: "us-central1",
    timeoutSeconds: 30,
  },
  async (request) => {
    const buyerUid = request.auth?.uid;
    if (!buyerUid) {
      throw new HttpsError("unauthenticated", "User must be authenticated.");
    }

    const payload = toRecord(request.data);
    const bookId = asString(payload.bookId);
    if (!bookId) {
      throw new HttpsError("invalid-argument", "Missing bookId.");
    }

    await db.runTransaction(async (transaction) => {
      const now = Timestamp.now();
      const bookRef = db.collection("books").doc(bookId);
      const bookSnapshot = await transaction.get(bookRef);

      if (!bookSnapshot.exists) {
        throw new HttpsError("not-found", "Book not found.");
      }

      const bookData = toRecord(bookSnapshot.data());
      const status = asString(bookData.status);
      const reservedByUid = asString(bookData.reservedByUid);
      const reservationId = asString(bookData.reservationId);

      if (status !== "RESERVED" || reservedByUid !== buyerUid) {
        throw new HttpsError(
          "failed-precondition",
          "Book is not reserved by you.",
        );
      }

      transaction.update(bookRef, {
        status: "AVAILABLE",
        reservedByUid: FieldValue.delete(),
        reservationId: FieldValue.delete(),
        reservationExpiresAt: FieldValue.delete(),
      });

      if (reservationId) {
        const reservationRef = db.collection("reservations").doc(reservationId);
        transaction.update(reservationRef, {
          status: RESERVATION_STATUS_CANCELED,
          canceledAt: now,
          updatedAt: now,
        });
      }

      const cartItemRef = db
        .collection("users")
        .doc(buyerUid)
        .collection("cart")
        .doc(bookId);
      transaction.delete(cartItemRef);
    });

    return { success: true };
  },
);

export const createWalletTopupIntent = onCall(
  {
    region: "us-central1",
    secrets: [stripeSecretKey],
    timeoutSeconds: 60,
    memory: "256MiB",
  },
  async (request) => {
    try {
      const userId = request.auth?.uid;
      if (!userId) {
        throw new HttpsError("unauthenticated", "User must be authenticated.");
      }

      if (request.auth?.token?.email_verified !== true) {
        throw new HttpsError(
          "permission-denied",
          "Email must be verified before wallet topup.",
        );
      }

      const payload = toRecord(request.data);
      const amountMinor = readWalletTopupAmountMinor(payload);
      const currency = readWalletTopupCurrency(payload);
      const idempotencyKey = readWalletTopupIdempotencyKey(payload);
      const idempotencyDocId = buildWalletTopupIdempotencyDocId(
        userId,
        idempotencyKey,
      );

      const stripeSecret = stripeSecretKey.value();
      if (!stripeSecret) {
        throw new HttpsError(
          "failed-precondition",
          "Stripe secret key is not configured.",
        );
      }
      const stripe = new Stripe(stripeSecret);
      const idempotencyRef = db
        .collection("walletTopupIdempotency")
        .doc(idempotencyDocId);

      const idempotencySnapshot = await idempotencyRef.get();
      if (idempotencySnapshot.exists) {
        const topupId = asString(idempotencySnapshot.data()?.topupId);
        if (!topupId) {
          throw new HttpsError(
            "internal",
            "Idempotency mapping is invalid for wallet topup.",
          );
        }

        return loadWalletTopupIntentResponse({
          stripe,
          userId,
          topupId,
        });
      }

      const topupRef = db.collection("walletTopups").doc(idempotencyDocId);
      const stripeIdempotencyKey = buildStripeWalletIdempotencyKey(
        userId,
        idempotencyKey,
      );

      const paymentIntent = await stripe.paymentIntents.create(
        {
          amount: amountMinor,
          currency,
          automatic_payment_methods: {
            enabled: true,
          },
          metadata: {
            purpose: WALLET_TOPUP_PURPOSE,
            topupId: topupRef.id,
            userId,
          },
        },
        {
          idempotencyKey: stripeIdempotencyKey,
        },
      );

      if (!paymentIntent.client_secret) {
        throw new HttpsError("internal", "Stripe PaymentIntent missing client_secret.");
      }

      let resolvedTopupId = topupRef.id;
      await db.runTransaction(async (transaction) => {
        const existingIdempotencySnapshot = await transaction.get(idempotencyRef);
        if (existingIdempotencySnapshot.exists) {
          const existingTopupId = asString(existingIdempotencySnapshot.data()?.topupId);
          if (existingTopupId) {
            resolvedTopupId = existingTopupId;
          }
          return;
        }

        // Firestore transactions require all reads to happen before any writes.
        const walletRef = db.collection("wallets").doc(userId);
        const walletSnapshot = await transaction.get(walletRef);

        transaction.set(topupRef, {
          userId,
          paymentIntentId: paymentIntent.id,
          status: WALLET_TOPUP_STATUS_PENDING,
          amountMinor,
          currency,
          idempotencyKey,
          createdAt: FieldValue.serverTimestamp(),
          updatedAt: FieldValue.serverTimestamp(),
          settledAt: null,
          failureCode: null,
          failureMessage: null,
        });

        transaction.set(idempotencyRef, {
          userId,
          idempotencyKey,
          topupId: topupRef.id,
          paymentIntentId: paymentIntent.id,
          createdAt: FieldValue.serverTimestamp(),
        });

        if (!walletSnapshot.exists) {
          transaction.set(walletRef, {
            currency,
            balanceMinor: 0,
            updatedAt: FieldValue.serverTimestamp(),
          });
        }
      });

      return loadWalletTopupIntentResponse({
        stripe,
        userId,
        topupId: resolvedTopupId,
      });
    } catch (error) {
      logger.error("createWalletTopupIntent failed.", error);
      throw toHttpsError(error);
    }
  },
);

export const stripeWalletWebhook = onRequest(
  {
    region: "us-central1",
    timeoutSeconds: 60,
    memory: "256MiB",
    secrets: [stripeSecretKey, stripeWalletWebhookSecret],
  },
  async (req, res) => {
    if (req.method !== "POST") {
      res.status(405).send("Method Not Allowed");
      return;
    }

    const signature = readStripeSignatureHeader(req.headers["stripe-signature"]);
    if (!signature) {
      res.status(400).send("Missing Stripe signature header.");
      return;
    }

    const webhookSecret = stripeWalletWebhookSecret.value();
    if (!webhookSecret) {
      logger.error("Missing STRIPE_WALLET_WEBHOOK_SECRET configuration.");
      res.status(500).send("Webhook secret is not configured.");
      return;
    }

    const stripe = new Stripe(stripeSecretKey.value());
    const rawBody = readRawRequestBody(req);

    let event: Stripe.Event;
    try {
      event = stripe.webhooks.constructEvent(rawBody, signature, webhookSecret);
    } catch (error) {
      logger.warn("Stripe wallet webhook signature verification failed.", error);
      res.status(400).send("Invalid webhook signature.");
      return;
    }

    try {
      await processWalletWebhookEvent(event);
      res.status(200).json({ received: true });
    } catch (error) {
      logger.error("Stripe wallet webhook processing failed.", error);
      res.status(500).send("Webhook processing error.");
    }
  },
);

export const stripeWebhook = onRequest(
  {
    region: "us-central1",
    timeoutSeconds: 60,
    memory: "256MiB",
    secrets: [stripeSecretKey, stripeWebhookSecret],
  },
  async (req, res) => {
    if (req.method !== "POST") {
      res.status(405).send("Method Not Allowed");
      return;
    }

    const signature = readStripeSignatureHeader(req.headers["stripe-signature"]);
    if (!signature) {
      res.status(400).send("Missing Stripe signature header.");
      return;
    }

    const webhookSecret = stripeWebhookSecret.value();
    if (!webhookSecret) {
      logger.error("Missing STRIPE_WEBHOOK_SECRET configuration.");
      res.status(500).send("Webhook secret is not configured.");
      return;
    }

    const stripe = new Stripe(stripeSecretKey.value());
    const rawBody = readRawRequestBody(req);

    let event: Stripe.Event;
    try {
      event = stripe.webhooks.constructEvent(rawBody, signature, webhookSecret);
    } catch (error) {
      logger.warn("Stripe webhook signature verification failed.", error);
      res.status(400).send("Invalid webhook signature.");
      return;
    }

    try {
      switch (event.type) {
        case "payment_intent.succeeded": {
          const paymentIntent = event.data.object as Stripe.PaymentIntent;
          if (!isCheckoutPaymentIntent(paymentIntent)) {
            logger.info("Checkout payment_intent.succeeded ignored.", {
              eventId: event.id,
              paymentIntentId: paymentIntent.id,
            });
            break;
          }
          await finalizeCheckoutPaymentIntent(paymentIntent);
          break;
        }
        case "payment_intent.payment_failed":
        case "payment_intent.canceled": {
          const paymentIntent = event.data.object as Stripe.PaymentIntent;
          if (!isCheckoutPaymentIntent(paymentIntent)) {
            logger.info("Checkout payment_intent terminal event ignored.", {
              eventId: event.id,
              eventType: event.type,
              paymentIntentId: paymentIntent.id,
            });
            break;
          }
          await releaseCheckoutSession({
            sessionId: paymentIntent.id,
            releaseStatus: CHECKOUT_STATUS_CANCELED,
            fallbackBuyerUid: asString(paymentIntent.metadata?.buyerUid),
            fallbackReservationIds: parseReservationIds(
              paymentIntent.metadata?.reservationIds,
            ),
          });
          break;
        }
        default:
          logger.info("Stripe event ignored.", { eventType: event.type });
      }

      res.status(200).json({ received: true });
    } catch (error) {
      logger.error("Stripe webhook processing failed.", error);
      res.status(500).send("Webhook processing error.");
    }
  },
);

export const cancelCheckout = onCall(
  {
    region: "us-central1",
    secrets: [stripeSecretKey],
  },
  async (request) => {
    const buyerUid = request.auth?.uid;
    if (!buyerUid) {
      throw new HttpsError("unauthenticated", "User must be authenticated.");
    }

    const payload = toRecord(request.data);
    const checkoutId = asString(payload.checkoutId);
    if (!checkoutId) {
      throw new HttpsError("invalid-argument", "Missing checkoutId.");
    }

    const checkoutSessionRef = db.collection("checkoutSessions").doc(checkoutId);
    const checkoutSnapshot = await checkoutSessionRef.get();
    if (!checkoutSnapshot.exists) {
      throw new HttpsError("not-found", "Checkout session not found.");
    }
    const checkoutData = toRecord(checkoutSnapshot.data());
    if (asString(checkoutData.buyerUid) !== buyerUid) {
      throw new HttpsError("permission-denied", "Not your checkout session.");
    }

    await releaseCheckoutSession({
      sessionId: checkoutId,
      releaseStatus: CHECKOUT_STATUS_CANCELED,
      fallbackBuyerUid: buyerUid,
      fallbackReservationIds: parseReservationIds(checkoutData.reservationIds),
    });

    const checkoutFlow = asString(checkoutData.checkoutFlow);
    if (checkoutFlow === CHECKOUT_FLOW_PAYMENT_INTENT) {
      const stripe = new Stripe(stripeSecretKey.value());
      const paymentIntentId = asString(checkoutData.paymentIntentId) ?? checkoutId;
      await cancelStripePaymentIntentSafely(stripe, paymentIntentId);
    }

    return { canceled: true };
  },
);

export const releaseExpiredCheckoutSessions = onSchedule(
  {
    region: "us-central1",
    schedule: "every 5 minutes",
    timeZone: "Etc/UTC",
    timeoutSeconds: 60,
    memory: "256MiB",
    secrets: [stripeSecretKey],
  },
  async () => {
    const now = Timestamp.now();
    const expiredSessions = await db
      .collection("checkoutSessions")
      .where("status", "==", CHECKOUT_STATUS_PENDING)
      .where("expiresAt", "<=", now)
      .limit(100)
      .get();

    if (expiredSessions.empty) {
      return;
    }

    const stripe = new Stripe(stripeSecretKey.value());
    let processedCount = 0;

    for (const doc of expiredSessions.docs) {
      const checkoutData = toRecord(doc.data());
      const status = asString(checkoutData.status);
      if (status !== CHECKOUT_STATUS_PENDING) {
        continue;
      }
      const checkoutFlow = asString(checkoutData.checkoutFlow);
      const isPaymentIntentFlow = checkoutFlow === CHECKOUT_FLOW_PAYMENT_INTENT;
      const paymentIntentId = asString(checkoutData.paymentIntentId) ?? doc.id;

      await releaseCheckoutSession({
        sessionId: doc.id,
        releaseStatus: CHECKOUT_STATUS_EXPIRED,
        fallbackBuyerUid: asString(checkoutData.buyerUid),
        fallbackReservationIds: parseReservationIds(checkoutData.reservationIds),
      });

      if (isPaymentIntentFlow) {
        await cancelStripePaymentIntentSafely(stripe, paymentIntentId);
      }
      processedCount++;
    }

    logger.info("Expired checkout session release job completed.", {
      processedCount,
    });
  },
);

export const releaseExpiredCartReservations = onSchedule(
  {
    region: "us-central1",
    schedule: "every 5 minutes",
    timeZone: "Etc/UTC",
    timeoutSeconds: 60,
    memory: "256MiB",
  },
  async () => {
    const now = Timestamp.now();
    const expiredBooks = await db
      .collection("books")
      .where("status", "==", "RESERVED")
      .where("reservationExpiresAt", "<=", now)
      .limit(100)
      .get();

    if (expiredBooks.empty) {
      return;
    }

    const batch = db.batch();
    let processedCount = 0;

    for (const doc of expiredBooks.docs) {
      const bookData = toRecord(doc.data());
      const reservedByUid = asString(bookData.reservedByUid);
      const reservationId = asString(bookData.reservationId);

      batch.update(doc.ref, {
        status: "AVAILABLE",
        reservedByUid: FieldValue.delete(),
        reservationId: FieldValue.delete(),
        reservationExpiresAt: FieldValue.delete(),
      });

      if (reservationId) {
        const reservationRef = db.collection("reservations").doc(reservationId);
        batch.update(reservationRef, {
          status: RESERVATION_STATUS_EXPIRED,
          expiredAt: now,
          updatedAt: now,
        });
      }

      if (reservedByUid) {
        const cartItemRef = db
          .collection("users")
          .doc(reservedByUid)
          .collection("cart")
          .doc(doc.id);
        batch.delete(cartItemRef);
      }

      processedCount++;
    }

    await batch.commit();

    if (processedCount > 0) {
      logger.info("Expired cart reservations released.", { processedCount });
    }
  },
);

async function reserveBooksForCheckout(
  buyerUid: string,
  cartBookIds: string[],
  reservationTtlMinutes: number,
): Promise<{
  reservations: ReservationDraft[];
  reservationIds: string[];
  expiresAt: Timestamp;
}> {
  return db.runTransaction(async (transaction) => {
    const now = Timestamp.now();
    const expiresAt = Timestamp.fromMillis(
      now.toMillis() + reservationTtlMinutes * 60_000,
    );
    const reservations: ReservationDraft[] = [];

    for (const bookId of cartBookIds) {
      const bookRef = db.collection("books").doc(bookId);
      const bookSnapshot = await transaction.get(bookRef);

      if (!bookSnapshot.exists) {
        throw new HttpsError(
          "failed-precondition",
          `Book ${bookId} no longer exists.`,
        );
      }

      const bookData = toRecord(bookSnapshot.data());
      const sellerUid = asString(bookData.userID);
      if (!sellerUid) {
        throw new HttpsError(
          "failed-precondition",
          `Book ${bookId} has no valid seller.`,
        );
      }
      if (sellerUid === buyerUid) {
        throw new HttpsError(
          "failed-precondition",
          "Cannot purchase your own book listing.",
        );
      }

      const status = asString(bookData.status);
      const reservedByUid = asString(bookData.reservedByUid);
      const reservationId = asString(bookData.reservationId);
      if (
        status !== "AVAILABLE" ||
        Boolean(reservedByUid) ||
        Boolean(reservationId)
      ) {
        throw new HttpsError(
          "failed-precondition",
          `Book ${bookId} is not available for checkout.`,
        );
      }

      const title = asString(bookData.title) ?? "Book";
      const price = asNumber(bookData.price);
      if (price === null || price <= 0) {
        throw new HttpsError(
          "failed-precondition",
          `Book ${bookId} has invalid price.`,
        );
      }
      const unitAmount = Math.round(price * CENTS_MULTIPLIER);
      if (unitAmount <= 0) {
        throw new HttpsError(
          "failed-precondition",
          `Book ${bookId} has invalid amount.`,
        );
      }

      const currencyRaw = asString(bookData.priceCurrency) ?? "EUR";
      const currency = currencyRaw.toLowerCase();
      if (!/^[a-z]{3}$/.test(currency)) {
        throw new HttpsError(
          "failed-precondition",
          `Book ${bookId} has invalid currency.`,
        );
      }

      const reservationRef = db.collection("reservations").doc();
      const draft: ReservationDraft = {
        reservationRef,
        reservationId: reservationRef.id,
        bookRef,
        bookId,
        sellerUid,
        title,
        unitAmount,
        currency,
      };
      reservations.push(draft);
    }

    const uniqueCurrencies = Array.from(
      new Set(reservations.map((reservation) => reservation.currency)),
    );
    if (uniqueCurrencies.length > 1) {
      throw new HttpsError(
        "failed-precondition",
        "All cart items must use the same currency.",
      );
    }

    for (const reservation of reservations) {
      transaction.set(reservation.reservationRef, {
        bookId: reservation.bookId,
        buyerUid,
        sellerUid: reservation.sellerUid,
        status: RESERVATION_STATUS_PENDING,
        createdAt: now,
        expiresAt,
        checkoutSessionId: null,
        unitAmount: reservation.unitAmount,
        currency: reservation.currency,
      });

      transaction.update(reservation.bookRef, {
        status: "RESERVED",
        reservedByUid: buyerUid,
        reservationId: reservation.reservationId,
        reservationExpiresAt: expiresAt,
      });
    }

    return {
      reservations,
      reservationIds: reservations.map((reservation) => reservation.reservationId),
      expiresAt,
    };
  });
}

async function validateAndExtendCartReservations(
  buyerUid: string,
  cartBookIds: string[],
  reservationTtlMinutes: number,
): Promise<{
  reservations: ReservationDraft[];
  reservationIds: string[];
  expiresAt: Timestamp;
}> {
  return db.runTransaction(async (transaction) => {
    const now = Timestamp.now();
    const expiresAt = Timestamp.fromMillis(
      now.toMillis() + reservationTtlMinutes * 60_000,
    );
    const reservations: ReservationDraft[] = [];

    for (const bookId of cartBookIds) {
      const bookRef = db.collection("books").doc(bookId);
      const bookSnapshot = await transaction.get(bookRef);

      if (!bookSnapshot.exists) {
        throw new HttpsError(
          "failed-precondition",
          `Book ${bookId} no longer exists.`,
        );
      }

      const bookData = toRecord(bookSnapshot.data());
      const status = asString(bookData.status);
      const reservedByUid = asString(bookData.reservedByUid);
      const reservationId = asString(bookData.reservationId);

      if (status !== "RESERVED" || reservedByUid !== buyerUid || !reservationId) {
        throw new HttpsError(
          "failed-precondition",
          `Book ${bookId} is not reserved by you. Please add it to cart first.`,
        );
      }

      const sellerUid = asString(bookData.userID);
      if (!sellerUid) {
        throw new HttpsError(
          "failed-precondition",
          `Book ${bookId} has no valid seller.`,
        );
      }
      if (sellerUid === buyerUid) {
        throw new HttpsError(
          "failed-precondition",
          "Cannot purchase your own book listing.",
        );
      }

      const title = asString(bookData.title) ?? "Book";
      const price = asNumber(bookData.price);
      if (price === null || price <= 0) {
        throw new HttpsError(
          "failed-precondition",
          `Book ${bookId} has invalid price.`,
        );
      }
      const unitAmount = Math.round(price * CENTS_MULTIPLIER);
      if (unitAmount <= 0) {
        throw new HttpsError(
          "failed-precondition",
          `Book ${bookId} has invalid amount.`,
        );
      }

      const currencyRaw = asString(bookData.priceCurrency) ?? "EUR";
      const currency = currencyRaw.toLowerCase();
      if (!/^[a-z]{3}$/.test(currency)) {
        throw new HttpsError(
          "failed-precondition",
          `Book ${bookId} has invalid currency.`,
        );
      }

      const reservationRef = db.collection("reservations").doc(reservationId);

      const draft: ReservationDraft = {
        reservationRef,
        reservationId,
        bookRef,
        bookId,
        sellerUid,
        title,
        unitAmount,
        currency,
      };
      reservations.push(draft);
    }

    const uniqueCurrencies = Array.from(
      new Set(reservations.map((reservation) => reservation.currency)),
    );
    if (uniqueCurrencies.length > 1) {
      throw new HttpsError(
        "failed-precondition",
        "All cart items must use the same currency.",
      );
    }

    for (const reservation of reservations) {
      // Extend TTL on both reservation and book after all reads are complete.
      transaction.update(reservation.bookRef, {
        reservationExpiresAt: expiresAt,
      });
      transaction.update(reservation.reservationRef, {
        expiresAt,
      });
    }

    return {
      reservations,
      reservationIds: reservations.map((reservation) => reservation.reservationId),
      expiresAt,
    };
  });
}

async function persistCheckoutSession({
  buyerUid,
  checkoutId,
  paymentIntentId,
  reservations,
  expiresAt,
  totalAmountMinor,
  stripeAmountMinor,
  walletContributionMinor,
  walletDeductionMinor,
  currency,
  checkoutFlow,
}: {
  buyerUid: string;
  checkoutId: string;
  paymentIntentId: string | null;
  reservations: ReservationDraft[];
  expiresAt: Timestamp;
  totalAmountMinor: number;
  stripeAmountMinor: number;
  walletContributionMinor: number;
  walletDeductionMinor: number;
  currency: string;
  checkoutFlow: typeof CHECKOUT_FLOW_PAYMENT_INTENT | typeof CHECKOUT_FLOW_WALLET_ONLY;
}): Promise<void> {
  const checkoutSessionRef = db.collection("checkoutSessions").doc(checkoutId);
  const batch = db.batch();

  batch.set(checkoutSessionRef, {
    sessionId: checkoutId,
    buyerUid,
    reservationIds: reservations.map((reservation) => reservation.reservationId),
    status: CHECKOUT_STATUS_PENDING,
    checkoutFlow,
    createdAt: FieldValue.serverTimestamp(),
    expiresAt,
    paymentIntentId,
    amountMinor: totalAmountMinor,
    stripeAmountMinor,
    walletContributionMinor,
    walletDeductionMinor,
    currency,
  });

  for (const reservation of reservations) {
    batch.update(reservation.reservationRef, {
      checkoutSessionId: checkoutId,
    });
  }

  await batch.commit();
}

async function rollbackReservationState(
  reservations: ReservationDraft[],
): Promise<void> {
  if (reservations.length === 0) {
    return;
  }

  const batch = db.batch();
  for (const reservation of reservations) {
    batch.delete(reservation.reservationRef);
    batch.update(reservation.bookRef, {
      status: "AVAILABLE",
      reservedByUid: FieldValue.delete(),
      reservationId: FieldValue.delete(),
      reservationExpiresAt: FieldValue.delete(),
    });
  }
  await batch.commit();
}

interface WalletContributionResult {
  contributionMinor: number;
  walletDeductionMinor: number;
}

async function reserveWalletContributionForCheckout({
  userId,
  currency,
  requestedAmountMinor,
}: {
  userId: string;
  currency: string;
  requestedAmountMinor: number;
}): Promise<WalletContributionResult> {
  const zero: WalletContributionResult = { contributionMinor: 0, walletDeductionMinor: 0 };

  if (requestedAmountMinor <= 0) {
    return zero;
  }

  return db.runTransaction(async (transaction) => {
    const walletRef = db.collection("wallets").doc(userId);
    const walletSnapshot = await transaction.get(walletRef);
    if (!walletSnapshot.exists) {
      return zero;
    }

    const walletData = toRecord(walletSnapshot.data());
    const walletCurrency = asString(walletData.currency)?.toLowerCase() ?? currency;
    const currentBalanceMinor = asInteger(walletData.balanceMinor) ?? 0;
    if (currentBalanceMinor <= 0) {
      return zero;
    }

    // Convert wallet balance to checkout currency for comparison.
    // If currencies differ, use EXCHANGE_RATE_TABLE; returns 0 for unsupported pairs.
    const walletBalanceInCheckoutCurrency = convertMinorBetweenCurrencies(
      currentBalanceMinor,
      walletCurrency,
      currency,
    );
    if (walletBalanceInCheckoutCurrency <= 0) {
      return zero;
    }

    // How much of the checkout total can the wallet cover (in checkout currency)?
    const contributionMinor = Math.min(walletBalanceInCheckoutCurrency, requestedAmountMinor);

    // How much to deduct from the wallet (in wallet currency)?
    // Round up so we never under-deduct relative to the discount given.
    const walletDeductionMinor =
      walletCurrency === currency
        ? contributionMinor
        : Math.min(
            Math.ceil(contributionMinor / (EXCHANGE_RATE_TABLE[walletCurrency]?.[currency] ?? 1)),
            currentBalanceMinor,
          );

    transaction.set(
      walletRef,
      {
        currency: walletCurrency,
        balanceMinor: currentBalanceMinor - walletDeductionMinor,
        updatedAt: FieldValue.serverTimestamp(),
      },
      { merge: true },
    );

    return { contributionMinor, walletDeductionMinor };
  });
}

async function refundWalletContributionForCheckout({
  userId,
  amountMinor,
}: {
  userId: string;
  amountMinor: number;
}): Promise<void> {
  if (amountMinor <= 0) {
    return;
  }

  await db.runTransaction(async (transaction) => {
    await applyWalletCheckoutRefundInTransaction({
      transaction,
      userId,
      amountMinor,
    });
  });
}

async function applyWalletCheckoutRefundInTransaction({
  transaction,
  userId,
  amountMinor,
}: {
  transaction: Transaction;
  userId: string;
  amountMinor: number;
}): Promise<void> {
  const walletRef = db.collection("wallets").doc(userId);
  const walletSnapshot = await transaction.get(walletRef);
  const walletData = toRecord(walletSnapshot.data());
  const currentBalanceMinor = asInteger(walletData.balanceMinor) ?? 0;
  const currentCurrency = asString(walletData.currency);

  // amountMinor is already in wallet currency - add it back directly.
  transaction.set(
    walletRef,
    {
      currency: currentCurrency ?? WALLET_DEFAULT_CURRENCY,
      balanceMinor: currentBalanceMinor + amountMinor,
      updatedAt: FieldValue.serverTimestamp(),
    },
    { merge: true },
  );
}

async function loadWalletTopupIntentResponse({
  stripe,
  userId,
  topupId,
}: {
  stripe: Stripe;
  userId: string;
  topupId: string;
}): Promise<{
  topupId: string;
  paymentIntentId: string;
  clientSecret: string;
  amountMinor: number;
  currency: string;
}> {
  const topupSnapshot = await db.collection("walletTopups").doc(topupId).get();
  if (!topupSnapshot.exists) {
    throw new HttpsError("internal", "Wallet topup record not found.");
  }

  const topupData = toRecord(topupSnapshot.data());
  const topupUserId = asString(topupData.userId);
  if (topupUserId !== userId) {
    throw new HttpsError("permission-denied", "Wallet topup does not belong to user.");
  }

  const paymentIntentId = asString(topupData.paymentIntentId);
  const amountMinor = asInteger(topupData.amountMinor);
  const currency = asString(topupData.currency) ?? WALLET_DEFAULT_CURRENCY;

  if (!paymentIntentId || amountMinor === null) {
    throw new HttpsError("internal", "Wallet topup record is invalid.");
  }

  const paymentIntent = await stripe.paymentIntents.retrieve(paymentIntentId);
  if (!paymentIntent.client_secret) {
    throw new HttpsError("internal", "Stripe PaymentIntent missing client_secret.");
  }

  return {
    topupId,
    paymentIntentId,
    clientSecret: paymentIntent.client_secret,
    amountMinor,
    currency,
  };
}

async function processWalletWebhookEvent(event: Stripe.Event): Promise<void> {
  switch (event.type) {
    case "payment_intent.succeeded":
      await handleWalletPaymentIntentEvent({
        event,
        targetStatus: WALLET_TOPUP_STATUS_SUCCEEDED,
      });
      return;
    case "payment_intent.payment_failed":
      await handleWalletPaymentIntentEvent({
        event,
        targetStatus: WALLET_TOPUP_STATUS_FAILED,
      });
      return;
    case "payment_intent.canceled":
      await handleWalletPaymentIntentEvent({
        event,
        targetStatus: WALLET_TOPUP_STATUS_CANCELED,
      });
      return;
    case "charge.refunded":
      await handleWalletChargeRefundedEvent(event);
      return;
    default:
      logger.info("Stripe wallet event ignored.", { eventType: event.type });
  }
}

async function handleWalletPaymentIntentEvent({
  event,
  targetStatus,
}: {
  event: Stripe.Event;
  targetStatus:
    | typeof WALLET_TOPUP_STATUS_SUCCEEDED
    | typeof WALLET_TOPUP_STATUS_FAILED
    | typeof WALLET_TOPUP_STATUS_CANCELED;
}): Promise<void> {
  const paymentIntent = event.data.object as Stripe.PaymentIntent;
  const paymentIntentId = paymentIntent.id;
  const topupIdFromMetadata = asString(paymentIntent.metadata?.topupId);
  const topupRef = await resolveWalletTopupRef({
    topupId: topupIdFromMetadata,
    paymentIntentId,
  });
  const eventRef = db.collection("stripeWalletWebhookEvents").doc(event.id);

  await db.runTransaction(async (transaction) => {
    const existingEventSnapshot = await transaction.get(eventRef);
    if (existingEventSnapshot.exists) {
      return;
    }

    if (!topupRef) {
      transaction.set(eventRef, {
        eventId: event.id,
        eventType: event.type,
        paymentIntentId,
        result: "IGNORED",
        reason: "TOPUP_NOT_FOUND",
        processedAt: FieldValue.serverTimestamp(),
      });
      return;
    }

    const topupSnapshot = await transaction.get(topupRef);
    if (!topupSnapshot.exists) {
      transaction.set(eventRef, {
        eventId: event.id,
        eventType: event.type,
        paymentIntentId,
        topupId: topupRef.id,
        result: "IGNORED",
        reason: "TOPUP_NOT_FOUND",
        processedAt: FieldValue.serverTimestamp(),
      });
      return;
    }

    const topupData = toRecord(topupSnapshot.data());
    const topupStatus =
      asString(topupData.status) ?? WALLET_TOPUP_STATUS_PENDING;
    const userId = asString(topupData.userId);
    const amountMinor = asInteger(topupData.amountMinor);
    const currency = asString(topupData.currency) ?? WALLET_DEFAULT_CURRENCY;

    if (!userId || amountMinor === null) {
      transaction.set(eventRef, {
        eventId: event.id,
        eventType: event.type,
        paymentIntentId,
        topupId: topupRef.id,
        result: "IGNORED",
        reason: "INVALID_TOPUP_RECORD",
        processedAt: FieldValue.serverTimestamp(),
      });
      return;
    }

    let result = "IGNORED";
    let reason: string | null = "NO_STATE_CHANGE";

    if (
      targetStatus === WALLET_TOPUP_STATUS_SUCCEEDED &&
      topupStatus === WALLET_TOPUP_STATUS_PENDING
    ) {
      await applyWalletTopupBalanceChangeInTransaction({
        transaction,
        userId,
        currency,
        amountDeltaMinor: amountMinor,
      });

      const walletLedgerRef = db.collection("wallets").doc(userId).collection("ledger").doc();
      transaction.set(walletLedgerRef, {
        type: "TOPUP_CREDIT",
        amountMinor,
        currency,
        referenceType: "WALLET_TOPUP",
        referenceId: topupRef.id,
        createdAt: FieldValue.serverTimestamp(),
      });

      transaction.update(topupRef, {
        status: WALLET_TOPUP_STATUS_SUCCEEDED,
        settledAt: FieldValue.serverTimestamp(),
        failureCode: null,
        failureMessage: null,
        updatedAt: FieldValue.serverTimestamp(),
      });

      result = "APPLIED";
      reason = null;
    } else if (
      (targetStatus === WALLET_TOPUP_STATUS_FAILED ||
        targetStatus === WALLET_TOPUP_STATUS_CANCELED) &&
      topupStatus === WALLET_TOPUP_STATUS_PENDING
    ) {
      const lastPaymentError = paymentIntent.last_payment_error;
      transaction.update(topupRef, {
        status: targetStatus,
        failureCode: asString(lastPaymentError?.code) ?? null,
        failureMessage:
          asString(lastPaymentError?.message) ??
          asString(paymentIntent.cancellation_reason) ??
          null,
        updatedAt: FieldValue.serverTimestamp(),
      });
      result = "APPLIED";
      reason = null;
    }

    transaction.set(eventRef, {
      eventId: event.id,
      eventType: event.type,
      paymentIntentId,
      topupId: topupRef.id,
      result,
      reason,
      processedAt: FieldValue.serverTimestamp(),
    });
  });
}

async function handleWalletChargeRefundedEvent(event: Stripe.Event): Promise<void> {
  const charge = event.data.object as Stripe.Charge;
  const paymentIntentId =
    typeof charge.payment_intent === "string" ? charge.payment_intent : null;
  const topupIdFromMetadata = asString(charge.metadata?.topupId);
  const topupRef = await resolveWalletTopupRef({
    topupId: topupIdFromMetadata,
    paymentIntentId,
  });
  const eventRef = db.collection("stripeWalletWebhookEvents").doc(event.id);

  await db.runTransaction(async (transaction) => {
    const existingEventSnapshot = await transaction.get(eventRef);
    if (existingEventSnapshot.exists) {
      return;
    }

    if (!topupRef) {
      transaction.set(eventRef, {
        eventId: event.id,
        eventType: event.type,
        paymentIntentId,
        result: "IGNORED",
        reason: "TOPUP_NOT_FOUND",
        processedAt: FieldValue.serverTimestamp(),
      });
      return;
    }

    const topupSnapshot = await transaction.get(topupRef);
    if (!topupSnapshot.exists) {
      transaction.set(eventRef, {
        eventId: event.id,
        eventType: event.type,
        paymentIntentId,
        topupId: topupRef.id,
        result: "IGNORED",
        reason: "TOPUP_NOT_FOUND",
        processedAt: FieldValue.serverTimestamp(),
      });
      return;
    }

    const topupData = toRecord(topupSnapshot.data());
    const topupStatus =
      asString(topupData.status) ?? WALLET_TOPUP_STATUS_PENDING;
    const userId = asString(topupData.userId);
    const amountMinor = asInteger(topupData.amountMinor);
    const currency = asString(topupData.currency) ?? WALLET_DEFAULT_CURRENCY;

    if (!userId || amountMinor === null) {
      transaction.set(eventRef, {
        eventId: event.id,
        eventType: event.type,
        paymentIntentId,
        topupId: topupRef.id,
        result: "IGNORED",
        reason: "INVALID_TOPUP_RECORD",
        processedAt: FieldValue.serverTimestamp(),
      });
      return;
    }

    let result = "IGNORED";
    let reason: string | null = "NO_STATE_CHANGE";

    if (topupStatus === WALLET_TOPUP_STATUS_SUCCEEDED) {
      await applyWalletTopupBalanceChangeInTransaction({
        transaction,
        userId,
        currency,
        amountDeltaMinor: -amountMinor,
      });

      const walletLedgerRef = db.collection("wallets").doc(userId).collection("ledger").doc();
      transaction.set(walletLedgerRef, {
        type: "TOPUP_REFUND_DEBIT",
        amountMinor: -amountMinor,
        currency,
        referenceType: "WALLET_TOPUP",
        referenceId: topupRef.id,
        createdAt: FieldValue.serverTimestamp(),
      });

      transaction.update(topupRef, {
        status: WALLET_TOPUP_STATUS_REFUNDED,
        refundedAt: FieldValue.serverTimestamp(),
        updatedAt: FieldValue.serverTimestamp(),
      });
      result = "APPLIED";
      reason = null;
    }

    transaction.set(eventRef, {
      eventId: event.id,
      eventType: event.type,
      paymentIntentId,
      topupId: topupRef.id,
      result,
      reason,
      processedAt: FieldValue.serverTimestamp(),
    });
  });
}

async function applyWalletTopupBalanceChangeInTransaction({
  transaction,
  userId,
  currency,
  amountDeltaMinor,
}: {
  transaction: Transaction;
  userId: string;
  currency: string;
  amountDeltaMinor: number;
}): Promise<void> {
  const walletRef = db.collection("wallets").doc(userId);
  const walletSnapshot = await transaction.get(walletRef);
  const walletData = toRecord(walletSnapshot.data());
  const currentBalanceMinor = asInteger(walletData.balanceMinor) ?? 0;
  const currentCurrency = asString(walletData.currency);

  if (currentCurrency && currentCurrency !== currency) {
    throw new Error("Wallet currency mismatch.");
  }

  const newBalanceMinor = currentBalanceMinor + amountDeltaMinor;
  transaction.set(
    walletRef,
    {
      currency,
      balanceMinor: newBalanceMinor,
      updatedAt: FieldValue.serverTimestamp(),
    },
    { merge: true },
  );
}

async function resolveWalletTopupRef({
  topupId,
  paymentIntentId,
}: {
  topupId: string | null;
  paymentIntentId: string | null;
}): Promise<DocumentReference | null> {
  if (topupId) {
    return db.collection("walletTopups").doc(topupId);
  }

  if (!paymentIntentId) {
    return null;
  }

  const topupSnapshot = await db
    .collection("walletTopups")
    .where("paymentIntentId", "==", paymentIntentId)
    .limit(1)
    .get();

  return topupSnapshot.empty ? null : topupSnapshot.docs[0].ref;
}

function readWalletTopupAmountMinor(payload: Record<string, unknown>): number {
  const value = payload.amountMinor;
  if (typeof value !== "number" || !Number.isInteger(value)) {
    throw new HttpsError("invalid-argument", "amountMinor must be an integer.");
  }

  if (value < MIN_WALLET_TOPUP_MINOR || value > MAX_WALLET_TOPUP_MINOR) {
    throw new HttpsError(
      "invalid-argument",
      `amountMinor must be between ${MIN_WALLET_TOPUP_MINOR} and ${MAX_WALLET_TOPUP_MINOR}.`,
    );
  }

  return value;
}

function readWalletTopupCurrency(payload: Record<string, unknown>): string {
  const currency = asString(payload.currency)?.toLowerCase();
  if (!currency) {
    throw new HttpsError("invalid-argument", "currency is required.");
  }

  if (currency !== WALLET_DEFAULT_CURRENCY) {
    throw new HttpsError(
      "invalid-argument",
      `Only ${WALLET_DEFAULT_CURRENCY} currency is currently supported.`,
    );
  }

  return currency;
}

function readWalletTopupIdempotencyKey(payload: Record<string, unknown>): string {
  const idempotencyKey = asString(payload.idempotencyKey);
  if (!idempotencyKey) {
    throw new HttpsError("invalid-argument", "idempotencyKey is required.");
  }
  return idempotencyKey;
}

function buildWalletTopupIdempotencyDocId(
  userId: string,
  idempotencyKey: string,
): string {
  return createHash("sha256")
    .update(`${userId}:${idempotencyKey}`)
    .digest("hex");
}

function buildStripeWalletIdempotencyKey(
  userId: string,
  idempotencyKey: string,
): string {
  const candidate = `wallet_topup_${userId}_${idempotencyKey}`;
  if (candidate.length <= MAX_STRIPE_IDEMPOTENCY_KEY_LENGTH) {
    return candidate;
  }

  return `wallet_topup_${createHash("sha256").update(candidate).digest("hex")}`;
}

async function finalizeCheckoutSession(
  session: Stripe.Checkout.Session,
): Promise<void> {
  const sessionId = session.id;
  const metadataFallback = extractSessionMetadataFallback(session);

  await db.runTransaction(async (transaction) => {
    const checkoutSessionRef = db.collection("checkoutSessions").doc(sessionId);
    const checkoutSessionSnapshot = await transaction.get(checkoutSessionRef);

    if (!checkoutSessionSnapshot.exists) {
      logger.warn("Checkout session document not found while finalizing.", {
        sessionId,
      });
      return;
    }

    const checkoutData = toRecord(checkoutSessionSnapshot.data());
    const currentStatus =
      asString(checkoutData.status) ?? CHECKOUT_STATUS_PENDING;

    if (currentStatus === CHECKOUT_STATUS_COMPLETED) {
      return;
    }

    if (currentStatus !== CHECKOUT_STATUS_PENDING) {
      logger.warn("Skipping checkout finalization due to non-pending status.", {
        sessionId,
        currentStatus,
      });
      return;
    }

    const buyerUid = asString(checkoutData.buyerUid) ?? metadataFallback.buyerUid;
    if (!buyerUid) {
      throw new Error(
        `Cannot finalize checkout session ${sessionId}: missing buyerUid.`,
      );
    }

    const reservationIds = uniqueStrings([
      ...parseReservationIds(checkoutData.reservationIds),
      ...metadataFallback.reservationIds,
    ]);
    const purchasedBookIds = new Set<string>();
    const reservationsToConfirm: DocumentReference[] = [];
    const booksToMarkSold: DocumentReference[] = [];
    type SellerCreditDraft = {
      sellerUid: string;
      amountMinor: number;
      currency: string;
    };
    const sellerCreditDrafts: SellerCreditDraft[] = [];
    type ReservationReadResult = {
      reservationId: string;
      reservationRef: DocumentReference;
      reservationStatus: string;
      bookId: string | null;
      reservationBuyerUid: string | null;
      sellerUid: string | null;
      reservationUnitAmount: number | null;
      reservationCurrency: string | null;
    };
    const reservationReadResults: ReservationReadResult[] = [];

    for (const reservationId of reservationIds) {
      const reservationRef = db.collection("reservations").doc(reservationId);
      const reservationSnapshot = await transaction.get(reservationRef);

      if (!reservationSnapshot.exists) {
        logger.warn("Reservation missing while finalizing checkout.", {
          sessionId,
          reservationId,
        });
        continue;
      }

      const reservationData = toRecord(reservationSnapshot.data());
      const reservationStatus =
        asString(reservationData.status) ?? RESERVATION_STATUS_PENDING;
      if (
        reservationStatus !== RESERVATION_STATUS_PENDING &&
        reservationStatus !== RESERVATION_STATUS_CONFIRMED
      ) {
        logger.warn("Skipping reservation finalization due to status.", {
          sessionId,
          reservationId,
          reservationStatus,
        });
        continue;
      }

      const bookId = asString(reservationData.bookId);
      const reservationBuyerUid =
        asString(reservationData.buyerUid) ?? buyerUid;
      const sellerUid = asString(reservationData.sellerUid);
      const reservationUnitAmount = asInteger(reservationData.unitAmount);
      const reservationCurrency = asString(reservationData.currency)?.toLowerCase() ?? null;
      reservationReadResults.push({
        reservationId,
        reservationRef,
        reservationStatus,
        bookId,
        reservationBuyerUid,
        sellerUid,
        reservationUnitAmount,
        reservationCurrency,
      });

      if (bookId) {
        purchasedBookIds.add(bookId);
      }
    }

    for (const reservationResult of reservationReadResults) {
      if (reservationResult.reservationStatus === RESERVATION_STATUS_PENDING) {
        reservationsToConfirm.push(reservationResult.reservationRef);
      }

      if (!reservationResult.bookId) {
        continue;
      }

      const bookRef = db.collection("books").doc(reservationResult.bookId);
      const bookSnapshot = await transaction.get(bookRef);

      if (!bookSnapshot.exists) {
        logger.warn("Book missing while finalizing checkout.", {
          sessionId,
          reservationId: reservationResult.reservationId,
          bookId: reservationResult.bookId,
        });
        continue;
      }

      const bookData = toRecord(bookSnapshot.data());
      const bookStatus = asString(bookData.status);
      if (bookStatus === "SOLD") {
        continue;
      }

      const activeReservationId = asString(bookData.reservationId);
      if (
        activeReservationId &&
        activeReservationId !== reservationResult.reservationId
      ) {
        logger.warn("Book reservation mismatch while finalizing checkout.", {
          sessionId,
          reservationId: reservationResult.reservationId,
          bookId: reservationResult.bookId,
          activeReservationId,
        });
        continue;
      }

      const reservedByUid = asString(bookData.reservedByUid);
      if (
        reservedByUid &&
        reservationResult.reservationBuyerUid &&
        reservedByUid !== reservationResult.reservationBuyerUid
      ) {
        logger.warn("Book reservedByUid mismatch while finalizing checkout.", {
          sessionId,
          reservationId: reservationResult.reservationId,
          bookId: reservationResult.bookId,
          reservedByUid,
          reservationBuyerUid: reservationResult.reservationBuyerUid,
        });
        continue;
      }

      booksToMarkSold.push(bookRef);

      const sellerUid = reservationResult.sellerUid ?? asString(bookData.userID);
      if (!sellerUid) {
        throw new Error(
          `Cannot finalize checkout ${sessionId}: missing sellerUid for reservation ${reservationResult.reservationId}.`,
        );
      }

      let amountMinor = reservationResult.reservationUnitAmount;
      if (amountMinor === null || amountMinor <= 0) {
        const price = asNumber(bookData.price);
        if (price !== null && price > 0) {
          amountMinor = Math.round(price * CENTS_MULTIPLIER);
        }
      }

      let creditCurrency = reservationResult.reservationCurrency;
      if (!creditCurrency) {
        creditCurrency = (asString(bookData.priceCurrency) ?? "EUR").toLowerCase();
      }

      if (
        amountMinor === null ||
        amountMinor <= 0 ||
        !creditCurrency ||
        !/^[a-z]{3}$/.test(creditCurrency)
      ) {
        throw new Error(
          `Cannot finalize checkout ${sessionId}: invalid seller credit data for reservation ${reservationResult.reservationId}.`,
        );
      }

      sellerCreditDrafts.push({
        sellerUid,
        amountMinor,
        currency: creditCurrency,
      });
    }

    type SellerWalletCreditWrite = {
      sellerUid: string;
      walletRef: DocumentReference;
      walletCurrency: string;
      currentBalanceMinor: number;
      creditMinor: number;
    };
    const sellerWalletCredits: SellerWalletCreditWrite[] = [];

    if (sellerCreditDrafts.length > 0) {
      const creditsBySeller = new Map<string, SellerCreditDraft[]>();
      for (const draft of sellerCreditDrafts) {
        const existing = creditsBySeller.get(draft.sellerUid) ?? [];
        existing.push(draft);
        creditsBySeller.set(draft.sellerUid, existing);
      }

      for (const [sellerUid, sellerCredits] of creditsBySeller.entries()) {
        const walletRef = db.collection("wallets").doc(sellerUid);
        const walletSnapshot = await transaction.get(walletRef);
        const walletData = toRecord(walletSnapshot.data());
        const walletCurrency =
          asString(walletData.currency)?.toLowerCase() ?? WALLET_DEFAULT_CURRENCY;
        const currentBalanceMinor = asInteger(walletData.balanceMinor) ?? 0;

        let creditMinor = 0;
        for (const sellerCredit of sellerCredits) {
          const convertedAmountMinor = convertMinorBetweenCurrencies(
            sellerCredit.amountMinor,
            sellerCredit.currency,
            walletCurrency,
          );
          if (convertedAmountMinor <= 0) {
            throw new Error(
              `Cannot finalize checkout ${sessionId}: unsupported currency conversion for seller ${sellerUid} (${sellerCredit.currency} -> ${walletCurrency}).`,
            );
          }
          creditMinor += convertedAmountMinor;
        }

        if (creditMinor <= 0) {
          continue;
        }

        sellerWalletCredits.push({
          sellerUid,
          walletRef,
          walletCurrency,
          currentBalanceMinor,
          creditMinor,
        });
      }
    }

    const now = Timestamp.now();
    transaction.update(checkoutSessionRef, {
      status: CHECKOUT_STATUS_COMPLETED,
      completedAt: now,
      updatedAt: now,
      paymentStatus: session.payment_status ?? null,
      paymentIntentId: extractPaymentIntentId(session),
    });

    for (const reservationRef of reservationsToConfirm) {
      transaction.update(reservationRef, {
        status: RESERVATION_STATUS_CONFIRMED,
        confirmedAt: now,
        updatedAt: now,
      });
    }

    for (const bookRef of booksToMarkSold) {
      transaction.update(bookRef, {
        status: "SOLD",
        soldAt: now,
        soldToUid: buyerUid,
        reservedByUid: FieldValue.delete(),
        reservationId: FieldValue.delete(),
        reservationExpiresAt: FieldValue.delete(),
      });
    }

    for (const bookId of purchasedBookIds) {
      const cartItemRef = db
        .collection("users")
        .doc(buyerUid)
        .collection("cart")
        .doc(bookId);
      transaction.delete(cartItemRef);
    }

    for (const sellerWalletCredit of sellerWalletCredits) {
      transaction.set(
        sellerWalletCredit.walletRef,
        {
          currency: sellerWalletCredit.walletCurrency,
          balanceMinor:
            sellerWalletCredit.currentBalanceMinor +
            sellerWalletCredit.creditMinor,
          updatedAt: FieldValue.serverTimestamp(),
        },
        { merge: true },
      );

      const walletLedgerRef = sellerWalletCredit.walletRef.collection("ledger").doc();
      transaction.set(walletLedgerRef, {
        type: "SALE_CREDIT",
        amountMinor: sellerWalletCredit.creditMinor,
        currency: sellerWalletCredit.walletCurrency,
        referenceType: "CHECKOUT_SESSION",
        referenceId: sessionId,
        counterpartyUid: buyerUid,
        createdAt: FieldValue.serverTimestamp(),
      });
    }
  });

  logger.info("Checkout session finalized.", { sessionId });
}

async function finalizeCheckoutPaymentIntent(
  paymentIntent: Stripe.PaymentIntent,
): Promise<void> {
  try {
    await finalizeCheckoutSession(
      {
        id: paymentIntent.id,
        metadata: paymentIntent.metadata,
        payment_status: paymentIntent.status,
        payment_intent: paymentIntent.id,
      } as unknown as Stripe.Checkout.Session,
    );
  } catch (error) {
    try {
      await releaseCheckoutSession({
        sessionId: paymentIntent.id,
        releaseStatus: CHECKOUT_STATUS_CANCELED,
        fallbackBuyerUid: asString(paymentIntent.metadata?.buyerUid),
        fallbackReservationIds: parseReservationIds(
          paymentIntent.metadata?.reservationIds,
        ),
      });
    } catch (releaseError) {
      logger.error(
        "Failed to release checkout session after finalizeCheckoutPaymentIntent error.",
        {
          paymentIntentId: paymentIntent.id,
          releaseError,
        },
      );
    }

    try {
      const stripe = new Stripe(stripeSecretKey.value());
      await refundStripePaymentIntentSafely(stripe, paymentIntent.id);
    } catch (refundError) {
      logger.error(
        "Failed to refund payment intent after finalizeCheckoutPaymentIntent error.",
        {
          paymentIntentId: paymentIntent.id,
          refundError,
        },
      );
    }
    throw error;
  }
}

async function releaseCheckoutSession({
  session,
  sessionId: explicitSessionId,
  releaseStatus,
  fallbackBuyerUid,
  fallbackReservationIds,
}: {
  session?: Stripe.Checkout.Session;
  sessionId?: string;
  releaseStatus: CheckoutReleaseStatus;
  fallbackBuyerUid?: string | null;
  fallbackReservationIds?: string[];
}): Promise<void> {
  const sessionId = session?.id ?? explicitSessionId;
  if (!sessionId) {
    return;
  }

  const metadataFallback = session
    ? extractSessionMetadataFallback(session)
    : { buyerUid: null, reservationIds: [] };
  const mergedFallbackReservationIds = uniqueStrings([
    ...(fallbackReservationIds ?? []),
    ...metadataFallback.reservationIds,
  ]);
  const mergedFallbackBuyerUid =
    fallbackBuyerUid ?? metadataFallback.buyerUid ?? null;

  const releaseApplied = await db.runTransaction(async (transaction) => {
    const checkoutSessionRef = db.collection("checkoutSessions").doc(sessionId);
    const checkoutSessionSnapshot = await transaction.get(checkoutSessionRef);

    if (!checkoutSessionSnapshot.exists) {
      logger.warn("Checkout session document not found while releasing.", {
        sessionId,
      });
      return false;
    }

    const checkoutData = toRecord(checkoutSessionSnapshot.data());
    const currentStatus =
      asString(checkoutData.status) ?? CHECKOUT_STATUS_PENDING;

    if (currentStatus === CHECKOUT_STATUS_COMPLETED) {
      return false;
    }

    if (currentStatus !== CHECKOUT_STATUS_PENDING) {
      return false;
    }

    const buyerUid = asString(checkoutData.buyerUid) ?? mergedFallbackBuyerUid;
    const walletContributionMinor = asInteger(checkoutData.walletContributionMinor) ?? 0;
    // walletDeductionMinor is the actual wallet-currency amount deducted. Falls back to
    // walletContributionMinor for older sessions where currencies always matched.
    const walletDeductionMinor = asInteger(checkoutData.walletDeductionMinor) ?? walletContributionMinor;
    const reservationIds = uniqueStrings([
      ...parseReservationIds(checkoutData.reservationIds),
      ...mergedFallbackReservationIds,
    ]);
    const now = Timestamp.now();
    const reservationsToRelease: DocumentReference[] = [];

    for (const reservationId of reservationIds) {
      const reservationRef = db.collection("reservations").doc(reservationId);
      const reservationSnapshot = await transaction.get(reservationRef);

      if (!reservationSnapshot.exists) {
        logger.warn("Reservation missing while releasing checkout.", {
          sessionId,
          reservationId,
        });
        continue;
      }

      const reservationData = toRecord(reservationSnapshot.data());
      const reservationStatus =
        asString(reservationData.status) ?? RESERVATION_STATUS_PENDING;
      if (reservationStatus !== RESERVATION_STATUS_PENDING) {
        continue;
      }

      reservationsToRelease.push(reservationRef);
    }

    if (walletContributionMinor > 0) {
      if (!buyerUid) {
        throw new Error(
          `Cannot release checkout session ${sessionId}: missing buyerUid for wallet refund.`,
        );
      }
      await applyWalletCheckoutRefundInTransaction({
        transaction,
        userId: buyerUid,
        amountMinor: walletDeductionMinor,
      });
    }

    transaction.update(checkoutSessionRef, {
      status: releaseStatus,
      releasedAt: now,
      updatedAt: now,
    });

    // Books stay RESERVED in cart — only clear checkout association on reservations.
    for (const reservationRef of reservationsToRelease) {
      transaction.update(reservationRef, {
        checkoutSessionId: null,
        updatedAt: now,
      });
    }

    return true;
  });

  if (releaseApplied) {
    logger.info("Checkout session released.", {
      sessionId,
      releaseStatus,
    });
  }
}

function extractSessionMetadataFallback(
  session: Stripe.Checkout.Session,
): {
  buyerUid: string | null;
  reservationIds: string[];
} {
  const metadata = session.metadata ?? {};
  return {
    buyerUid: asString(metadata.buyerUid),
    reservationIds: parseReservationIds(metadata.reservationIds),
  };
}

function isCheckoutPaymentIntent(paymentIntent: Stripe.PaymentIntent): boolean {
  const purpose = asString(paymentIntent.metadata?.purpose);
  if (purpose === CHECKOUT_PAYMENT_PURPOSE) {
    return true;
  }

  return parseReservationIds(paymentIntent.metadata?.reservationIds).length > 0;
}

function parseReservationIds(value: unknown): string[] {
  if (Array.isArray(value)) {
    return uniqueStrings(value.map((entry) => asString(entry)));
  }

  if (typeof value === "string") {
    return uniqueStrings(value.split(","));
  }

  return [];
}

function uniqueStrings(values: Array<string | null | undefined>): string[] {
  return Array.from(
    new Set(
      values
        .map((value) => (typeof value === "string" ? value.trim() : ""))
        .filter((value) => value.length > 0),
    ),
  );
}

function extractPaymentIntentId(session: Stripe.Checkout.Session): string | null {
  const paymentIntent = session.payment_intent;
  if (typeof paymentIntent === "string") {
    return paymentIntent;
  }

  if (
    paymentIntent &&
    typeof paymentIntent === "object" &&
    typeof paymentIntent.id === "string"
  ) {
    return paymentIntent.id;
  }

  return null;
}

async function cancelStripePaymentIntentSafely(
  stripe: Stripe,
  paymentIntentId: string,
): Promise<void> {
  try {
    await stripe.paymentIntents.cancel(paymentIntentId);
  } catch (error) {
    if (error instanceof Stripe.errors.StripeInvalidRequestError) {
      logger.info("Stripe PaymentIntent cannot be canceled or is already terminal.", {
        paymentIntentId,
        code: error.code ?? null,
      });
      return;
    }
    logger.warn("Failed to cancel Stripe PaymentIntent.", {
      paymentIntentId,
      error,
    });
  }
}

async function refundStripePaymentIntentSafely(
  stripe: Stripe,
  paymentIntentId: string,
): Promise<void> {
  try {
    await stripe.refunds.create(
      {
        payment_intent: paymentIntentId,
      },
      {
        idempotencyKey: `checkout_finalize_fail_${paymentIntentId}`,
      },
    );
  } catch (error) {
    if (error instanceof Stripe.errors.StripeInvalidRequestError) {
      logger.info("Stripe PaymentIntent refund skipped or already handled.", {
        paymentIntentId,
        code: error.code ?? null,
      });
      return;
    }
    logger.warn("Failed to refund Stripe PaymentIntent.", {
      paymentIntentId,
      error,
    });
  }
}

function readRawRequestBody(request: {
  rawBody?: unknown;
  body?: unknown;
}): Buffer {
  if (Buffer.isBuffer(request.rawBody)) {
    return request.rawBody;
  }

  if (typeof request.rawBody === "string") {
    return Buffer.from(request.rawBody, "utf8");
  }

  if (Buffer.isBuffer(request.body)) {
    return request.body;
  }

  if (typeof request.body === "string") {
    return Buffer.from(request.body, "utf8");
  }

  return Buffer.from("");
}

function readStripeSignatureHeader(
  value: string | string[] | undefined,
): string | null {
  if (typeof value === "string" && value.length > 0) {
    return value;
  }
  if (Array.isArray(value) && value.length > 0 && value[0]) {
    return value[0];
  }
  return null;
}

function readReservationTtlMinutes(payload: Record<string, unknown>): number {
  const rawValue = payload.reservationTtlMinutes;
  if (rawValue === undefined || rawValue === null) {
    return DEFAULT_RESERVATION_TTL_MINUTES;
  }

  if (typeof rawValue !== "number" || Number.isNaN(rawValue)) {
    throw new HttpsError(
      "invalid-argument",
      "reservationTtlMinutes must be a number.",
    );
  }

  const normalized = Math.floor(rawValue);
  if (normalized < 1 || normalized > MAX_RESERVATION_TTL_MINUTES) {
    throw new HttpsError(
      "invalid-argument",
      `reservationTtlMinutes must be between 1 and ${MAX_RESERVATION_TTL_MINUTES}.`,
    );
  }

  return normalized;
}

function toRecord(value: unknown): Record<string, unknown> {
  if (typeof value !== "object" || value === null || Array.isArray(value)) {
    return {};
  }
  return value as Record<string, unknown>;
}

function asString(value: unknown): string | null {
  if (typeof value !== "string") {
    return null;
  }
  const normalized = value.trim();
  return normalized.length > 0 ? normalized : null;
}

function asNumber(value: unknown): number | null {
  if (typeof value !== "number" || !Number.isFinite(value)) {
    return null;
  }
  return value;
}

function asInteger(value: unknown): number | null {
  if (typeof value !== "number" || !Number.isInteger(value)) {
    return null;
  }
  return value;
}

function toHttpsError(error: unknown): HttpsError {
  if (error instanceof HttpsError) {
    return error;
  }

  if (error instanceof Stripe.errors.StripeError) {
    return new HttpsError(
      "internal",
      `Stripe error: ${error.message ?? "Unknown Stripe error."}`,
    );
  }

  if (error instanceof Error) {
    return new HttpsError("internal", error.message);
  }

  return new HttpsError("internal", "Unknown checkout error.");
}

export const onCheckoutCompleted = onDocumentUpdated(
  { document: "checkoutSessions/{sessionId}", secrets: [gmailPass] },
  async (event) => {
    const before = event.data?.before.data();
    const after = event.data?.after.data();

    if (!before || !after) return null;
    if (before.status === "COMPLETED" || after.status !== "COMPLETED") return null;

    const buyerId: string = after.buyerUid;
    const reservationIds: string[] = after.reservationIds ?? [];

    if (reservationIds.length === 0) return null;

    const buyerDoc = await db.collection("users").doc(buyerId).get();
    const buyerEmail: string | null = buyerDoc.exists ? (buyerDoc.data()?.email ?? null) : null;

    const bookDetails: BookData[] = [];
    const sellerEmails = new Map<string, { email: string; books: BookData[] }>();

    for (const resId of reservationIds) {
      const resDoc = await db.collection("reservations").doc(resId).get();
      if (!resDoc.exists) continue;

      const resData = resDoc.data()!;
      const bookId: string = resData.bookId;
      const sellerId: string = resData.sellerUid;

      const bookDoc = await db.collection("books").doc(bookId).get();
      if (!bookDoc.exists) continue;

      const bookData = bookDoc.data() as BookData;
      bookDetails.push(bookData);

      if (!sellerEmails.has(sellerId)) {
        const sellerDoc = await db.collection("users").doc(sellerId).get();
        if (sellerDoc.exists) {
          sellerEmails.set(sellerId, { email: sellerDoc.data()?.email, books: [] });
        }
      }
      sellerEmails.get(sellerId)?.books.push(bookData);
    }

    if (buyerEmail) {
      await sendBuyerOrderEmail(buyerEmail, bookDetails);
    }

    for (const [, data] of sellerEmails) {
      await sendSellerOrderEmail(data.email, data.books);
    }

    return null;
  },
);
