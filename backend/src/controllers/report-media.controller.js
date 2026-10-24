// Report media controller.
//
// Thin HTTP layer: validates the uploaded file, delegates to the service, and
// shapes the response. No business logic here.

const { sendSuccess, sendError } = require("../utils/response");
const reportMediaService = require("../services/report-media.service");
const { validateUploadMedia } = require("../validations/report-media.validation");

function handleError(res, error, context) {
  if (error instanceof reportMediaService.ServiceError) {
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
 * POST /api/reports/:reportId/media
 */
async function create(req, res) {
  try {
    const { valid, errors, data } = validateUploadMedia(req.file);
    if (!valid) {
      return sendError(res, {
        statusCode: 422,
        message: "Validation failed.",
        errors,
      });
    }

    const media = await reportMediaService.createMedia({
      userId: req.user.id,
      reportId: req.params.reportId,
      buffer: req.file.buffer,
      mediaType: data.mediaType,
      resourceType: data.resourceType,
    });

    return sendSuccess(res, {
      statusCode: 201,
      message: "Media uploaded successfully.",
      data: media,
    });
  } catch (error) {
    return handleError(res, error, "Create media");
  }
}

/**
 * GET /api/reports/:reportId/media
 */
async function list(req, res) {
  try {
    const media = await reportMediaService.getMedia(
      req.user.id,
      req.params.reportId
    );

    return sendSuccess(res, {
      statusCode: 200,
      message: "Media retrieved successfully.",
      data: media,
    });
  } catch (error) {
    return handleError(res, error, "List media");
  }
}

/**
 * DELETE /api/reports/:reportId/media/:mediaId
 */
async function remove(req, res) {
  try {
    await reportMediaService.deleteMedia(
      req.user.id,
      req.params.reportId,
      req.params.mediaId
    );

    return sendSuccess(res, {
      statusCode: 200,
      message: "Media deleted successfully.",
    });
  } catch (error) {
    return handleError(res, error, "Delete media");
  }
}

module.exports = {
  create,
  list,
  remove,
};
