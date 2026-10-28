// Standardized API response helpers.
//
// Every endpoint returns a consistent envelope: { status, success, message, data }
// on success, { status, success, data, meta } on paginated listings, and
// { success, message, errors } on failure.

/**
 * Send a success response.
 * @param {import("express").Response} res
 * @param {object} [options]
 * @param {number} [options.statusCode=200] - HTTP status code.
 * @param {string} [options.message="Success"] - Human-readable message.
 * @param {*} [options.data=null] - Payload.
 * @param {object} [options.meta] - Optional pagination metadata.
 */
function sendSuccess(res, { statusCode = 200, message = "Success", data = null, meta = undefined } = {}) {
  const payload = {
    status: "success",
    success: true,
    message,
    data,
  };
  if (meta !== undefined) {
    payload.meta = meta;
  }
  return res.status(statusCode).json(payload);
}

/**
 * Send a standardized paginated response.
 *
 * Conforms to the required format:
 * {
 *   "status": "success",
 *   "data": [...],
 *   "meta": {
 *     "page": 1,
 *     "limit": 10,
 *     "total": 42,
 *     "totalPages": 5
 *   }
 * }
 * Retains `success` and `message` for consistency across all endpoints.
 *
 * @param {import("express").Response} res
 * @param {object} options
 * @param {number} [options.statusCode=200] - HTTP status code.
 * @param {string} [options.message="Success"] - Human-readable message.
 * @param {Array} [options.data=[]] - Paginated items list.
 * @param {object} options.meta - Pagination metadata object.
 */
function sendPaginated(res, { statusCode = 200, message = "Success", data = [], meta } = {}) {
  return res.status(statusCode).json({
    status: "success",
    success: true,
    message,
    data,
    meta,
  });
}

/**
 * Send an error response.
 * @param {import("express").Response} res
 * @param {object} [options]
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
  sendPaginated,
  sendError,
};
