// Emergency contact controller.
//
// Thin HTTP layer: validates input, delegates to the service, and shapes the
// response. No business logic here. Handlers are wrapped with asyncHandler, so
// thrown ServiceErrors (and any unexpected error) propagate to the global
// error handler, which produces the standardized error response.

const { sendSuccess, sendPaginated, sendError } = require("../utils/response");
const { asyncHandler } = require("../utils/async-handler");
const { parsePagination, buildPaginationMeta } = require("../utils/pagination");
const emergencyContactService = require("../services/emergency-contact.service");
const {
  validateCreateEmergencyContact,
  validateUpdateEmergencyContact,
} = require("../validations/emergency-contact.validation");

/**
 * POST /api/emergency-contacts
 */
const create = asyncHandler(async (req, res) => {
  const { valid, errors, data } = validateCreateEmergencyContact(req.body);
  if (!valid) {
    return sendError(res, {
      statusCode: 422,
      message: "Validation failed.",
      errors,
    });
  }

  const contact = await emergencyContactService.createEmergencyContact(
    req.user.id,
    data
  );

  return sendSuccess(res, {
    statusCode: 201,
    message: "Emergency contact created successfully.",
    data: contact,
  });
});

/**
 * GET /api/emergency-contacts
 */
const list = asyncHandler(async (req, res) => {
  const pagination = parsePagination(req.query);
  const { contacts, total } = await emergencyContactService.getEmergencyContacts(
    req.user.id,
    pagination
  );
  const meta = buildPaginationMeta(pagination.page, pagination.limit, total);

  return sendPaginated(res, {
    statusCode: 200,
    message: "Emergency contacts retrieved successfully.",
    data: contacts,
    meta,
  });
});

/**
 * PUT /api/emergency-contacts/:id
 */
const update = asyncHandler(async (req, res) => {
  const { valid, errors, data } = validateUpdateEmergencyContact(req.body);
  if (!valid) {
    return sendError(res, {
      statusCode: 422,
      message: "Validation failed.",
      errors,
    });
  }

  const contact = await emergencyContactService.updateEmergencyContact(
    req.user.id,
    req.params.id,
    data
  );

  return sendSuccess(res, {
    statusCode: 200,
    message: "Emergency contact updated successfully.",
    data: contact,
  });
});

/**
 * DELETE /api/emergency-contacts/:id
 */
const remove = asyncHandler(async (req, res) => {
  await emergencyContactService.deleteEmergencyContact(
    req.user.id,
    req.params.id
  );

  return sendSuccess(res, {
    statusCode: 200,
    message: "Emergency contact deleted successfully.",
  });
});

module.exports = {
  create,
  list,
  update,
  remove,
};
