// Incident report service.
//
// Business logic for the Incident Report module. Mirrors the auth/emergency
// contact service pattern, including a ServiceError carrying an HTTP status.

const prisma = require("../config/prisma");

/**
 * Error thrown by the service layer with an HTTP-ish status code and optional
 * field-level details. Controllers map this to a standardized JSON response.
 */
class ServiceError extends Error {
  constructor(message, statusCode = 400, errors = null) {
    super(message);
    this.name = "ServiceError";
    this.statusCode = statusCode;
    this.errors = errors;
  }
}

const REPORT_FIELDS = {
  id: true,
  userId: true,
  category: true,
  title: true,
  description: true,
  latitude: true,
  longitude: true,
  createdAt: true,
  updatedAt: true,
};

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
 * Create an incident report for the authenticated user.
 * @param {string} userId
 * @param {object} data - Sanitized { category, title, description?, latitude, longitude }.
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
    },
    select: REPORT_FIELDS,
  });
  return serializeReport(report);
}

/**
 * List the authenticated user's reports, newest first.
 * @param {string} userId
 * @returns {Promise<object[]>}
 */
async function getReports(userId) {
  const reports = await prisma.incidentReport.findMany({
    where: { userId },
    orderBy: { createdAt: "desc" },
    select: REPORT_FIELDS,
  });
  return reports.map(serializeReport);
}

/**
 * Get one report owned by the authenticated user.
 *
 * Returns 404 if the report does not exist OR belongs to another user, so we
 * never reveal the existence of other users' reports.
 * @param {string} userId
 * @param {string} reportId
 * @returns {Promise<object>}
 */
async function getReportById(userId, reportId) {
  const report = await prisma.incidentReport.findUnique({
    where: { id: reportId },
    select: REPORT_FIELDS,
  });

  if (!report || report.userId !== userId) {
    throw new ServiceError("Incident report not found.", 404);
  }

  return serializeReport(report);
}

/**
 * Update one report owned by the authenticated user.
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

  const report = await prisma.incidentReport.update({
    where: { id: reportId },
    data: updates,
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
  getReports,
  getReportById,
  updateReport,
  deleteReport,
};
