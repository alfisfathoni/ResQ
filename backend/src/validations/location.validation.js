// Location request validation.
//
// Same hand-rolled { valid, errors, data } pattern as the other validators.
// Coordinate parsing is shared with the sharing-session validator.

const {
  isPlainObject,
  validateCoordinates,
} = require("./sharing-session.validation");

/**
 * Validate the PUT /api/location payload: required latitude (-90..90) and
 * longitude (-180..180), as numbers or numeric strings.
 * @param {object} body
 * @returns {{ valid: boolean, errors: Record<string,string>, data: { latitude?: number, longitude?: number } }}
 */
function validateUpdateLocation(body = {}) {
  if (!isPlainObject(body)) {
    return { valid: false, errors: { _: "Request body must be a JSON object." }, data: {} };
  }

  const errors = {};
  const data = validateCoordinates(body, errors);

  return {
    valid: Object.keys(errors).length === 0,
    errors,
    data,
  };
}

module.exports = {
  validateUpdateLocation,
};
