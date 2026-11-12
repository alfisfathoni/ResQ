// Location service.
//
// Writes the authenticated user's live location (User.lastLatitude,
// lastLongitude, locationUpdatedAt). A position is only accepted while the
// user owns an active, unexpired sharing session, so the location is never
// stored when nobody is allowed to see it. Viewers read it through the
// sharing-session service.

const prisma = require("../config/prisma");
const { ServiceError } = require("../utils/service-error");
const sharingSessionService = require("./sharing-session.service");

/**
 * Update the user's live location.
 *
 * Runs lazy expiry first, then — under the same per-owner lock used by
 * start/stop — checks for an active session and writes the position. Holding
 * the lock means a concurrent stop cannot interleave and leave a stale
 * location behind after the session ended.
 *
 * @param {string} userId
 * @param {{ latitude: number, longitude: number }} data
 * @returns {Promise<{ latitude: number, longitude: number, updatedAt: Date }>}
 * @throws {ServiceError} 409 when the user has no active sharing session.
 */
async function updateLocation(userId, { latitude, longitude }) {
  await sharingSessionService.expireStaleSessions({ ownerId: userId });

  const user = await sharingSessionService.withOwnerLock(userId, async (tx) => {
    const now = new Date();
    const activeCount = await tx.sharingSession.count({
      where: {
        ownerId: userId,
        isActive: true,
        OR: [{ expiresAt: null }, { expiresAt: { gte: now } }],
      },
    });
    if (activeCount === 0) {
      throw new ServiceError(
        "You have no active sharing session. Start one before sending your location.",
        409
      );
    }

    return tx.user.update({
      where: { id: userId },
      data: {
        lastLatitude: latitude,
        lastLongitude: longitude,
        locationUpdatedAt: now,
      },
      select: { lastLatitude: true, lastLongitude: true, locationUpdatedAt: true },
    });
  });

  return {
    latitude: Number(user.lastLatitude),
    longitude: Number(user.lastLongitude),
    updatedAt: user.locationUpdatedAt,
  };
}

module.exports = {
  ServiceError,
  updateLocation,
};
