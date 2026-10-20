// Authentication middleware.
//
// Verifies the Bearer access token on protected routes and attaches the
// decoded identity to req.user.

const jwt = require("jsonwebtoken");
const { sendError } = require("../utils/response");
const { verifyAccessToken } = require("../utils/jwt");

/**
 * Require a valid access token.
 *
 * Expects an `Authorization: Bearer <token>` header. On success, attaches
 * { id, role } (from the token's sub/role claims) to req.user and calls next.
 * On any failure, responds with 401 and does not call next.
 */
function authenticate(req, res, next) {
  const header = req.headers.authorization || "";
  const [scheme, token] = header.split(" ");

  if (scheme !== "Bearer" || !token) {
    return sendError(res, {
      statusCode: 401,
      message: "Authentication required.",
    });
  }

  try {
    const payload = verifyAccessToken(token);
    req.user = { id: payload.sub, role: payload.role };
    return next();
  } catch (error) {
    if (error instanceof jwt.TokenExpiredError) {
      return sendError(res, {
        statusCode: 401,
        message: "Access token has expired.",
      });
    }
    if (error instanceof jwt.JsonWebTokenError) {
      return sendError(res, {
        statusCode: 401,
        message: "Invalid access token.",
      });
    }

    console.error("Authentication error:", error);
    return sendError(res, {
      statusCode: 500,
      message: "Something went wrong during authentication.",
    });
  }
}

module.exports = {
  authenticate,
};
