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
  "HARASSMENT",
  "PUBLIC_FACILITY",
];

// Valid ReportStatus enum values (must match prisma/schema.prisma exactly).
// Only used to filter the feed: users cannot set a status themselves.
const REPORT_STATUSES = ["SUBMITTED", "IN_REVIEW", "FORWARDED", "RESOLVED", "REJECTED"];

// Feed sort orders. `distance` needs lat/lng.
const FEED_SORTS = ["latest", "distance"];

const DEFAULT_RADIUS_KM = 5;
const MAX_RADIUS_KM = 50;
const MAX_DAYS = 365;

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
 * Parse an optional boolean given as a JSON boolean or "true"/"false".
 * @param {*} value
 * @returns {boolean|null} The boolean, or null when invalid.
 */
function parseBoolean(value) {
  if (typeof value === "boolean") return value;
  if (value === "true") return true;
  if (value === "false") return false;
  return null;
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

  // isAnonymous (optional, defaults to false in the database)
  if (body.isAnonymous !== undefined && body.isAnonymous !== null) {
    const isAnonymous = parseBoolean(body.isAnonymous);
    if (isAnonymous === null) {
      errors.isAnonymous = "isAnonymous must be a boolean.";
    } else {
      data.isAnonymous = isAnonymous;
    }
  }

  // status is never accepted from the client: new reports are always
  // SUBMITTED (schema default) and only moderation may change it.
  if (body.status !== undefined) {
    errors.status = "Status cannot be set by the reporter.";
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

  if (body.isAnonymous !== undefined) {
    const isAnonymous = parseBoolean(body.isAnonymous);
    if (isAnonymous === null) {
      errors.isAnonymous = "isAnonymous must be a boolean.";
    } else {
      data.isAnonymous = isAnonymous;
    }
  }

  // The reporter can never change the status (moderation only).
  if (body.status !== undefined) {
    errors.status = "Status cannot be updated by the reporter.";
  }

  // Reject an empty update body.
  if (Object.keys(data).length === 0 && Object.keys(errors).length === 0) {
    errors._ =
      "Provide at least one field to update (category, title, description, latitude, longitude, or isAnonymous).";
  }

  return {
    valid: Object.keys(errors).length === 0,
    errors,
    data,
  };
}

/**
 * Validate the GET /api/reports (public feed) query string.
 *
 * All filters are optional. `lat` and `lng` must be sent together; when they
 * are, `radiusKm` defaults to 5 (max 50). `days` limits the feed to reports
 * created in the last N days (1..365). `sort` is `latest` (default) or
 * `distance` (requires lat/lng). Pagination (`page`, `limit`) is parsed
 * separately by utils/pagination.js.
 *
 * @param {object} query - req.query
 * @returns {{ valid: boolean, errors: Record<string,string>, data: { category?: string, status?: string, days?: number, lat?: number, lng?: number, radiusKm?: number, sort: string } }}
 */
function validateFeedQuery(query = {}) {
  const errors = {};
  const data = {};

  const has = (key) =>
    query[key] !== undefined && query[key] !== null && query[key] !== "";

  if (has("category")) {
    if (!INCIDENT_CATEGORIES.includes(query.category)) {
      errors.category = `Category must be one of: ${INCIDENT_CATEGORIES.join(", ")}.`;
    } else {
      data.category = query.category;
    }
  }

  if (has("status")) {
    if (!REPORT_STATUSES.includes(query.status)) {
      errors.status = `Status must be one of: ${REPORT_STATUSES.join(", ")}.`;
    } else {
      data.status = query.status;
    }
  }

  if (has("days")) {
    const days = Number(query.days);
    if (!Number.isInteger(days) || days < 1 || days > MAX_DAYS) {
      errors.days = `Days must be an integer between 1 and ${MAX_DAYS}.`;
    } else {
      data.days = days;
    }
  }

  const hasLat = has("lat");
  const hasLng = has("lng");
  if (hasLat || hasLng) {
    if (!hasLat || !hasLng) {
      errors.location = "lat and lng must be provided together.";
    } else {
      const lat = parseCoordinate(query.lat, -90, 90);
      const lng = parseCoordinate(query.lng, -180, 180);
      if (lat === null) errors.lat = "lat must be a decimal between -90 and 90.";
      if (lng === null) errors.lng = "lng must be a decimal between -180 and 180.";
      if (lat !== null && lng !== null) {
        data.lat = lat;
        data.lng = lng;
      }
    }
  }

  if (has("radiusKm")) {
    if (!hasLat || !hasLng) {
      errors.radiusKm = "radiusKm requires lat and lng.";
    } else {
      const radius = parseCoordinate(query.radiusKm, 0, MAX_RADIUS_KM);
      if (radius === null || radius <= 0) {
        errors.radiusKm = `radiusKm must be a number greater than 0 and at most ${MAX_RADIUS_KM}.`;
      } else {
        data.radiusKm = radius;
      }
    }
  } else if (data.lat !== undefined) {
    data.radiusKm = DEFAULT_RADIUS_KM;
  }

  data.sort = "latest";
  if (has("sort")) {
    if (!FEED_SORTS.includes(query.sort)) {
      errors.sort = `Sort must be one of: ${FEED_SORTS.join(", ")}.`;
    } else if (query.sort === "distance" && (!hasLat || !hasLng)) {
      errors.sort = "Sorting by distance requires lat and lng.";
    } else {
      data.sort = query.sort;
    }
  }

  return {
    valid: Object.keys(errors).length === 0,
    errors,
    data,
  };
}

module.exports = {
  INCIDENT_CATEGORIES,
  REPORT_STATUSES,
  validateCreateReport,
  validateUpdateReport,
  validateFeedQuery,
};
