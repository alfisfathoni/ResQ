// Rate limiting middleware.
//
// Protects sensitive endpoints (such as authentication and password reset)
// against brute-force attacks and abuse.

const { rateLimit } = require("express-rate-limit");
const { sendError } = require("../utils/response");

/**
 * Rate limiter specifically for authentication endpoints to prevent brute-force attacks.
 * Restricts requests per IP within a 15-minute window.
 */
const authLimiter = rateLimit({
  windowMs: 15 * 60 * 1000, // 15 minutes
  max: 20, // Max 20 requests per IP per window
  standardHeaders: true, // Draft-6 RateLimit-* headers
  legacyHeaders: false, // Disable deprecated X-RateLimit-* headers
  message: "Too many authentication attempts, please try again after 15 minutes.",
  handler: (req, res, _next, options) => {
    return sendError(res, {
      statusCode: options.statusCode,
      message: options.message,
    });
  },
});

module.exports = {
  authLimiter,
};
