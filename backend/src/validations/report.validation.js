// Incident report request validation.
//
// Same hand-rolled pattern as the other validators: each returns
// { valid, errors, data } with `data` holding sanitized values.

// Valid IncidentCategory enum values (must match prisma/schema.prisma exactly).
const INCIDENT_CATEGORIES = [
  "CRIME",
  "ACCIDENT",
  "FIRE",
  "MEDICAL",
  "DISASTER",
  "SUSPICIOUS",
  "OTHER",
];

function isNonEmptyString(value) {
  return typeof value === "string" && value.trim().length > 0;
}

/**
 * Parse a coordinate into a finite number within [min, max], or null.
 * Accepts numbers and numeric strings.
 * @param {*} value
 * @param {number} min
 * @param {number} max
 * @returns {number|null}
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
 * Validate the POST /api/reports payload.
 * @param {object} body
 * @returns {{ valid: boolean, errors: Record<string,string>, data: object }}
 */
function validateCreateReport(body = {}) {
  const errors = {};
  const data = {};

  // category
  if (!isNonEmptyString(body.category)) {
    errors.category = "Category is required.";
  } else if (!INCIDENT_CATEGORIES.includes(body.category)) {
    errors.category = `Category must be one of: ${INCIDENT_CATEGORIES.join(", ")}.`;
  } else {
    data.category = body.category;
  }

  // title
  const title = typeof body.title === "string" ? body.title.trim() : "";
  if (!isNonEmptyString(title)) {
    errors.title = "Title is required.";
  } else if (title.length > 255) {
    errors.title = "Title must be at most 255 characters.";
  } else {
    data.title = title;
  }

  // description (optional)
  if (body.description !== undefined && body.description !== null) {
    if (typeof body.description !== "string") {
      errors.description = "Description must be a string.";
    } else {
      data.description = body.description.trim();
    }
  }

  // latitude
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

  // longitude
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

  return {
    valid: Object.keys(errors).length === 0,
    errors,
    data,
  };
}

/**
 * Validate the PUT /api/reports/:id payload (partial update).
 *
 * Any subset of { category, title, description, latitude, longitude } may be
 * provided, but at least one is required and each supplied value is validated.
 * @param {object} body
 * @returns {{ valid: boolean, errors: Record<string,string>, data: object }}
 */
function validateUpdateReport(body = {}) {
  const errors = {};
  const data = {};

  if (body.category !== undefined) {
    if (!INCIDENT_CATEGORIES.includes(body.category)) {
      errors.category = `Category must be one of: ${INCIDENT_CATEGORIES.join(", ")}.`;
    } else {
      data.category = body.category;
    }
  }

  if (body.title !== undefined) {
    const title = typeof body.title === "string" ? body.title.trim() : "";
    if (!isNonEmptyString(title)) {
      errors.title = "Title cannot be empty.";
    } else if (title.length > 255) {
      errors.title = "Title must be at most 255 characters.";
    } else {
      data.title = title;
    }
  }

  if (body.description !== undefined) {
    // Allow clearing the description with null.
    if (body.description === null) {
      data.description = null;
    } else if (typeof body.description === "string") {
      data.description = body.description.trim();
    } else {
      errors.description = "Description must be a string or null.";
    }
  }

  if (body.latitude !== undefined) {
    const lat = parseCoordinate(body.latitude, -90, 90);
    if (lat === null) {
      errors.latitude = "Latitude must be a decimal between -90 and 90.";
    } else {
      data.latitude = lat;
    }
  }

  if (body.longitude !== undefined) {
    const lng = parseCoordinate(body.longitude, -180, 180);
    if (lng === null) {
      errors.longitude = "Longitude must be a decimal between -180 and 180.";
    } else {
      data.longitude = lng;
    }
  }

  // Reject an empty update body.
  if (Object.keys(data).length === 0 && Object.keys(errors).length === 0) {
    errors._ =
      "Provide at least one field to update (category, title, description, latitude, or longitude).";
  }

  return {
    valid: Object.keys(errors).length === 0,
    errors,
    data,
  };
}

module.exports = {
  validateCreateReport,
  validateUpdateReport,
};
