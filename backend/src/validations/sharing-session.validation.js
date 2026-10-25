// Sharing session request validation.
//
// Follows the same hand-rolled pattern as the other validators, returning
// { valid, errors, data }. Starting a session needs no client-supplied fields
// (it shares the owner's location with all accepted friends), so this
// validator only guards against a malformed body: when a body is sent it must
// be a plain JSON object, not an array or other non-object value.

/**
 * Validate the POST /api/sharing-sessions payload.
 *
 * The endpoint takes no required fields. An empty body is valid. A provided
 * body must be a plain object (Express parses JSON arrays/strings into
 * non-object values, which are rejected here).
 * @param {object} body
 * @returns {{ valid: boolean, errors: Record<string,string>, data: object }}
 */
function validateCreateSharingSession(body = {}) {
  const errors = {};

  // Reject non-object bodies (arrays, strings, numbers). express.json() leaves
  // body as {} when nothing is sent, which is valid.
  if (body === null || typeof body !== "object" || Array.isArray(body)) {
    errors._ = "Request body must be a JSON object.";
  }

  return {
    valid: Object.keys(errors).length === 0,
    errors,
    data: {},
  };
}

module.exports = {
  validateCreateSharingSession,
};
