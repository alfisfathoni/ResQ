// Nodemailer email utility.
//
// Builds a single reusable SMTP transport from the existing environment
// variables (SMTP_HOST, SMTP_PORT, SMTP_USER, SMTP_PASS) and exposes a
// thin sendMail() wrapper. The transport is created lazily so the app can
// boot even when SMTP is not yet configured.

const nodemailer = require("nodemailer");

let transporter = null;

/**
 * Lazily create (and cache) the SMTP transport from environment variables.
 * @returns {import("nodemailer").Transporter}
 */
function getTransporter() {
  if (transporter) return transporter;

  const port = parseInt(process.env.SMTP_PORT || "587", 10);

  transporter = nodemailer.createTransport({
    host: process.env.SMTP_HOST,
    port,
    // Port 465 uses implicit TLS; other ports (e.g. 587) use STARTTLS.
    secure: port === 465,
    auth: {
      user: process.env.SMTP_USER,
      pass: process.env.SMTP_PASS,
    },
  });

  return transporter;
}

/**
 * Send an email.
 * @param {object} options
 * @param {string} options.to - Recipient address.
 * @param {string} options.subject - Email subject.
 * @param {string} [options.text] - Plaintext body.
 * @param {string} [options.html] - HTML body.
 * @returns {Promise<import("nodemailer").SentMessageInfo>}
 */
async function sendMail({ to, subject, text, html }) {
  const from = process.env.SMTP_FROM || process.env.SMTP_USER;
  return getTransporter().sendMail({ from, to, subject, text, html });
}

/**
 * Send the account email-verification message.
 * @param {object} options
 * @param {string} options.to - Recipient address.
 * @param {string} options.fullName - Recipient's display name.
 * @param {string} options.token - The verification token to embed in the link.
 * @returns {Promise<import("nodemailer").SentMessageInfo>}
 */
async function sendVerificationEmail({ to, fullName, token }) {
  const baseUrl = process.env.APP_URL || "http://localhost:3000";
  const verifyUrl = `${baseUrl}/api/auth/verify-email?token=${encodeURIComponent(token)}`;

  const subject = "Verify your ResQ account";
  const text =
    `Hi ${fullName},\n\n` +
    `Welcome to ResQ. Please verify your email address by opening the link below:\n\n` +
    `${verifyUrl}\n\n` +
    `If you did not create this account, you can safely ignore this email.`;
  const html =
    `<p>Hi ${fullName},</p>` +
    `<p>Welcome to ResQ. Please verify your email address by clicking the link below:</p>` +
    `<p><a href="${verifyUrl}">Verify my email</a></p>` +
    `<p>If you did not create this account, you can safely ignore this email.</p>`;

  return sendMail({ to, subject, text, html });
}

/**
 * Send the password-reset email.
 * @param {object} options
 * @param {string} options.to - Recipient address.
 * @param {string} options.fullName - Recipient's display name.
 * @param {string} options.token - The password-reset token to embed in the link.
 * @returns {Promise<import("nodemailer").SentMessageInfo>}
 */
async function sendPasswordResetEmail({ to, fullName, token }) {
  const baseUrl = process.env.APP_URL || "http://localhost:3000";
  // Points at the browser form served by GET /reset-password (page.routes.js).
  const resetUrl = `${baseUrl}/reset-password?token=${encodeURIComponent(token)}`;

  const subject = "Reset your ResQ password";
  const text =
    `Hi ${fullName},\n\n` +
    `We received a request to reset your ResQ password. Open the link below to choose a new one:\n\n` +
    `${resetUrl}\n\n` +
    `This link will expire shortly. If you did not request a password reset, you can safely ignore this email.`;
  const html =
    `<p>Hi ${fullName},</p>` +
    `<p>We received a request to reset your ResQ password. Click the link below to choose a new one:</p>` +
    `<p><a href="${resetUrl}">Reset my password</a></p>` +
    `<p>This link will expire shortly. If you did not request a password reset, you can safely ignore this email.</p>`;

  return sendMail({ to, subject, text, html });
}

module.exports = {
  getTransporter,
  sendMail,
  sendVerificationEmail,
  sendPasswordResetEmail,
};
