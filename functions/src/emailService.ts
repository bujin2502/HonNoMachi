import * as nodemailer from "nodemailer";

export interface BookData {
  title?: string;
  price?: number;
  priceCurrency?: string;
}

function createTransporter() {
  return nodemailer.createTransport({
    service: "gmail",
    auth: {
      user: "honnomachi.app@gmail.com",
      pass: process.env.GMAIL_PASS,
    },
  });
}

export async function sendBuyerOrderEmail(to: string, books: BookData[]): Promise<void> {
  const booksListHtml = books
    .map((book) => `<li><strong>${book.title}</strong> - ${book.price} ${book.priceCurrency}</li>`)
    .join("");
  const total = books.reduce((sum, b) => sum + (b.price ?? 0), 0);
  const currency = books[0]?.priceCurrency;

  const mailOptions = {
    from: "HonNoMachi <honnomachi.app@gmail.com>",
    to,
    subject: "Uspješna kupnja! / Successful purchase!",
    html: `
      <div style="font-family: sans-serif; line-height: 1.6;">
        <h2>Hvala na kupnji!</h2>
        <p>Uspješno ste kupili sljedeće knjige:</p>
        <ul>${booksListHtml}</ul>
        <p><strong>Ukupno plaćeno: ${total.toFixed(2)} ${currency}</strong></p>
        <p>Vaše knjige će vam uskoro biti dostupne.</p>

        <p>------------------------------------------------------------</p>

        <h2>Thank you for your purchase!</h2>
        <p>You have successfully purchased the following books:</p>
        <ul>${booksListHtml}</ul>
        <p><strong>Total paid: ${total.toFixed(2)} ${currency}</strong></p>
        <p>Your books will be available to you soon.</p>

        <br>
        <p>Srdačan pozdrav / Best regards,<br><strong>HonNoMachi Team!</strong></p>
      </div>
    `,
  };

  try {
    await createTransporter().sendMail(mailOptions);
    console.log("Buyer email sent successfully");
  } catch (error) {
    console.error("Error sending buyer email:", error);
  }
}

export async function sendSellerOrderEmail(to: string, books: BookData[]): Promise<void> {
  const booksListHtml = books.map((book) => `<li>${book.title}</li>`).join("");

  const mailOptions = {
    from: "HonNoMachi <honnomachi.app@gmail.com>",
    to,
    subject: "Vaša knjiga je prodana! / Your book has been sold!",
    html: `
      <div style="font-family: sans-serif; line-height: 1.6;">
        <h2>Čestitamo!</h2>
        <p>Sljedeći naslovi koje ste oglasili su upravo prodani:</p>
        <ul>${booksListHtml}</ul>
        <p>Sredstva će biti dodana na vaš račun nakon obrade. Provjerite aplikaciju za detalje.</p>

        <p>------------------------------------------------------------</p>

        <h2>Congratulations!</h2>
        <p>The following titles you listed have just been sold:</p>
        <ul>${booksListHtml}</ul>
        <p>The funds will be added to your account after processing. Check the app for details.</p>

        <br>
        <p>Hvala što prodajete na HonNoMachi!<br>Thank you for selling on HonNoMachi!</p>
      </div>
    `,
  };

  try {
    await createTransporter().sendMail(mailOptions);
    console.log("Seller email sent successfully");
  } catch (error) {
    console.error("Error sending seller email:", error);
  }
}
