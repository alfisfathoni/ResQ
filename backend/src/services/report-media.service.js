// Report media service.
//
// Business logic for the Report Media module. Enforces that a user may only
// manage media belonging to their own reports. Uploads to and deletes from
// Cloudinary via the cloudinary utility. Mirrors the established ServiceError
// pattern.

const prisma = require("../config/prisma");
const {
  uploadBuffer,
  deleteAsset,
  publicIdFromUrl,
} = require("../utils/cloudinary");
const { ServiceError } = require("../utils/service-error");

const MEDIA_FIELDS = {
  id: true,
  reportId: true,
  mediaUrl: true,
  mediaType: true,
  createdAt: true,
};

/**
 * Load a report and assert the authenticated user owns it.
 * Returns 404 for both "not found" and "owned by another user".
 * @param {string} userId
 * @param {string} reportId
 */
async function assertReportOwnership(userId, reportId) {
  const report = await prisma.incidentReport.findUnique({
    where: { id: reportId },
    select: { id: true, userId: true },
  });
  if (!report || report.userId !== userId) {
    throw new ServiceError("Incident report not found.", 404);
  }
}

/**
 * Upload a media file for one of the authenticated user's reports.
 * @param {object} params
 * @param {string} params.userId
 * @param {string} params.reportId
 * @param {Buffer} params.buffer - File contents from multer.
 * @param {"IMAGE"|"VIDEO"} params.mediaType
 * @param {"image"|"video"} params.resourceType
 * @returns {Promise<object>} The created media row.
 */
async function createMedia({ userId, reportId, buffer, mediaType, resourceType }) {
  await assertReportOwnership(userId, reportId);

  let uploadResult;
  try {
    uploadResult = await uploadBuffer(buffer, { resourceType });
  } catch (error) {
    console.error("Cloudinary upload failed:", error.message);
    throw new ServiceError("Failed to upload media.", 502);
  }

  const media = await prisma.reportMedia.create({
    data: {
      reportId,
      mediaUrl: uploadResult.secure_url,
      mediaType,
    },
    select: MEDIA_FIELDS,
  });

  return media;
}

/**
 * List media for one of the authenticated user's reports, oldest first.
 * @param {string} userId
 * @param {string} reportId
 * @returns {Promise<object[]>}
 */
async function getMedia(userId, reportId) {
  await assertReportOwnership(userId, reportId);

  return prisma.reportMedia.findMany({
    where: { reportId },
    orderBy: { createdAt: "asc" },
    select: MEDIA_FIELDS,
  });
}

/**
 * Delete one media item: remove the Cloudinary asset, then the DB row.
 * @param {string} userId
 * @param {string} reportId
 * @param {string} mediaId
 * @returns {Promise<void>}
 */
async function deleteMedia(userId, reportId, mediaId) {
  await assertReportOwnership(userId, reportId);

  const media = await prisma.reportMedia.findUnique({
    where: { id: mediaId },
    select: { id: true, reportId: true, mediaUrl: true, mediaType: true },
  });

  // Media must exist AND belong to the given report.
  if (!media || media.reportId !== reportId) {
    throw new ServiceError("Media not found.", 404);
  }

  // Best-effort delete of the Cloudinary asset. Don't block DB cleanup if the
  // remote delete fails (e.g. asset already gone); log for diagnostics.
  const publicId = publicIdFromUrl(media.mediaUrl);
  if (publicId) {
    const resourceType = media.mediaType === "VIDEO" ? "video" : "image";
    try {
      await deleteAsset(publicId, resourceType);
    } catch (error) {
      console.error("Cloudinary delete failed:", error.message);
    }
  }

  await prisma.reportMedia.delete({ where: { id: mediaId } });
}

module.exports = {
  ServiceError,
  createMedia,
  getMedia,
  deleteMedia,
};
