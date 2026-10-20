// Role-based authorization middleware.
//
// Restricts a route to users whose role is in the allowed list. Must run
// after the authenticate middleware so req.user is populated. Implementation
// is a placeholder for now.

const { sendError } = require("../utils/response");

/**
 * Restrict access to the given roles.
 * @param {...string} allowedRoles - e.g. authorize("ADMIN").
 * @returns {import("express").RequestHandler}
 *
 * TODO: compare req.user.role against allowedRoles and allow/deny accordingly.
 */
function authorize(...allowedRoles) {
  return function roleGuard(req, res, next) {
    // TODO: implement role check using req.user.role and allowedRoles
    return sendError(res, { statusCode: 501, message: "Not implemented" });
  };
}

module.exports = {
  authorize,
};
