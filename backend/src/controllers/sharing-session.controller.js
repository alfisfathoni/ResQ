// Sharing session controller.
//
// Thin HTTP layer: validates input, delegates to the service, and shapes the
// response. No business logic here. All handlers run behind authenticate, so
// req.user is always populated. Handlers are wrapped with asyncHandler, so
// thrown ServiceErrors (and any unexpected error) propagate to the global
// error handler.

const { sendSuccess, sendPaginated, sendError } = require("../utils/response");
const { asyncHandler } = require("../utils/async-handler");
const { parsePagination, buildPaginationMeta } = require("../utils/pagination");
const sharingSessionService = require("../services/sharing-session.service");
const {
  validateCreateSharingSession,
} = require("../validations/sharing-session.validation");

/**
 * POST /api/sharing-sessions
 */
const create = asyncHandler(async (req, res) => {
  const { valid, errors } = validateCreateSharingSession(req.body);
  if (!valid) {
    return sendError(res, {
      statusCode: 422,
      message: "Validation failed.",
      errors,
    });
  }

  const sessions = await sharingSessionService.startSession(req.user.id);

  return sendSuccess(res, {
    statusCode: 201,
    message: "Sharing session started successfully.",
    data: sessions,
  });
});

/**
 * GET /api/sharing-sessions/active
 */
const listActive = asyncHandler(async (req, res) => {
  const pagination = parsePagination(req.query);
  const { sessions, total } = await sharingSessionService.getActiveSessions(
    req.user.id,
    pagination
  );
  const meta = buildPaginationMeta(pagination.page, pagination.limit, total);

  return sendPaginated(res, {
    statusCode: 200,
    message: "Active sharing sessions retrieved successfully.",
    data: sessions,
    meta,
  });
});

/**
 * GET /api/sharing-sessions/:id
 */
const detail = asyncHandler(async (req, res) => {
  const session = await sharingSessionService.getSessionById(
    req.user.id,
    req.params.id
  );

  return sendSuccess(res, {
    statusCode: 200,
    message: "Sharing session retrieved successfully.",
    data: session,
  });
});

/**
 * PATCH /api/sharing-sessions/:id/stop
 */
const stop = asyncHandler(async (req, res) => {
  const sessions = await sharingSessionService.stopSession(
    req.user.id,
    req.params.id
  );

  return sendSuccess(res, {
    statusCode: 200,
    message: "Sharing session stopped successfully.",
    data: sessions,
  });
});

module.exports = {
  create,
  listActive,
  detail,
  stop,
};
