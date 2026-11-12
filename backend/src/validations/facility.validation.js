// Facility request validation.
//
// Plain validators (no external library). Each function returns:
//   { valid: boolean, errors: { field: "message" }, data: { ...cleanValues } }
// `data` only contains values that passed validation, already normalized.

// Mirrors enum FacilityType in prisma/schema.prisma.
const FACILITY_TYPES = [
  "POLICE",
  "HOSPITAL",
  "CLINIC",
  "FIRE_STATION",
  "BUS_STOP",
  "TRAIN_STATION",
];

const DEFAULT_RADIUS_KM = 5;
const MAX_RADIUS_KM = 50;
const DEFAULT_LIMIT = 30;
const MAX_LIMIT = 100;

/**
 * Parse a number given as a number or numeric string, within [min, max].
 * @param {*} value
 * @param {number} min
 * @param {number} max
 * @returns {number|null} The number, or null when missing/invalid/out of range.
 */
function parseNumber(value, min, max) {
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
 * Validate the GET /api/facilities query string.
 *
 * - lat, lng: required (-90..90, -180..180).
 * - radiusKm: optional, > 0 and <= 50 (default 5).
 * - type: optional, one or more FacilityType values, comma-separated
 *   (`type=POLICE,HOSPITAL`) and/or repeated (`type=POLICE&type=HOSPITAL`).
 *   Duplicates are removed.
 * - limit: optional integer 1..100 (default 30).
 *
 * @param {object} query - req.query
 * @returns {{ valid: boolean, errors: Record<string,string>, data: { lat?: number, lng?: number, radiusKm: number, types?: string[], limit: number } }}
 */
function validateNearbyQuery(query = {}) {
  const errors = {};
  const data = { radiusKm: DEFAULT_RADIUS_KM, limit: DEFAULT_LIMIT };

  const has = (key) =>
    query[key] !== undefined && query[key] !== null && query[key] !== "";

  if (!has("lat")) {
    errors.lat = "lat is required.";
  } else {
    const lat = parseNumber(query.lat, -90, 90);
    if (lat === null) errors.lat = "lat must be a decimal between -90 and 90.";
    else data.lat = lat;
  }

  if (!has("lng")) {
    errors.lng = "lng is required.";
  } else {
    const lng = parseNumber(query.lng, -180, 180);
    if (lng === null) errors.lng = "lng must be a decimal between -180 and 180.";
    else data.lng = lng;
  }

  if (has("radiusKm")) {
    const radius = parseNumber(query.radiusKm, 0, MAX_RADIUS_KM);
    if (radius === null || radius <= 0) {
      errors.radiusKm = `radiusKm must be a number greater than 0 and at most ${MAX_RADIUS_KM}.`;
    } else {
      data.radiusKm = radius;
    }
  }

  if (has("type")) {
    const raw = Array.isArray(query.type) ? query.type : [query.type];
    if (!raw.every((v) => typeof v === "string")) {
      errors.type = "type must be a comma-separated list of facility types.";
    } else {
      const types = raw
        .flatMap((v) => v.split(","))
        .map((v) => v.trim())
        .filter((v) => v.length > 0);
      const invalid = types.filter((t) => !FACILITY_TYPES.includes(t));
      if (types.length === 0) {
        errors.type = "type must contain at least one facility type.";
      } else if (invalid.length > 0) {
        errors.type = `Invalid type: ${invalid.join(", ")}. Allowed: ${FACILITY_TYPES.join(", ")}.`;
      } else {
        data.types = [...new Set(types)];
      }
    }
  }

  if (has("limit")) {
    const limit = Number(query.limit);
    if (!Number.isInteger(limit) || limit < 1 || limit > MAX_LIMIT) {
      errors.limit = `limit must be an integer between 1 and ${MAX_LIMIT}.`;
    } else {
      data.limit = limit;
    }
  }

  return {
    valid: Object.keys(errors).length === 0,
    errors,
    data,
  };
}

module.exports = {
  FACILITY_TYPES,
  DEFAULT_RADIUS_KM,
  MAX_RADIUS_KM,
  DEFAULT_LIMIT,
  MAX_LIMIT,
  validateNearbyQuery,
};
