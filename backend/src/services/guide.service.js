// Guide service.
//
// Business logic for the Guide module. Mirrors the other services' pattern,
// including a local ServiceError carrying an HTTP status and optional field
// errors. Controllers call into this layer; this layer never touches req/res.
//
// Guides are global, admin-managed content (not user-owned), so there are no
// ownership checks here — any user may read a guide, and only admin-gated
// routes reach the create/update/delete functions.

const prisma = require("../config/prisma");
const { ServiceError } = require("../utils/service-error");

const GUIDE_FIELDS = {
  id: true,
  title: true,
  category: true,
  content: true,
  thumbnail: true,
  createdAt: true,
  updatedAt: true,
};

/**
 * Create a guide. Intended for admin-gated routes only.
 * @param {object} data - Sanitized { title, category, content, thumbnail? }.
 * @returns {Promise<object>} The created guide.
 */
async function createGuide(data) {
  return prisma.guide.create({
    data: {
      title: data.title,
      category: data.category,
      content: data.content,
      thumbnail: data.thumbnail ?? null,
    },
    select: GUIDE_FIELDS,
  });
}

/**
 * List all guides, newest first. Public.
 * @returns {Promise<object[]>}
 */
async function getGuides() {
  return prisma.guide.findMany({
    orderBy: { createdAt: "desc" },
    select: GUIDE_FIELDS,
  });
}

/**
 * Get a single guide by id. Public.
 *
 * Returns 404 if the guide does not exist.
 * @param {string} guideId
 * @returns {Promise<object>}
 */
async function getGuideById(guideId) {
  const guide = await prisma.guide.findUnique({
    where: { id: guideId },
    select: GUIDE_FIELDS,
  });

  if (!guide) {
    throw new ServiceError("Guide not found.", 404);
  }

  return guide;
}

/**
 * Update a guide. Intended for admin-gated routes only.
 *
 * Returns 404 if the guide does not exist.
 * @param {string} guideId
 * @param {object} updates - Sanitized subset of { title, category, content, thumbnail }.
 * @returns {Promise<object>} The updated guide.
 */
async function updateGuide(guideId, updates) {
  const existing = await prisma.guide.findUnique({
    where: { id: guideId },
    select: { id: true },
  });

  if (!existing) {
    throw new ServiceError("Guide not found.", 404);
  }

  return prisma.guide.update({
    where: { id: guideId },
    data: updates,
    select: GUIDE_FIELDS,
  });
}

/**
 * Delete a guide. Intended for admin-gated routes only.
 *
 * Returns 404 if the guide does not exist.
 * @param {string} guideId
 * @returns {Promise<void>}
 */
async function deleteGuide(guideId) {
  const existing = await prisma.guide.findUnique({
    where: { id: guideId },
    select: { id: true },
  });

  if (!existing) {
    throw new ServiceError("Guide not found.", 404);
  }

  await prisma.guide.delete({ where: { id: guideId } });
}

module.exports = {
  ServiceError,
  createGuide,
  getGuides,
  getGuideById,
  updateGuide,
  deleteGuide,
};
