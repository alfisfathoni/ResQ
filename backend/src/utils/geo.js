// Geo helpers for raw-SQL distance queries.
//
// Shared by the report feed (distance filter) and the facilities search. The
// pattern is: a bounding box in the WHERE clause so the [latitude, longitude]
// index can narrow rows cheaply, then the exact Haversine distance in the
// SELECT clause. Every caller value goes in as a bound parameter through the
// Prisma.sql tagged template; nothing is concatenated into the SQL string.

const { Prisma } = require("@prisma/client");

const EARTH_RADIUS_KM = 6371;
const KM_PER_DEGREE_LAT = 111.32;

/**
 * Compute a bounding box around a point. Longitude bounds are dropped (null)
 * near the poles or when the box crosses the antimeridian, where a simple
 * range would be wrong.
 * @param {number} lat
 * @param {number} lng
 * @param {number} radiusKm
 * @returns {{ minLat: number, maxLat: number, minLng: number|null, maxLng: number|null }}
 */
function boundingBox(lat, lng, radiusKm) {
  const dLat = radiusKm / KM_PER_DEGREE_LAT;
  const minLat = Math.max(-90, lat - dLat);
  const maxLat = Math.min(90, lat + dLat);

  const cosLat = Math.cos((lat * Math.PI) / 180);
  if (cosLat < 1e-6) {
    return { minLat, maxLat, minLng: null, maxLng: null };
  }
  const dLng = radiusKm / (KM_PER_DEGREE_LAT * cosLat);
  const minLng = lng - dLng;
  const maxLng = lng + dLng;
  if (minLng < -180 || maxLng > 180) {
    return { minLat, maxLat, minLng: null, maxLng: null };
  }
  return { minLat, maxLat, minLng, maxLng };
}

/**
 * WHERE conditions restricting DECIMAL(10,7) coordinate columns to the
 * bounding box. The parameters are cast to the column type so the comparison
 * stays numeric and the index remains usable.
 * @param {import("@prisma/client").Prisma.Sql} latCol - Static column reference, e.g. Prisma.sql`f."latitude"`.
 * @param {import("@prisma/client").Prisma.Sql} lngCol - Static column reference.
 * @param {number} lat
 * @param {number} lng
 * @param {number} radiusKm
 * @returns {import("@prisma/client").Prisma.Sql[]}
 */
function boundingBoxConditions(latCol, lngCol, lat, lng, radiusKm) {
  const box = boundingBox(lat, lng, radiusKm);
  const conditions = [
    Prisma.sql`${latCol} BETWEEN CAST(${box.minLat} AS DECIMAL(10,7)) AND CAST(${box.maxLat} AS DECIMAL(10,7))`,
  ];
  if (box.minLng !== null) {
    conditions.push(
      Prisma.sql`${lngCol} BETWEEN CAST(${box.minLng} AS DECIMAL(10,7)) AND CAST(${box.maxLng} AS DECIMAL(10,7))`
    );
  }
  return conditions;
}

/**
 * Haversine distance in km between the given columns and a point, as a SQL
 * expression. LEAST(1, …) guards ASIN against rounding just above 1.
 * @param {import("@prisma/client").Prisma.Sql} latCol - Static column reference.
 * @param {import("@prisma/client").Prisma.Sql} lngCol - Static column reference.
 * @param {number} lat
 * @param {number} lng
 * @returns {import("@prisma/client").Prisma.Sql}
 */
function haversineKmSql(latCol, lngCol, lat, lng) {
  return Prisma.sql`
    ${EARTH_RADIUS_KM} * 2 * ASIN(LEAST(1, SQRT(
      POWER(SIN(RADIANS(${latCol}::double precision - ${lat}::double precision) / 2), 2)
      + COS(RADIANS(${lat}::double precision)) * COS(RADIANS(${latCol}::double precision))
      * POWER(SIN(RADIANS(${lngCol}::double precision - ${lng}::double precision) / 2), 2)
    )))`;
}

module.exports = {
  EARTH_RADIUS_KM,
  KM_PER_DEGREE_LAT,
  boundingBox,
  boundingBoxConditions,
  haversineKmSql,
};
