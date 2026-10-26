// Report media controller.
//
// Thin HTTP layer: validates the uploaded file, delegates to the service, and
// shapes the response. No business logic here. Handlers are wrapped with
// asyncHandler, so thrown ServiceErrors (and any unexpected error) propagate
// to the global error handler.

const { sendSuccess, sendPaginated, sendError } = require("../utils/response");
const { asyncHandler } = require("../utils/async-handler");
const { parsePagination, buildPaginationMeta } = require("../utils/pagination");
const reportMediaService = require("../services/report-media.service");
const { validateUploadMedia } = require("../validations/report-media.validation");

/**
 * POST /api/reports/:reportId/media
 */
const create = asyncHandler(async (req, res) => {
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
});

/**
 * GET /api/reports/:reportId/media
 */
const list = asyncHandler(async (req, res) => {
  const pagination = parsePagination(req.query);
  const { media, total } = await reportMediaService.getMedia(
    req.user.id,
    req.params.reportId,
    pagination
  );
  const meta = buildPaginationMeta(pagination.page, pagination.limit, total);

  return sendPaginated(res, {
    statusCode: 200,
    message: "Media retrieved successfully.",
    data: media,
    meta,
  });
});

/**
 * DELETE /api/reports/:reportId/media/:mediaId
 */
const remove = asyncHandler(async (req, res) => {
  await reportMediaService.deleteMedia(
    req.user.id,
    req.params.reportId,
    req.params.mediaId
  );

  return sendSuccess(res, {
    statusCode: 200,
    message: "Media deleted successfully.",
  });
});

module.exports = {
  create,
  list,
  remove,
};
