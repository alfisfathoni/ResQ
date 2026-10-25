// Friendship request validation.
//
// Same hand-rolled pattern as the other validators: each function receives the
// request body and returns { valid, errors, data } where `data` holds the
// sanitized values the service should use. Existence, self-request, and
// duplicate checks are business rules and live in the service layer.

function isNonEmptyString(value) {
  return typeof value === "string" && value.trim().length > 0;
}

/**
 * Validate the POST /api/friendships/request payload.
 *
 * Only the target user's id (addresseeId) is required here. Whether that user
 * exists, is not the requester, and has no existing relationship is enforced
 * by the service.
 * @param {object} body
 * @returns {{ valid: boolean, errors: Record<string,string>, data: { addresseeId: string } }}
 */
function validateCreateFriendRequest(body = {}) {
  const errors = {};
  const data = {};

  const addresseeId =
    typeof body.addresseeId === "string" ? body.addresseeId.trim() : "";

  if (!isNonEmptyString(addresseeId)) {
    errors.addresseeId = "Addressee id is required.";
  } else {
    data.addresseeId = addresseeId;
  }

  return {
    valid: Object.keys(errors).length === 0,
    errors,
    data,
  };
}

module.exports = {
  validateCreateFriendRequest,
};
