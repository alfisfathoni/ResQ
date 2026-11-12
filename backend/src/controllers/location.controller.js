// Location controller.
//
// Thin HTTP layer over location.service. Runs behind authenticate; wrapped
// with asyncHandler so ServiceErrors reach the global error handler.

const { sendSuccess, sendError } = require("../utils/response");
const { asyncHandler } = require("../utils/async-handler");
const locationService = require("../services/location.service");
const { validateUpdateLocation } = require("../validations/location.validation");

/**
 * PUT /api/location
 */
const update = asyncHandler(async (req, res) => {
  const { valid, errors, data } = validateUpdateLocation(req.body);
  if (!valid) {
    return sendError(res, {
      statusCode: 422,
      message: "Validation failed.",
      errors,
    });
  }

  const location = await locationService.updateLocation(req.user.id, data);

  return sendSuccess(res, {
    statusCode: 200,
    message: "Location updated successfully.",
    data: location,
  });
});

module.exports = {
  update,
};
