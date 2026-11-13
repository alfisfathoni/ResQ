// Facility controller.
//
// Thin HTTP layer: validates input, delegates to the service, and shapes the
// response. No business logic here. Runs behind authenticate; handlers are
// wrapped with asyncHandler so ServiceErrors reach the global error handler.

const { sendSuccess, sendError } = require("../utils/response");
const { asyncHandler } = require("../utils/async-handler");
const facilityService = require("../services/facility.service");
const { validateNearbyQuery } = require("../validations/facility.validation");

/**
 * GET /api/facilities?lat=&lng=&radiusKm=&type=&limit=
 */
const listNearby = asyncHandler(async (req, res) => {
  const { valid, errors, data } = validateNearbyQuery(req.query);
  if (!valid) {
    return sendError(res, {
      statusCode: 422,
      message: "Validation failed.",
      errors,
    });
  }

  const facilities = await facilityService.findNearby(data);

  return sendSuccess(res, {
    statusCode: 200,
    message: "Facilities retrieved successfully.",
    data: facilities,
  });
});

/**
 * GET /api/facilities/:id
 */
const detail = asyncHandler(async (req, res) => {
  const facility = await facilityService.getFacilityById(req.params.id);

  return sendSuccess(res, {
    statusCode: 200,
    message: "Facility retrieved successfully.",
    data: facility,
  });
});

module.exports = {
  listNearby,
  detail,
};
