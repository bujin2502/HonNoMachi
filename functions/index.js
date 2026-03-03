const functions = require("firebase-functions/v1");
const admin = require("firebase-admin");
const { sendSellerEmail } = require("./emailService");

admin.initializeApp();

exports.onBookSold = functions
  .runWith({ secrets: ["GMAIL_PASS"] })
  .firestore.document("books/{bookId}")
  .onUpdate(async (change, context) => {
    const before = change.before.data();
    const after = change.after.data();

    // Promjena statusa knjige u SOLD
    if (before.status !== "SOLD" && after.status === "SOLD") {
      console.log("Book sold detected");

      const sellerId = after.userID;

      if (!sellerId) {
        console.log("No seller ID found");
        return null;
      }

      try {
        const userDoc = await admin
          .firestore()
          .collection("users")
          .doc(sellerId)
          .get();

        if (!userDoc.exists) {
          console.log("Seller not found");
          return null;
        }

        const sellerEmail = userDoc.data().email;

        console.log("Sending email to:", sellerEmail);

        await sendSellerEmail(sellerEmail, after);
      } catch (error) {
        console.error("Error:", error);
      }
    }

    return null;
  });
