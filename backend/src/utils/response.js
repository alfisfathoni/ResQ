// Standardized API response helpers.
//
// Every endpoint should return a consistent envelope so the mobile client
// can rely on a predictable shape: { success, message, data } on success
// and { success, message, errors } on failure.

/**
 * Send a success response.
 * @param {import("express").Response} res
 * @param {object} options
 * @param {number} [options.statusCode=200] - HTTP status code.
 * @param {string} [options.message="Success"] - Human-readable message.
 * @param {*} [options.data=null] - Payload.
 */
function sendSuccess(res, { statusCode = 200, message = "Success", data = null } = {}) {
  return res.status(statusCode).json({
    success: true,
    message,
    data,
  });
}

/**
 * Send an error response.
 * @param {import("express").Response} res
 * @param {object} options
 * @param {number} [options.statusCode=500] - HTTP status code.
 * @param {string} [options.message="Something went wrong"] - Human-readable message.
 * @param {*} [options.errors=null] - Optional error details (e.g. validation issues).
 */
function sendError(res, { statusCode = 500, message = "Something went wrong", errors = null } = {}) {
  return res.status(statusCode).json({
    success: false,
    message,
    errors,
  });
}

module.exports = {
  sendSuccess,
  sendError,
};
