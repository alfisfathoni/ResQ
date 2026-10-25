// Global error handler.
//
// The single, last-registered Express error middleware. It maps a thrown
// ServiceError to the same standardized response the controllers used to
// produce locally, and turns any unexpected error into a generic 500 — never
// leaking stack traces. This preserves the existing API contract exactly:
// ServiceError keeps its statusCode/message/errors, and the generic 500 uses
// the same "Something went wrong." message the CRUD controllers already used.

const { sendError } = require("../utils/response");
const { ServiceError } = require("../utils/service-error");

/**
 * Express error-handling middleware. Must be registered last, after all routes,
 * and must keep its four-argument signature so Express treats it as an error
 * handler.
 *
 * @param {Error} err
 * @param {import("express").Request} req
 * @param {import("express").Response} res
 * @param {import("express").NextFunction} next
 */
function errorHandler(err, req, res, next) {
  // If the response has already started, defer to Express's default handler.
  if (res.headersSent) {
    return next(err);
  }

  // Expected, mapped service errors keep their status, message, and details.
  if (err instanceof ServiceError) {
    return sendError(res, {
      statusCode: err.statusCode,
      message: err.message,
      errors: err.errors,
    });
  }

  // Unexpected error: log for diagnostics, return a generic 500. Never expose
  // the stack trace or internal error shape to the client.
  console.error("Unhandled error:", err);
  return sendError(res, {
    statusCode: 500,
    message: "Something went wrong.",
  });
}

module.exports = { errorHandler };
