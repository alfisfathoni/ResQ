// Role-based authorization middleware.
//
// Restricts a route to users whose role is in the allowed list. Must run
// AFTER the authenticate middleware so req.user (populated with { id, role }
// from the access token) is available. Authorization is strictly role-based:
// it inspects req.user.role only and performs no other checks.

const { sendError } = require("../utils/response");

/**
 * Restrict access to the given roles.
 *
 * Returns an Express middleware that allows the request through only when the
 * authenticated user's role is one of `allowedRoles`. It relies on a prior
 * `authenticate` call to have set req.user; if req.user is missing the request
 * is treated as unauthenticated (401). A present user whose role is not
 * permitted is rejected with 403. Single responsibility: role check only — no
 * verification/ownership/other concerns.
 *
 * @param {...string} allowedRoles - e.g. authorize("ADMIN").
 * @returns {import("express").RequestHandler}
 */
function authorize(...allowedRoles) {
  return function roleGuard(req, res, next) {
    // authenticate must run first; without req.user there is no identity to
    // authorize. Mirror the auth middleware's wording for a consistent 401.
    if (!req.user || !req.user.role) {
      return sendError(res, {
        statusCode: 401,
        message: "Authentication required.",
      });
    }

    // Strictly role-based: allow only when the user's role is permitted.
    if (!allowedRoles.includes(req.user.role)) {
      return sendError(res, {
        statusCode: 403,
        message: "You do not have permission to perform this action.",
      });
    }

    return next();
  };
}

/**
 * Convenience guard for admin-only routes.
 *
 * Equivalent to authorize("ADMIN"); provided so admin routes read clearly
 * (e.g. router.post("/", authenticate, isAdmin, handler)). authorize(...)
 * remains available for any other role combination.
 *
 * @type {import("express").RequestHandler}
 */
const isAdmin = authorize("ADMIN");

module.exports = {
  authorize,
  isAdmin,
};
