// Incident report service.
//
// Business logic for the Incident Report module. Mirrors the auth/emergency
// contact service pattern, including a ServiceError carrying an HTTP status.
//
// Read access is public to every authenticated user (the feed and the detail
// endpoint). Write access (PUT/DELETE) stays owner-only and answers 404 for
// reports owned by someone else. The author's identity is hidden on
// anonymous reports for everyone except the owner.

const { Prisma } = require("@prisma/client");
const prisma = require("../config/prisma");
const { ServiceError } = require("../utils/service-error");

const REPORT_FIELDS = {
  id: true,
  userId: true,
  category: true,
  title: true,
  description: true,
  latitude: true,
  longitude: true,
  status: true,
  statusNote: true,
  isAnonymous: true,
  createdAt: true,
  updatedAt: true,
};

// Public author fields (never password, email, phone or location).
const AUTHOR_FIELDS = {
  id: true,
  username: true,
  fullName: true,
  profilePicture: true,
};

const MEDIA_FIELDS = {
  id: true,
  mediaUrl: true,
  mediaType: true,
  createdAt: true,
};

// Shape used by the feed and the detail endpoint: report + author + the
// oldest media item (the "cover").
const PUBLIC_REPORT_SELECT = {
  ...REPORT_FIELDS,
  user: { select: AUTHOR_FIELDS },
  media: {
    select: MEDIA_FIELDS,
    orderBy: { createdAt: "asc" },
    take: 1,
  },
};

const EARTH_RADIUS_KM = 6371;
const KM_PER_DEGREE_LAT = 111.32;

/**
 * Convert a report's Decimal latitude/longitude to plain numbers so the JSON
 * response contains numeric coordinates rather than Decimal strings.
 * @param {object|null} report
 * @returns {object|null}
 */
function serializeReport(report) {
  if (!report) return report;
  return {
    ...report,
    latitude: report.latitude === null ? null : Number(report.latitude),
    longitude: report.longitude === null ? null : Number(report.longitude),
  };
}

/**
 * Turn a report loaded with PUBLIC_REPORT_SELECT into the public shape:
 * `author` (null when anonymous and the viewer is not the owner), `media`
 * (first item or null), and placeholder social fields until likes/comments
 * get their API (roadmap 3.1).
 *
 * userId is dropped for anonymous reports viewed by others, otherwise it
 * would reveal the author.
 *
 * @param {object} report
 * @param {string} viewerId
 * @param {number} [distanceKm] - Only present on location-filtered feeds.
 * @returns {object}
 */
function toPublicReport(report, viewerId, distanceKm) {
  const { user, media, ...rest } = serializeReport(report);
  const isOwner = report.userId === viewerId;
  const hideAuthor = report.isAnonymous && !isOwner;

  const result = {
    ...rest,
    userId: hideAuthor ? null : rest.userId,
    author: hideAuthor ? null : user,
    media: media && media.length > 0 ? media[0] : null,
    likeCount: 0,
    commentCount: 0,
    likedByMe: false,
  };
  if (distanceKm !== undefined) {
    result.distanceKm = Math.round(distanceKm * 1000) / 1000;
  }
  return result;
}

/**
 * Create an incident report for the authenticated user.
 *
 * `status` is never taken from the input: the schema default (SUBMITTED)
 * applies, and it is also set explicitly here so the rule does not depend on
 * the schema alone.
 * @param {string} userId
 * @param {object} data - Sanitized { category, title, description?, latitude, longitude, isAnonymous? }.
 * @returns {Promise<object>} The created report.
 */
async function createReport(userId, data) {
  const report = await prisma.incidentReport.create({
    data: {
      userId,
      category: data.category,
      title: data.title,
      description: data.description ?? null,
      latitude: data.latitude,
      longitude: data.longitude,
      isAnonymous: data.isAnonymous ?? false,
      status: "SUBMITTED",
    },
    select: REPORT_FIELDS,
  });
  return serializeReport(report);
}

