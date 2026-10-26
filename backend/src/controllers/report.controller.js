// Incident report controller.
//
// Thin HTTP layer: validates input, delegates to the service, shapes the
// response. No business logic here. Handlers are wrapped with asyncHandler, so
// thrown ServiceErrors (and any unexpected error) propagate to the global
// error handler.

const { sendSuccess, sendPaginated, sendError } = require("../utils/response");
const { asyncHandler } = require("../utils/async-handler");
const { parsePagination, buildPaginationMeta } = require("../utils/pagination");
const reportService = require("../services/report.service");
const {
  validateCreateReport,
  validateUpdateReport,
} = require("../validations/report.validation");

/**
 * POST /api/reports
 */
const create = asyncHandler(async (req, res) => {
  const { valid, errors, data } = validateCreateReport(req.body);
  if (!valid) {
    return sendError(res, {
      statusCode: 422,
      message: "Validation failed.",
      errors,
    });
  }

  const report = await reportService.createReport(req.user.id, data);

  return sendSuccess(res, {
    statusCode: 201,
    message: "Incident report created successfully.",
    data: report,
  });
});

/**
 * GET /api/reports
 */
const list = asyncHandler(async (req, res) => {
  const pagination = parsePagination(req.query);
  const { reports, total } = await reportService.getReports(req.user.id, pagination);
  const meta = buildPaginationMeta(pagination.page, pagination.limit, total);

  return sendPaginated(res, {
    statusCode: 200,
    message: "Incident reports retrieved successfully.",
    data: reports,
    meta,
  });
});

/**
 * GET /api/reports/:id
 */
const detail = asyncHandler(async (req, res) => {
  const report = await reportService.getReportById(req.user.id, req.params.id);

  return sendSuccess(res, {
    statusCode: 200,
    message: "Incident report retrieved successfully.",
    data: report,
  });
});

/**
 * PUT /api/reports/:id
 */
const update = asyncHandler(async (req, res) => {
  const { valid, errors, data } = validateUpdateReport(req.body);
  if (!valid) {
    return sendError(res, {
      statusCode: 422,
      message: "Validation failed.",
      errors,
    });
  }

  const report = await reportService.updateReport(
    req.user.id,
    req.params.id,
    data
  );

  return sendSuccess(res, {
    statusCode: 200,
    message: "Incident report updated successfully.",
    data: report,
  });
});

/**
 * DELETE /api/reports/:id
 */
const remove = asyncHandler(async (req, res) => {
  await reportService.deleteReport(req.user.id, req.params.id);

  return sendSuccess(res, {
    statusCode: 200,
    message: "Incident report deleted successfully.",
  });
});

module.exports = {
  create,
  list,
  detail,
  update,
  remove,
};
