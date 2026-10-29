// SOS validation.
//
// Plain validators (no external library). Each function returns:
//   { valid: boolean, errors: { field: "message" }, data: { ...cleanValues } }
// `data` only contains values that passed validation, already normalized.

// Mirrors enum SosCategory in prisma/schema.prisma.
const SOS_CATEGORIES = ["POLICE", "MEDICAL", "FIRE"];

// Statuses a user may move an ACTIVE alert into.
const RESOLVE_STATUSES = ["RESOLVED", "CANCELLED"];

const MAX_MESSAGE_LENGTH = 500;

/**
 * True when value is a string with at least one non-whitespace character.
 * @param {*} value
 * @returns {boolean}
 */
function isNonEmptyString(value) {
  return typeof value === "string" && value.trim().length > 0;
}

/**
 * Parse a coordinate given as a number or numeric string.
 * @param {*} value
 * @param {number} min
 * @param {number} max
 * @returns {number|null} The number, or null when missing/invalid/out of range.
 */
function parseCoordinate(value, min, max) {
  let num;
  if (typeof value === "number") {
    num = value;
  } else if (typeof value === "string" && value.trim() !== "") {
    num = Number(value);
  } else {
    return null;
  }
  if (!Number.isFinite(num)) return null;
  if (num < min || num > max) return null;
  return num;
}

/**
 * Validate the POST /api/sos payload.
 *
 * `data.message` is only present when the client sent the field, so the
 * service can tell "not sent" (keep the old message) from "sent as null"
 * (clear it). An empty string is stored as null.
 *
 * @param {object} body
 * @returns {{ valid: boolean, errors: Record<string,string>, data: { category?: string, latitude?: number, longitude?: number, message?: string|null } }}
 */
function validateTriggerSos(body = {}) {
  const errors = {};
  const data = {};

  if (body === null || typeof body !== "object" || Array.isArray(body)) {
    return { valid: false, errors: { _: "Request body must be a JSON object." }, data };
  }

  if (!isNonEmptyString(body.category)) {
    errors.category = "Category is required.";
  } else if (!SOS_CATEGORIES.includes(body.category)) {
    errors.category = `Category must be one of: ${SOS_CATEGORIES.join(", ")}.`;
  } else {
    data.category = body.category;
  }

  if (body.latitude === undefined || body.latitude === null || body.latitude === "") {
    errors.latitude = "Latitude is required.";
  } else {
    const lat = parseCoordinate(body.latitude, -90, 90);
    if (lat === null) {
      errors.latitude = "Latitude must be a decimal between -90 and 90.";
    } else {
      data.latitude = lat;
    }
  }

  if (body.longitude === undefined || body.longitude === null || body.longitude === "") {
    errors.longitude = "Longitude is required.";
  } else {
    const lng = parseCoordinate(body.longitude, -180, 180);
    if (lng === null) {
      errors.longitude = "Longitude must be a decimal between -180 and 180.";
    } else {
      data.longitude = lng;
    }
  }

  if (body.message !== undefined) {
    if (body.message === null) {
      data.message = null;
    } else if (typeof body.message !== "string") {
      errors.message = "Message must be a string or null.";
    } else {
      const message = body.message.trim();
      if (message.length > MAX_MESSAGE_LENGTH) {
        errors.message = `Message must be at most ${MAX_MESSAGE_LENGTH} characters.`;
      } else {
        data.message = message.length > 0 ? message : null;
      }
    }
  }

  return {
    valid: Object.keys(errors).length === 0,
    errors,
    data,
  };
}

/**
 * Validate the PATCH /api/sos/:id/resolve payload.
 * @param {object} body
 * @returns {{ valid: boolean, errors: Record<string,string>, data: { status?: string } }}
 */
function validateResolveSos(body = {}) {
  const errors = {};
  const data = {};

  if (body === null || typeof body !== "object" || Array.isArray(body)) {
    return { valid: false, errors: { _: "Request body must be a JSON object." }, data };
  }

  if (!isNonEmptyString(body.status)) {
    errors.status = "Status is required.";
  } else if (!RESOLVE_STATUSES.includes(body.status)) {
    errors.status = `Status must be one of: ${RESOLVE_STATUSES.join(", ")}.`;
  } else {
    data.status = body.status;
  }

  return {
    valid: Object.keys(errors).length === 0,
    errors,
    data,
  };
}

module.exports = {
  SOS_CATEGORIES,
  RESOLVE_STATUSES,
  validateTriggerSos,
  validateResolveSos,
};