/**
 * Compute a bounding box around a point. Used in the WHERE clause so the
 * [latitude, longitude] index can narrow rows before the exact Haversine
 * check. Longitude bounds are dropped (null) near the poles or when the box
 * crosses the antimeridian, where a simple range would be wrong.
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
 * Location-filtered feed page, via raw SQL.
 *
 * WHERE: bounding box on the indexed coordinates + optional category /
 * status / since filters. The Haversine distance is computed in a subquery's
 * SELECT, then filtered to the exact radius, sorted and paged with
 * LIMIT/OFFSET. Every value is a bound parameter (Prisma.sql tagged
 * template); nothing is concatenated into the SQL string.
 *
 * @param {object} filters - { category?, status?, since?, lat, lng, radiusKm, sort }
 * @param {{ skip: number, take: number }} pagination
 * @returns {Promise<{ rows: Array<{ id: string, distanceKm: number }>, total: number }>}
 */
async function findNearbyReportIds(filters, { skip, take }) {
  const { lat, lng, radiusKm, category, status, since, sort } = filters;
  const box = boundingBox(lat, lng, radiusKm);

  // Coordinates are DECIMAL(10,7); casting the parameters to the same type
  // keeps the comparison on numeric so the index stays usable.
  const conditions = [
    Prisma.sql`r."latitude" BETWEEN CAST(${box.minLat} AS DECIMAL(10,7)) AND CAST(${box.maxLat} AS DECIMAL(10,7))`,
  ];
  if (box.minLng !== null) {
    conditions.push(
      Prisma.sql`r."longitude" BETWEEN CAST(${box.minLng} AS DECIMAL(10,7)) AND CAST(${box.maxLng} AS DECIMAL(10,7))`
    );
  }
  if (category) {
    conditions.push(Prisma.sql`r."category" = CAST(${category} AS "incident_category")`);
  }
  if (status) {
    conditions.push(Prisma.sql`r."status" = CAST(${status} AS "report_status")`);
  }
  if (since) {
    conditions.push(Prisma.sql`r."created_at" >= ${since}`);
  }
  const where = Prisma.join(conditions, " AND ");

  // Haversine; LEAST(1, …) guards asin against rounding just above 1.
  const distance = Prisma.sql`
    ${EARTH_RADIUS_KM} * 2 * ASIN(LEAST(1, SQRT(
      POWER(SIN(RADIANS(r."latitude"::double precision - ${lat}::double precision) / 2), 2)
      + COS(RADIANS(${lat}::double precision)) * COS(RADIANS(r."latitude"::double precision))
      * POWER(SIN(RADIANS(r."longitude"::double precision - ${lng}::double precision) / 2), 2)
    )))`;

  const nearby = Prisma.sql`
    SELECT r."id", r."created_at", ${distance} AS "distance_km"
    FROM "incident_report" r
    WHERE ${where}`;

  const orderBy =
    sort === "distance"
      ? Prisma.sql`ORDER BY n."distance_km" ASC, n."created_at" DESC, n."id" ASC`
      : Prisma.sql`ORDER BY n."created_at" DESC, n."id" ASC`;

  const radius = Prisma.sql`${radiusKm}::double precision`;

  const [rows, countRows] = await Promise.all([
    prisma.$queryRaw`
      SELECT n."id", n."distance_km"
      FROM (${nearby}) n
      WHERE n."distance_km" <= ${radius}
      ${orderBy}
      LIMIT ${take} OFFSET ${skip}`,
    prisma.$queryRaw`
      SELECT COUNT(*)::int AS "total"
      FROM (${nearby}) n
      WHERE n."distance_km" <= ${radius}`,
  ]);

  return {
    rows: rows.map((row) => ({ id: row.id, distanceKm: Number(row.distance_km) })),
    total: Number(countRows[0]?.total ?? 0),
  };
}

/**
 * Public feed: every user's reports, newest first (or nearest first when
 * `sort = distance`), with optional filters.
 * @param {string} viewerId - The authenticated user.
 * @param {object} filters - Sanitized feed query (see validateFeedQuery).
 * @param {{ skip: number, take: number }} pagination
 * @returns {Promise<{ reports: object[], total: number }>}
 */
