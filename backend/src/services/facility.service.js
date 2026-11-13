// Facility service.
//
// Read-only access to public-safety facilities (police, hospitals, clinics,
// fire stations, bus stops, train stations) for the map. Controllers call
// into this layer; this layer never touches req/res. Admin CRUD is a later
// roadmap task (2.3).
//
// The nearby search uses raw SQL: a bounding box in WHERE (so the
// [latitude, longitude] index narrows rows) and the Haversine distance in
// SELECT, then the exact radius filter, ordering by distance and LIMIT. All
// values are bound parameters via Prisma.sql; nothing is concatenated.

const { Prisma } = require("@prisma/client");
const prisma = require("../config/prisma");
const { ServiceError } = require("../utils/service-error");
const { boundingBoxConditions, haversineKmSql } = require("../utils/geo");

const FACILITY_FIELDS = {
  id: true,
  name: true,
  type: true,
  address: true,
  phone: true,
  latitude: true,
  longitude: true,
  createdAt: true,
  updatedAt: true,
};

/**
 * Convert a facility's Decimal coordinates (and distance, when present) to
 * plain numbers so the JSON response is numeric.
 * @param {object|null} facility
 * @returns {object|null}
 */
function serializeFacility(facility) {
  if (!facility) return facility;
  const result = {
    ...facility,
    latitude: facility.latitude === null ? null : Number(facility.latitude),
    longitude: facility.longitude === null ? null : Number(facility.longitude),
  };
  if (facility.distanceKm !== undefined) {
    result.distanceKm = Number(facility.distanceKm);
  }
  return result;
}

/**
 * Facilities within `radiusKm` of a point, nearest first.
 * @param {object} params
 * @param {number} params.lat
 * @param {number} params.lng
 * @param {number} params.radiusKm
 * @param {string[]} [params.types] - FacilityType filter (any of).
 * @param {number} params.limit
 * @returns {Promise<object[]>} Facilities, each with `distanceKm`.
 */
async function findNearby({ lat, lng, radiusKm, types, limit }) {
  const latCol = Prisma.sql`f."latitude"`;
  const lngCol = Prisma.sql`f."longitude"`;

  const conditions = boundingBoxConditions(latCol, lngCol, lat, lng, radiusKm);
  if (types && types.length > 0) {
    const typeList = Prisma.join(
      types.map((t) => Prisma.sql`CAST(${t} AS "facility_type")`)
    );
    conditions.push(Prisma.sql`f."type" IN (${typeList})`);
  }
  const where = Prisma.join(conditions, " AND ");
  const distance = haversineKmSql(latCol, lngCol, lat, lng);

  const rows = await prisma.$queryRaw`
    SELECT n.*
    FROM (
      SELECT
        f."id",
        f."name",
        f."type"::text AS "type",
        f."address",
        f."phone",
        f."latitude",
        f."longitude",
        f."created_at" AS "createdAt",
        f."updated_at" AS "updatedAt",
        ${distance} AS "distanceKm"
      FROM "facility" f
      WHERE ${where}
    ) n
    WHERE n."distanceKm" <= ${radiusKm}::double precision
    ORDER BY n."distanceKm" ASC, n."id" ASC
    LIMIT ${limit}`;

  return rows.map(serializeFacility);
}

/**
 * Get one facility by id.
 * @param {string} id
 * @returns {Promise<object>}
 * @throws {ServiceError} 404 when not found.
 */
async function getFacilityById(id) {
  const facility = await prisma.facility.findUnique({
    where: { id },
    select: FACILITY_FIELDS,
  });
  if (!facility) {
    throw new ServiceError("Facility not found.", 404);
  }
  return serializeFacility(facility);
}

module.exports = {
  ServiceError,
  findNearby,
  getFacilityById,
};
