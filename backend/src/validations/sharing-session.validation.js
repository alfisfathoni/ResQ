// Sharing session request validation.
//
// Follows the same hand-rolled pattern as the other validators, returning
// { valid, errors, data }. `data` only contains values that passed
// validation, already normalized (coordinates as numbers, ids de-duplicated,
// strings trimmed).

const DEFAULT_DURATION_MINUTES = 120;
const MAX_DURATION_MINUTES = 720;
const MAX_DESTINATION_NAME_LENGTH = 255;

/**
 * True when value is a plain (non-array, non-null) object.
 * @param {*} value
 * @returns {boolean}
 */
function isPlainObject(value) {
  return value !== null && typeof value === "object" && !Array.isArray(value);
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
 * Validate a required { latitude, longitude } pair on `source`, writing
 * errors under `${prefix}latitude` / `${prefix}longitude`.
 * @param {object} source
 * @param {Record<string,string>} errors
 * @param {string} [prefix=""]
 * @returns {{ latitude?: number, longitude?: number }}
 */
function validateCoordinates(source, errors, prefix = "") {
  const out = {};

  if (source.latitude === undefined || source.latitude === null || source.latitude === "") {
    errors[`${prefix}latitude`] = "Latitude is required.";
  } else {
    const lat = parseCoordinate(source.latitude, -90, 90);
    if (lat === null) {
      errors[`${prefix}latitude`] = "Latitude must be a decimal between -90 and 90.";
    } else {
      out.latitude = lat;
    }
  }

  if (source.longitude === undefined || source.longitude === null || source.longitude === "") {
    errors[`${prefix}longitude`] = "Longitude is required.";
  } else {
    const lng = parseCoordinate(source.longitude, -180, 180);
    if (lng === null) {
      errors[`${prefix}longitude`] = "Longitude must be a decimal between -180 and 180.";
    } else {
      out.longitude = lng;
    }
  }

  return out;
}

/**
 * Validate the POST /api/sharing-sessions payload.
 *
 * All fields are optional; an empty body starts a 120-minute session shared
 * with every ACCEPTED friend and no destination.
 * - viewerIds: non-empty array of non-empty string ids (duplicates removed).
 *   Whether each id is an ACCEPTED friend is checked by the service.
 * - durationMinutes: integer 1..720 (default 120).
 * - destination: { name?, latitude, longitude } or null.
 *
 * @param {object} body
 * @returns {{ valid: boolean, errors: Record<string,string>, data: { viewerIds?: string[], durationMinutes: number, destination: { name: string|null, latitude: number, longitude: number }|null } }}
 */
function validateCreateSharingSession(body = {}) {
  const errors = {};
  const data = { durationMinutes: DEFAULT_DURATION_MINUTES, destination: null };

  // express.json() leaves body as {} when nothing is sent, which is valid.
  if (!isPlainObject(body)) {
    return { valid: false, errors: { _: "Request body must be a JSON object." }, data };
  }

  if (body.viewerIds !== undefined && body.viewerIds !== null) {
    if (!Array.isArray(body.viewerIds) || body.viewerIds.length === 0) {
      errors.viewerIds = "viewerIds must be a non-empty array of user ids.";
    } else if (
      !body.viewerIds.every((id) => typeof id === "string" && id.trim().length > 0)
    ) {
      errors.viewerIds = "Every viewerId must be a non-empty string.";
    } else {
      data.viewerIds = [...new Set(body.viewerIds.map((id) => id.trim()))];
    }
  }

  if (body.durationMinutes !== undefined && body.durationMinutes !== null) {
    const minutes = body.durationMinutes;
    if (!Number.isInteger(minutes) || minutes < 1 || minutes > MAX_DURATION_MINUTES) {
      errors.durationMinutes = `durationMinutes must be an integer between 1 and ${MAX_DURATION_MINUTES}.`;
    } else {
      data.durationMinutes = minutes;
    }
  }

  if (body.destination !== undefined && body.destination !== null) {
    if (!isPlainObject(body.destination)) {
      errors.destination = "destination must be an object { name, latitude, longitude }.";
    } else {
      const dest = body.destination;
      const coords = validateCoordinates(dest, errors, "destination.");

      let name = null;
      if (dest.name !== undefined && dest.name !== null) {
        if (typeof dest.name !== "string") {
          errors["destination.name"] = "Destination name must be a string.";
        } else if (dest.name.trim().length > MAX_DESTINATION_NAME_LENGTH) {
          errors["destination.name"] = `Destination name must be at most ${MAX_DESTINATION_NAME_LENGTH} characters.`;
        } else {
          name = dest.name.trim() || null;
        }
      }

      if (coords.latitude !== undefined && coords.longitude !== undefined) {
        data.destination = { name, latitude: coords.latitude, longitude: coords.longitude };
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
 * Validate the PATCH /api/sharing-sessions/:id/stop payload.
 *
 * Body is optional. `arrived`, when sent, must be a boolean.
 * @param {object} body
 * @returns {{ valid: boolean, errors: Record<string,string>, data: { arrived: boolean } }}
 */
function validateStopSharingSession(body = {}) {
  const errors = {};
  const data = { arrived: false };

  // An absent body (undefined in Express 5) falls back to the {} default.
  if (!isPlainObject(body)) {
    return { valid: false, errors: { _: "Request body must be a JSON object." }, data };
  }

  if (body.arrived !== undefined && body.arrived !== null) {
    if (typeof body.arrived !== "boolean") {
      errors.arrived = "arrived must be a boolean.";
    } else {
      data.arrived = body.arrived;
    }
  }

  return {
    valid: Object.keys(errors).length === 0,
    errors,
    data,
  };
}

module.exports = {
  DEFAULT_DURATION_MINUTES,
  MAX_DURATION_MINUTES,
  isPlainObject,
  validateCoordinates,
  validateCreateSharingSession,
  validateStopSharingSession,
};