async function getFeed(viewerId, filters = {}, pagination = {}) {
  const { skip = 0, take = 10 } = pagination;
  const since =
    filters.days !== undefined
      ? new Date(Date.now() - filters.days * 24 * 60 * 60 * 1000)
      : undefined;

  if (filters.lat !== undefined && filters.lng !== undefined) {
    const { rows, total } = await findNearbyReportIds(
      { ...filters, since },
      { skip, take }
    );
    if (rows.length === 0) {
      return { reports: [], total };
    }

    const found = await prisma.incidentReport.findMany({
      where: { id: { in: rows.map((row) => row.id) } },
      select: PUBLIC_REPORT_SELECT,
    });
    const byId = new Map(found.map((report) => [report.id, report]));

    // Keep the SQL order; skip any row deleted between the two queries.
    const reports = rows
      .filter((row) => byId.has(row.id))
      .map((row) => toPublicReport(byId.get(row.id), viewerId, row.distanceKm));
    return { reports, total };
  }

  const where = {
    ...(filters.category ? { category: filters.category } : {}),
    ...(filters.status ? { status: filters.status } : {}),
    ...(since ? { createdAt: { gte: since } } : {}),
  };

  const [reports, total] = await Promise.all([
    prisma.incidentReport.findMany({
      where,
      orderBy: [{ createdAt: "desc" }, { id: "asc" }],
      select: PUBLIC_REPORT_SELECT,
      skip,
      take,
    }),
    prisma.incidentReport.count({ where }),
  ]);

  return {
    reports: reports.map((report) => toPublicReport(report, viewerId)),
    total,
  };
}

/**
 * List the authenticated user's own reports, newest first (the former
 * behavior of GET /api/reports).
 * @param {string} userId
 * @param {object} [pagination={}] - Normalized pagination options { skip, take }.
 * @returns {Promise<{ reports: object[], total: number }>}
 */
async function getMyReports(userId, pagination = {}) {
  const { skip, take } = pagination;
  const where = { userId };

  const [reports, total] = await Promise.all([
    prisma.incidentReport.findMany({
      where,
      orderBy: { createdAt: "desc" },
      select: REPORT_FIELDS,
      ...(typeof skip === "number" ? { skip } : {}),
      ...(typeof take === "number" ? { take } : {}),
    }),
    prisma.incidentReport.count({ where }),
  ]);

  return {
    reports: reports.map(serializeReport),
    total,
  };
}

/**
 * Get one report. Any authenticated user may read it; the author is hidden on
 * anonymous reports unless the viewer is the owner.
 * @param {string} viewerId
 * @param {string} reportId
 * @returns {Promise<object>}
 */
async function getReportById(viewerId, reportId) {
  const report = await prisma.incidentReport.findUnique({
    where: { id: reportId },
    select: PUBLIC_REPORT_SELECT,
  });

  if (!report) {
    throw new ServiceError("Incident report not found.", 404);
  }

  return toPublicReport(report, viewerId);
}

/**
 * Update one report owned by the authenticated user.
 *
 * Returns 404 if the report does not exist OR belongs to another user, so we
 * never reveal which reports a user may write to. `status` is not updatable
 * here (the validator rejects it; it is also stripped defensively).
 * @param {string} userId
 * @param {string} reportId
 * @param {object} updates - Sanitized subset of report fields.
 * @returns {Promise<object>} The updated report.
 */
async function updateReport(userId, reportId, updates) {
  const existing = await prisma.incidentReport.findUnique({
    where: { id: reportId },
    select: { id: true, userId: true },
  });

  if (!existing || existing.userId !== userId) {
    throw new ServiceError("Incident report not found.", 404);
  }

  const { status, statusNote, userId: ownerId, ...allowed } = updates;

  const report = await prisma.incidentReport.update({
    where: { id: reportId },
    data: allowed,
    select: REPORT_FIELDS,
  });

  return serializeReport(report);
}

/**
 * Delete one report owned by the authenticated user.
 *
 * Related ReportMedia rows are removed by the database via the schema's
 * onDelete: Cascade relation.
 * @param {string} userId
 * @param {string} reportId
 * @returns {Promise<void>}
 */
async function deleteReport(userId, reportId) {
  const existing = await prisma.incidentReport.findUnique({
    where: { id: reportId },
    select: { id: true, userId: true },
  });

  if (!existing || existing.userId !== userId) {
    throw new ServiceError("Incident report not found.", 404);
  }

  await prisma.incidentReport.delete({ where: { id: reportId } });
}

module.exports = {
  ServiceError,
  createReport,
  getFeed,
  getMyReports,
  getReportById,
  updateReport,
  deleteReport,
};
