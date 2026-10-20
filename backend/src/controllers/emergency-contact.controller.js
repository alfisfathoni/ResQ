// Emergency contact controller.
//
// Thin HTTP layer: validates input, delegates to the service, and shapes the
// response. No business logic here.

const { sendSuccess, sendError } = require("../utils/response");
const emergencyContactService = require("../services/emergency-contact.service");
const {
  validateCreateEmergencyContact,
  validateUpdateEmergencyContact,
} = require("../validations/emergency-contact.validation");

/**
 * Map a thrown error to a standardized error response.
 */
function handleError(res, error, context) {
  if (error instanceof emergencyContactService.ServiceError) {
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
 * POST /api/emergency-contacts
 */
async function create(req, res) {
  try {
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
  } catch (error) {
    return handleError(res, error, "Create emergency contact");
  }
}

/**
 * GET /api/emergency-contacts
 */
async function list(req, res) {
  try {
    const contacts = await emergencyContactService.getEmergencyContacts(
      req.user.id
    );

    return sendSuccess(res, {
      statusCode: 200,
      message: "Emergency contacts retrieved successfully.",
      data: contacts,
    });
  } catch (error) {
    return handleError(res, error, "List emergency contacts");
  }
}

/**
 * PUT /api/emergency-contacts/:id
 */
async function update(req, res) {
  try {
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
  } catch (error) {
    return handleError(res, error, "Update emergency contact");
  }
}

/**
 * DELETE /api/emergency-contacts/:id
 */
async function remove(req, res) {
  try {
    await emergencyContactService.deleteEmergencyContact(
      req.user.id,
      req.params.id
    );

    return sendSuccess(res, {
      statusCode: 200,
      message: "Emergency contact deleted successfully.",
    });
  } catch (error) {
    return handleError(res, error, "Delete emergency contact");
  }
}

module.exports = {
  create,
  list,
  update,
  remove,
};
