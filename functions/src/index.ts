import { initializeApp } from "firebase-admin/app";
import {
  DocumentReference,
  FieldValue,
  Timestamp,
  getFirestore,
} from "firebase-admin/firestore";
import { logger } from "firebase-functions";
import { defineSecret } from "firebase-functions/params";
import { HttpsError, onCall } from "firebase-functions/v2/https";
import Stripe from "stripe";

initializeApp();

const db = getFirestore();
const stripeSecretKey = defineSecret("STRIPE_SECRET_KEY");

const DEFAULT_RESERVATION_TTL_MINUTES = 15;
const MAX_RESERVATION_TTL_MINUTES = 60;
const CENTS_MULTIPLIER = 100;

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

export const createCheckoutSession = onCall(
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
    const successUrl = readRequiredUrl(payload, "successUrl");
    const cancelUrl = readRequiredUrl(payload, "cancelUrl");
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

    const transactionResult = await reserveBooksForCheckout(
      buyerUid,
      cartBookIds,
      reservationTtlMinutes,
    );

    const stripe = new Stripe(stripeSecretKey.value());
    let createdStripeSessionId: string | null = null;

    try {
      const session = await stripe.checkout.sessions.create({
        mode: "payment",
        success_url: successUrl,
        cancel_url: cancelUrl,
        line_items: transactionResult.reservations.map((item) => ({
          quantity: 1,
          price_data: {
            currency: item.currency,
            unit_amount: item.unitAmount,
            product_data: {
              name: item.title,
              metadata: {
                bookId: item.bookId,
                sellerUid: item.sellerUid,
              },
            },
          },
        })),
        metadata: {
          buyerUid,
          reservationIds: transactionResult.reservationIds.join(","),
        },
      });
      createdStripeSessionId = session.id;

      if (!session.url) {
        throw new HttpsError(
          "internal",
          "Stripe session is missing checkout URL.",
        );
      }

      await persistCheckoutSession(
        buyerUid,
        session.id,
        session.url,
        transactionResult.reservations,
        transactionResult.expiresAt,
      );

      return {
        sessionId: session.id,
        checkoutUrl: session.url,
        expiresAt: transactionResult.expiresAt.toDate().toISOString(),
        reservationIds: transactionResult.reservationIds,
      };
    } catch (error) {
      try {
        await rollbackReservationState(transactionResult.reservations);
      } catch (rollbackError) {
        logger.error("Failed to rollback reservation state", rollbackError);
      }

      if (createdStripeSessionId) {
        try {
          await stripe.checkout.sessions.expire(createdStripeSessionId);
        } catch (expireError) {
          logger.warn("Failed to expire Stripe session", expireError);
        }
      }
      throw toHttpsError(error);
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

      transaction.set(reservationRef, {
        bookId,
        buyerUid,
        sellerUid,
        status: "PENDING",
        createdAt: now,
        expiresAt,
        checkoutSessionId: null,
      });

      transaction.update(bookRef, {
        status: "RESERVED",
        reservedByUid: buyerUid,
        reservationId: reservationRef.id,
        reservationExpiresAt: expiresAt,
      });

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

    return {
      reservations,
      reservationIds: reservations.map((reservation) => reservation.reservationId),
      expiresAt,
    };
  });
}

async function persistCheckoutSession(
  buyerUid: string,
  stripeSessionId: string,
  stripeCheckoutUrl: string,
  reservations: ReservationDraft[],
  expiresAt: Timestamp,
): Promise<void> {
  const checkoutSessionRef = db.collection("checkoutSessions").doc(stripeSessionId);
  const batch = db.batch();

  batch.set(checkoutSessionRef, {
    sessionId: stripeSessionId,
    buyerUid,
    reservationIds: reservations.map((reservation) => reservation.reservationId),
    status: "PENDING",
    createdAt: FieldValue.serverTimestamp(),
    expiresAt,
    checkoutUrl: stripeCheckoutUrl,
  });

  for (const reservation of reservations) {
    batch.update(reservation.reservationRef, {
      checkoutSessionId: stripeSessionId,
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

function readRequiredUrl(
  payload: Record<string, unknown>,
  key: string,
): string {
  const value = asString(payload[key]);
  if (!value) {
    throw new HttpsError("invalid-argument", `${key} is required.`);
  }
  if (!isHttpUrl(value)) {
    throw new HttpsError(
      "invalid-argument",
      `${key} must be a valid http/https URL.`,
    );
  }
  return value;
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

function isHttpUrl(value: string): boolean {
  try {
    const parsed = new URL(value);
    return parsed.protocol === "https:" || parsed.protocol === "http:";
  } catch {
    return false;
  }
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
