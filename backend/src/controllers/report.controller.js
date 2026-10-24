// Incident report controller.
//
// Thin HTTP layer: validates input, delegates to the service, shapes the
// response. No business logic here.

const { sendSuccess, sendError } = require("../utils/response");
const reportService = require("../services/report.service");
const {
  validateCreateReport,
  validateUpdateReport,
} = require("../validations/report.validation");

function handleError(res, error, context) {
  if (error instanceof reportService.ServiceError) {
    return sendError(res, {
      statusCode: error.statusCode,
      message: error.message,
      errors: error.errors,
    });
  }
  console.error(`${context} error:`, error);
  return sendError(res, {
    statusCode: 500,
    message: "Something went wrong.",
  });
}

/**
 * POST /api/reports
 */
async function create(req, res) {
  try {
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
  } catch (error) {
    return handleError(res, error, "Create report");
  }
}

/**
 * GET /api/reports
 */
async function list(req, res) {
  try {
    const reports = await reportService.getReports(req.user.id);

    return sendSuccess(res, {
      statusCode: 200,
      message: "Incident reports retrieved successfully.",
      data: reports,
    });
  } catch (error) {
    return handleError(res, error, "List reports");
  }
}

/**
 * GET /api/reports/:id
 */
async function detail(req, res) {
  try {
    const report = await reportService.getReportById(req.user.id, req.params.id);

    return sendSuccess(res, {
      statusCode: 200,
      message: "Incident report retrieved successfully.",
      data: report,
    });
  } catch (error) {
    return handleError(res, error, "Get report");
  }
}

/**
 * PUT /api/reports/:id
 */
async function update(req, res) {
  try {
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
  } catch (error) {
    return handleError(res, error, "Update report");
  }
}

/**
 * DELETE /api/reports/:id
 */
async function remove(req, res) {
  try {
    await reportService.deleteReport(req.user.id, req.params.id);

    return sendSuccess(res, {
      statusCode: 200,
      message: "Incident report deleted successfully.",
    });
  } catch (error) {
    return handleError(res, error, "Delete report");
  }
}

module.exports = {
  create,
  list,
  detail,
  update,
  remove,
};
