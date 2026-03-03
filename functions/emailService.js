const nodemailer = require("nodemailer");

const transporter = nodemailer.createTransport({
  service: "gmail",
  auth: {
    user: "honnomachi.app@gmail.com",
    pass: process.env.GMAIL_PASS,
  },
});

async function sendSellerEmail(to, book) {
  const mailOptions = {
    from: "HonNoMachi <honnomachi.app@gmail.com>",
    to: to,
    subject: "Vaša knjiga je prodana! / Your book has been sold!",
    html: `
        <h2>Čestitamo!</h2>
        <p>Vaša knjiga <strong>${book.title}</strong> je prodana.</p>
        <p>Cijena: ${book.price} ${book.priceCurrency}</p>
        <p>Hvala što koristite HonNoMachi!</p>
        
        <p>-----------------------------------</p>
        
        <h2>Congratulations!</h2>
        <p>Your book <strong>${book.title}</strong> has been sold.</p>
        <p>Price: ${book.price} ${book.priceCurrency}</p>
        <p>Thank you for using HonNoMachi!</p>
      `,
  };

  try {
    await transporter.sendMail(mailOptions);
    console.log("Email sent successfully via Gmail");
  } catch (error) {
    console.error("Error sending email via Gmail:", error);
  }
}

module.exports = { sendSellerEmail };
