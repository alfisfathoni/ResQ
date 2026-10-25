// Notification request validation.
//
// The notification endpoints carry no request body; the only client-supplied
// input is the :id route param on the mark-as-read and delete routes. This
// validator follows the same hand-rolled pattern as the other validators,
// returning { valid, errors, data } with `data` holding the sanitized id.

function isNonEmptyString(value) {
  return typeof value === "string" && value.trim().length > 0;
}

/**
 * Validate the :id route param used by PATCH /:id/read and DELETE /:id.
 *
 * Confirms an id was supplied; whether it refers to a notification the user
 * owns is a business rule enforced by the service (404 otherwise).
 * @param {object} params - req.params
 * @returns {{ valid: boolean, errors: Record<string,string>, data: { id: string } }}
 */
function validateNotificationId(params = {}) {
  const errors = {};
  const data = {};

  const id = typeof params.id === "string" ? params.id.trim() : "";

  if (!isNonEmptyString(id)) {
    errors.id = "Notification id is required.";
  } else {
    data.id = id;
  }

  return {
    valid: Object.keys(errors).length === 0,
    errors,
    data,
  };
}

module.exports = {
  validateNotificationId,
};
