const functions = require("firebase-functions/v1");
const admin = require("firebase-admin");
const { sendSellerOrderEmail, sendBuyerOrderEmail } = require("./emailService");

admin.initializeApp();

exports.onCheckoutCompleted = functions
  .runWith({ secrets: ["GMAIL_PASS"] })
  .firestore.document("checkoutSessions/{sessionId}")
  .onUpdate(async (change, context) => {
    const before = change.before.data();
    const after = change.after.data();

    if (before.status !== "COMPLETED" && after.status === "COMPLETED") {
      const buyerId = after.buyerUid;
      const reservationIds = after.reservationIds;

      if (!reservationIds || reservationIds.length === 0) return null;

      const buyerDoc = await admin.firestore().collection("users").doc(buyerId).get();
      const buyerData = buyerDoc.exists ? buyerDoc.data() : null;
      const buyerEmail = buyerData ? buyerData.email : null;
      const buyerNotificationsEnabled = buyerData ? buyerData.notificationsEnabled !== false : true;

      const bookDetails = [];
      const sellerEmails = new Map();

      for (const resId of reservationIds) {
        const resDoc = await admin.firestore().collection("reservations").doc(resId).get();
        if (resDoc.exists) {
          const resData = resDoc.data();
          const bookId = resData.bookId;
          const sellerId = resData.sellerUid;

          const bookDoc = await admin.firestore().collection("books").doc(bookId).get();
          if (bookDoc.exists) {
            const bookData = bookDoc.data();
            bookDetails.push(bookData);

            if (!sellerEmails.has(sellerId)) {
              const sellerDoc = await admin.firestore().collection("users").doc(sellerId).get();
              if (sellerDoc.exists) {
                const sData = sellerDoc.data();
                sellerEmails.set(sellerId, {
                  email: sData.email,
                  notificationsEnabled: sData.notificationsEnabled !== false,
                  books: []
                });
              }
            }
            sellerEmails.get(sellerId)?.books.push(bookData);
          }
        }
      }

      if (buyerEmail && buyerNotificationsEnabled) {
        await sendBuyerOrderEmail(buyerEmail, bookDetails);
      }

      for (const [sellerId, data] of sellerEmails) {
        if (data.notificationsEnabled) {
          await sendSellerOrderEmail(data.email, data.books);
        }
      }
    }
    return null;
  });
