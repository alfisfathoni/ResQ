// SOS controller.
//
// Thin HTTP layer: validates input, delegates to the service, and shapes the
// response. No business logic here. Handlers are wrapped with asyncHandler, so
// thrown ServiceErrors (and any unexpected error) propagate to the global
// error handler, which produces the standardized error response.

const { sendSuccess, sendPaginated, sendError } = require("../utils/response");
const { asyncHandler } = require("../utils/async-handler");
const { parsePagination, buildPaginationMeta } = require("../utils/pagination");
const sosService = require("../services/sos.service");
const { validateTriggerSos, validateResolveSos } = require("../validations/sos.validation");

/**
 * POST /api/sos
 * 201 when a new alert is created, 200 when the user's ACTIVE alert is updated.
 */
const trigger = asyncHandler(async (req, res) => {
  const { valid, errors, data } = validateTriggerSos(req.body);
  if (!valid) {
    return sendError(res, {
      statusCode: 422,
      message: "Validation failed.",
      errors,
    });
  }

  const result = await sosService.triggerSos(req.user.id, data);

  return sendSuccess(res, {
    statusCode: result.created ? 201 : 200,
    message: result.created
      ? "SOS alert triggered successfully."
      : "Active SOS alert updated successfully.",
    data: {
      alert: result.alert,
      emergencyContacts: result.emergencyContacts,
      shareText: result.shareText,
    },
  });
});

/**
 * GET /api/sos/active
 * `data` is the ACTIVE alert, or null when there is none.
 */
const getActive = asyncHandler(async (req, res) => {
  const alert = await sosService.getActiveSos(req.user.id);

  return sendSuccess(res, {
    statusCode: 200,
    message: alert ? "Active SOS alert retrieved successfully." : "No active SOS alert.",
    data: alert,
  });
});

/**
 * GET /api/sos
 */
const list = asyncHandler(async (req, res) => {
  const pagination = parsePagination(req.query);
  const { alerts, total } = await sosService.getSosHistory(req.user.id, pagination);
  const meta = buildPaginationMeta(pagination.page, pagination.limit, total);

  return sendPaginated(res, {
    statusCode: 200,
    message: "SOS history retrieved successfully.",
    data: alerts,
    meta,
  });
});

/**
 * PATCH /api/sos/:id/resolve
 */
const resolve = asyncHandler(async (req, res) => {
  const { valid, errors, data } = validateResolveSos(req.body);
  if (!valid) {
    return sendError(res, {
      statusCode: 422,
      message: "Validation failed.",
      errors,
    });
  }

  const alert = await sosService.resolveSos(req.user.id, req.params.id, data.status);

  return sendSuccess(res, {
    statusCode: 200,
    message: "SOS alert updated successfully.",
    data: alert,
  });
});

module.exports = {
  trigger,
  getActive,
  list,
  resolve,
};
