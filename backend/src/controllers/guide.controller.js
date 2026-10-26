// Guide controller.
//
// Thin HTTP layer: validates input, delegates to the service, and shapes the
// response. No business logic here. list/detail are public; create/update/
// remove are reached only through admin-gated routes. Handlers are wrapped
// with asyncHandler, so thrown ServiceErrors (and any unexpected error)
// propagate to the global error handler.

const { sendSuccess, sendPaginated, sendError } = require("../utils/response");
const { asyncHandler } = require("../utils/async-handler");
const { parsePagination, buildPaginationMeta } = require("../utils/pagination");
const guideService = require("../services/guide.service");
const {
  validateCreateGuide,
  validateUpdateGuide,
} = require("../validations/guide.validation");

/**
 * GET /api/guides (public)
 */
const list = asyncHandler(async (req, res) => {
  const pagination = parsePagination(req.query);
  const { guides, total } = await guideService.getGuides(pagination);
  const meta = buildPaginationMeta(pagination.page, pagination.limit, total);

  return sendPaginated(res, {
    statusCode: 200,
    message: "Guides retrieved successfully.",
    data: guides,
    meta,
  });
});

/**
 * GET /api/guides/:id (public)
 */
const detail = asyncHandler(async (req, res) => {
  const guide = await guideService.getGuideById(req.params.id);

  return sendSuccess(res, {
    statusCode: 200,
    message: "Guide retrieved successfully.",
    data: guide,
  });
});

/**
 * POST /api/guides (admin only)
 */
const create = asyncHandler(async (req, res) => {
  const { valid, errors, data } = validateCreateGuide(req.body);
  if (!valid) {
    return sendError(res, {
      statusCode: 422,
      message: "Validation failed.",
      errors,
    });
  }

  const guide = await guideService.createGuide(data);

  return sendSuccess(res, {
    statusCode: 201,
    message: "Guide created successfully.",
    data: guide,
  });
});

/**
 * PUT /api/guides/:id (admin only)
 */
const update = asyncHandler(async (req, res) => {
  const { valid, errors, data } = validateUpdateGuide(req.body);
  if (!valid) {
    return sendError(res, {
      statusCode: 422,
      message: "Validation failed.",
      errors,
    });
  }

  const guide = await guideService.updateGuide(req.params.id, data);

  return sendSuccess(res, {
    statusCode: 200,
    message: "Guide updated successfully.",
    data: guide,
  });
});

/**
 * DELETE /api/guides/:id (admin only)
 */
const remove = asyncHandler(async (req, res) => {
  await guideService.deleteGuide(req.params.id);

  return sendSuccess(res, {
    statusCode: 200,
    message: "Guide deleted successfully.",
  });
});

module.exports = {
  list,
  detail,
  create,
  update,
  remove,
};
