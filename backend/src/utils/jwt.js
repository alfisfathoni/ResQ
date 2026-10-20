// JSON Web Token utility.
//
// Centralizes signing and verification of both access and refresh tokens.
// Secrets and lifetimes are read from environment variables, with sensible
// defaults so the module never crashes if an optional var is missing.
//
// Environment variables used:
//   JWT_SECRET              - secret for signing access tokens (required in prod)
//   JWT_EXPIRES_IN          - access token lifetime (default "15m")
//   JWT_REFRESH_SECRET      - secret for signing refresh tokens (falls back to JWT_SECRET)
//   JWT_REFRESH_EXPIRES_IN  - refresh token lifetime (default "7d")

const jwt = require("jsonwebtoken");

const ACCESS_SECRET = process.env.JWT_SECRET || "change_me_access_secret";
const ACCESS_EXPIRES_IN = process.env.JWT_EXPIRES_IN || "15m";
const REFRESH_SECRET = process.env.JWT_REFRESH_SECRET || ACCESS_SECRET;
const REFRESH_EXPIRES_IN = process.env.JWT_REFRESH_EXPIRES_IN || "7d";
const EMAIL_VERIFICATION_SECRET =
  process.env.JWT_EMAIL_VERIFICATION_SECRET || ACCESS_SECRET;
const EMAIL_VERIFICATION_EXPIRES_IN =
  process.env.JWT_EMAIL_VERIFICATION_EXPIRES_IN || "1d";
const PASSWORD_RESET_SECRET =
  process.env.JWT_PASSWORD_RESET_SECRET || ACCESS_SECRET;
const PASSWORD_RESET_EXPIRES_IN =
  process.env.JWT_PASSWORD_RESET_EXPIRES_IN || "1h";

/**
 * Sign a short-lived access token.
 * @param {object} payload - Claims to embed (e.g. { sub, role }).
 * @returns {string} Signed JWT.
 */
function signAccessToken(payload) {
  return jwt.sign(payload, ACCESS_SECRET, { expiresIn: ACCESS_EXPIRES_IN });
}

/**
 * Sign a long-lived refresh token.
 * @param {object} payload - Claims to embed (e.g. { sub }).
 * @returns {string} Signed JWT.
 */
function signRefreshToken(payload) {
  return jwt.sign(payload, REFRESH_SECRET, { expiresIn: REFRESH_EXPIRES_IN });
}

/**
 * Verify and decode an access token.
 * @param {string} token
 * @returns {object} The decoded payload.
 * @throws {jwt.JsonWebTokenError} When the token is invalid or expired.
 */
function verifyAccessToken(token) {
  return jwt.verify(token, ACCESS_SECRET);
}

/**
 * Verify and decode a refresh token.
 * @param {string} token
 * @returns {object} The decoded payload.
 * @throws {jwt.JsonWebTokenError} When the token is invalid or expired.
 */
function verifyRefreshToken(token) {
  return jwt.verify(token, REFRESH_SECRET);
}

/**
 * Sign a stateless email-verification token.
 *
 * The User model has no column to persist a verification token, so the token
 * is self-contained: the user id plus a `type` claim, validated later by
 * signature and expiry alone (no database lookup of the token needed).
 * @param {object} payload - Claims to embed (e.g. { sub: userId }).
 * @returns {string} Signed JWT.
 */
function signEmailVerificationToken(payload) {
  return jwt.sign({ ...payload, type: "email_verification" }, EMAIL_VERIFICATION_SECRET, {
    expiresIn: EMAIL_VERIFICATION_EXPIRES_IN,
  });
}

/**
 * Verify and decode an email-verification token.
 * @param {string} token
 * @returns {object} The decoded payload.
 * @throws {jwt.JsonWebTokenError} When the token is invalid or expired.
 */
function verifyEmailVerificationToken(token) {
  return jwt.verify(token, EMAIL_VERIFICATION_SECRET);
}

/**
 * Sign a stateless password-reset token.
 *
 * Like the email-verification token, this is self-contained (user id plus a
 * `type` claim) and short-lived, validated later by signature and expiry
 * alone — the User model has no column to persist a reset token.
 * @param {object} payload - Claims to embed (e.g. { sub: userId }).
 * @returns {string} Signed JWT.
 */
function signPasswordResetToken(payload) {
  return jwt.sign({ ...payload, type: "password_reset" }, PASSWORD_RESET_SECRET, {
    expiresIn: PASSWORD_RESET_EXPIRES_IN,
  });
}

/**
 * Verify and decode a password-reset token.
 * @param {string} token
 * @returns {object} The decoded payload.
 * @throws {jwt.JsonWebTokenError} When the token is invalid or expired.
 */
function verifyPasswordResetToken(token) {
  return jwt.verify(token, PASSWORD_RESET_SECRET);
}

module.exports = {
  signAccessToken,
  signRefreshToken,
  verifyAccessToken,
  verifyRefreshToken,
  signEmailVerificationToken,
  verifyEmailVerificationToken,
  signPasswordResetToken,
  verifyPasswordResetToken,
};
