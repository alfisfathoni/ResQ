// User utility request validation.
//
// The user endpoints carry no request body; the only client-supplied input is
// the `q` search query on GET /api/users/search. This validator follows the
// same hand-rolled pattern as the other validators, returning
// { valid, errors, data } with `data` holding the sanitized query.

function isNonEmptyString(value) {
  return typeof value === "string" && value.trim().length > 0;
}

/**
 * Validate the GET /api/users/search query string.
 *
 * Requires a non-empty `q` keyword. The trimmed keyword is returned in `data`.
 * @param {object} query - req.query
 * @returns {{ valid: boolean, errors: Record<string,string>, data: { q: string } }}
 */
function validateUserSearch(query = {}) {
  const errors = {};
  const data = {};

  const q = typeof query.q === "string" ? query.q.trim() : "";

  if (!isNonEmptyString(q)) {
    errors.q = "Search keyword (q) is required.";
  } else {
    data.q = q;
  }

  return {
    valid: Object.keys(errors).length === 0,
    errors,
    data,
  };
}

module.exports = {
  validateUserSearch,
};
