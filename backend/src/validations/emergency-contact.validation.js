// Emergency contact request validation.
//
// Follows the same hand-rolled pattern as auth.validation.js: each validator
// returns { valid, errors, data } where `data` holds the sanitized values the
// service should persist.

// Same phone rule used in user registration: optional leading +, 7-15 digits.
const PHONE_REGEX = /^\+?\d{7,15}$/;

function isNonEmptyString(value) {
  return typeof value === "string" && value.trim().length > 0;
}

/**
 * Parse a priority value into an integer 1-5, or return null if invalid.
 * Accepts numbers and numeric strings; rejects floats and out-of-range values.
 * @param {*} value
 * @returns {number|null}
 */
function parsePriority(value) {
  if (typeof value === "number") {
    if (!Number.isInteger(value)) return null;
  } else if (typeof value === "string" && /^\d+$/.test(value.trim())) {
    value = parseInt(value.trim(), 10);
  } else {
    return null;
  }
  if (value < 1 || value > 5) return null;
  return value;
}

/**
 * Validate the POST /api/emergency-contacts payload.
 * @param {object} body
 * @returns {{ valid: boolean, errors: Record<string,string>, data: { contactName: string, phoneNumber: string, priority: number } }}
 */
function validateCreateEmergencyContact(body = {}) {
  const errors = {};
  const data = {};

  // contactName
  const contactName =
    typeof body.contactName === "string" ? body.contactName.trim() : "";
  if (!isNonEmptyString(contactName)) {
    errors.contactName = "Contact name is required.";
  } else if (contactName.length > 255) {
    errors.contactName = "Contact name must be at most 255 characters.";
  } else {
    data.contactName = contactName;
  }

  // phoneNumber
  const phoneNumber =
    typeof body.phoneNumber === "string" ? body.phoneNumber.trim() : "";
  if (!isNonEmptyString(phoneNumber)) {
    errors.phoneNumber = "Phone number is required.";
  } else if (!PHONE_REGEX.test(phoneNumber)) {
    errors.phoneNumber =
      "A valid phone number is required (7-15 digits, optional leading +).";
  } else {
    data.phoneNumber = phoneNumber;
  }

  // priority
  if (body.priority === undefined || body.priority === null || body.priority === "") {
    errors.priority = "Priority is required.";
  } else {
    const priority = parsePriority(body.priority);
    if (priority === null) {
      errors.priority = "Priority must be an integer between 1 and 5.";
    } else {
      data.priority = priority;
    }
  }

  return {
    valid: Object.keys(errors).length === 0,
    errors,
    data,
  };
}

/**
 * Validate the PUT /api/emergency-contacts/:id payload (partial update).
 *
 * Any subset of { contactName, phoneNumber, priority } may be provided, but at
 * least one is required and each supplied value must pass its field rules.
 * @param {object} body
 * @returns {{ valid: boolean, errors: Record<string,string>, data: object }}
 */
function validateUpdateEmergencyContact(body = {}) {
  const errors = {};
  const data = {};

  if (body.contactName !== undefined) {
    const contactName =
      typeof body.contactName === "string" ? body.contactName.trim() : "";
    if (!isNonEmptyString(contactName)) {
      errors.contactName = "Contact name cannot be empty.";
    } else if (contactName.length > 255) {
      errors.contactName = "Contact name must be at most 255 characters.";
    } else {
      data.contactName = contactName;
    }
  }

  if (body.phoneNumber !== undefined) {
    const phoneNumber =
      typeof body.phoneNumber === "string" ? body.phoneNumber.trim() : "";
    if (!isNonEmptyString(phoneNumber)) {
      errors.phoneNumber = "Phone number cannot be empty.";
    } else if (!PHONE_REGEX.test(phoneNumber)) {
      errors.phoneNumber =
        "A valid phone number is required (7-15 digits, optional leading +).";
    } else {
      data.phoneNumber = phoneNumber;
    }
  }

  if (body.priority !== undefined) {
    const priority = parsePriority(body.priority);
    if (priority === null) {
      errors.priority = "Priority must be an integer between 1 and 5.";
    } else {
      data.priority = priority;
    }
  }

  // Reject an empty update body.
  if (Object.keys(data).length === 0 && Object.keys(errors).length === 0) {
    errors._ =
      "Provide at least one field to update (contactName, phoneNumber, or priority).";
  }

  return {
    valid: Object.keys(errors).length === 0,
    errors,
    data,
  };
}

module.exports = {
  validateCreateEmergencyContact,
  validateUpdateEmergencyContact,
};
