// HTTP request logging (morgan) with secret redaction.
//
// Some URLs carry one-time tokens in the query string, for example
// GET /api/auth/verify-email?token=... and GET /reset-password?token=...
// The stock morgan tokens (:url, :referrer) would write those tokens to the
// server log, so this module defines redacting replacements and builds the
// log formats on top of them.

const morgan = require("morgan");

// Matches the value of a `token` query parameter (`?token=...` or `&token=...`).
const TOKEN_PARAM_PATTERN = /([?&]token=)[^&#\s]*/gi;

/**
 * Replace the value of every `token` query parameter with [REDACTED].
 * @param {string|undefined} url - A request URL or referrer.
 * @returns {string|undefined} The URL with token values masked.
 */
function redactToken(url) {
  if (typeof url !== "string") return url;
  return url.replace(TOKEN_PARAM_PATTERN, "$1[REDACTED]");
}

// :safe-url - request URL with token values masked (use instead of :url).
morgan.token("safe-url", (req) => redactToken(req.originalUrl || req.url));

// :safe-referrer - Referer header with token values masked (use instead of :referrer).
morgan.token("safe-referrer", (req) => redactToken(req.headers.referer || req.headers.referrer));

// Same layout as morgan's "combined" format, but with the safe tokens.
const COMBINED_SAFE_FORMAT =
  ':remote-addr - :remote-user [:date[clf]] ":method :safe-url HTTP/:http-version" ' +
  ':status :res[content-length] ":safe-referrer" ":user-agent"';

/**
 * Same layout and colors as morgan's "dev" format, but with :safe-url.
 * @param {object} tokens - morgan tokens.
 * @param {import("express").Request} req
 * @param {import("express").Response} res
 * @returns {string}
 */
function devSafeFormat(tokens, req, res) {
  const status = res.headersSent ? res.statusCode : undefined;
  let color = 0; // no color
  if (status >= 500) color = 31; // red
  else if (status >= 400) color = 33; // yellow
  else if (status >= 300) color = 36; // cyan
  else if (status >= 200) color = 32; // green

  return (
    `\x1b[0m${tokens.method(req, res)} ${tokens["safe-url"](req, res)} ` +
    `\x1b[${color}m${tokens.status(req, res)}\x1b[0m ` +
    `${tokens["response-time"](req, res)} ms - ${tokens.res(req, res, "content-length")}\x1b[0m`
  );
}

/**
 * Build the request-logging middleware.
 * Production uses the combined layout; every other environment uses dev.
 * @returns {import("express").RequestHandler}
 */
function createRequestLogger() {
  return morgan(process.env.NODE_ENV === "production" ? COMBINED_SAFE_FORMAT : devSafeFormat);
}

module.exports = { redactToken, createRequestLogger };
