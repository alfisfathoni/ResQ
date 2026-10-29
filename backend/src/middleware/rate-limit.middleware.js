// Rate limiting middleware.
//
// Protects sensitive endpoints (such as authentication and password reset)
// against brute-force attacks and abuse. The maximum number of requests per
// window is configurable through environment variables (defaults below are
// the original hardcoded values) so many devices on one shared Wi-Fi/NAT
// (for example during a demo) do not lock each other out.

const { rateLimit } = require("express-rate-limit");
const { sendError } = require("../utils/response");
const { readPositiveInt } = require("../config/env");

const WINDOW_MS = 15 * 60 * 1000; // 15 minutes

// Default maximum requests per IP per window (override via env, see .env.example).
const DEFAULT_AUTH_MAX = 20;
const DEFAULT_EMAIL_MAX = 5;
const DEFAULT_REFRESH_MAX = 100;

/**
 * Build a limiter that answers 429 through the standard sendError envelope.
 * @param {object} options
 * @param {number} options.max - Max requests per key per window.
 * @param {string} options.message - Message returned with the 429.
 * @param {number} [options.windowMs] - Window length (default 15 minutes).
 * @param {(req: import("express").Request) => string} [options.keyGenerator] -
 *   Custom counter key; defaults to the client IP.
 * @returns {import("express").RequestHandler}
 */
function buildLimiter({ max, message, windowMs = WINDOW_MS, keyGenerator }) {
  return rateLimit({
    windowMs,
    limit: max,
    standardHeaders: true, // Draft-6 RateLimit-* headers
    legacyHeaders: false, // Disable deprecated X-RateLimit-* headers
    ...(keyGenerator ? { keyGenerator } : {}),
    message,
    handler: (req, res, _next, options) => {
      return sendError(res, {
        statusCode: options.statusCode,
        message: options.message,
      });
    },
  });
}

/**
 * Rate limiter specifically for authentication endpoints to prevent brute-force attacks.
 * Restricts requests per IP within a 15-minute window (AUTH_RATE_LIMIT_MAX, default 20).
 */
const authLimiter = buildLimiter({
  max: readPositiveInt("AUTH_RATE_LIMIT_MAX", DEFAULT_AUTH_MAX),
  message: "Too many authentication attempts, please try again after 15 minutes.",
});

/**
 * Strict rate limiter for endpoints that send email on demand (e.g. resend
 * verification), so they cannot be used to spam an inbox or the SMTP account.
 * EMAIL_RATE_LIMIT_MAX, default 5 per 15 minutes.
 */
const emailLimiter = buildLimiter({
  max: readPositiveInt("EMAIL_RATE_LIMIT_MAX", DEFAULT_EMAIL_MAX),
  message: "Too many email requests, please try again after 15 minutes.",
});

/**
 * Looser limiter for POST /api/auth/refresh. The app refreshes access tokens
 * automatically, so legitimate clients call it far more often than login, and
 * it has its own counter so it does not eat into the login budget.
 * REFRESH_RATE_LIMIT_MAX, default 100 per 15 minutes.
 */
const refreshLimiter = buildLimiter({
  max: readPositiveInt("REFRESH_RATE_LIMIT_MAX", DEFAULT_REFRESH_MAX),
  message: "Too many token refresh requests, please try again after 15 minutes.",
});

/**
 * Limiter for POST /api/sos: max 5 requests per minute PER USER (not per IP,
 * so people sharing a Wi-Fi network do not block each other's emergencies).
 * Must be mounted AFTER the authenticate middleware, which sets req.user.
 */
const sosLimiter = buildLimiter({
  max: 5,
  windowMs: 60 * 1000, // 1 minute
  keyGenerator: (req) => (req.user && req.user.id ? req.user.id : "anonymous"),
  message: "Too many SOS requests, please try again in a minute.",
});

module.exports = {
  authLimiter,
  emailLimiter,
  refreshLimiter,
  sosLimiter,
};
