// Sharing session service.
//
// Business logic for the Sharing Session module. Mirrors the other services'
// pattern, including a local ServiceError carrying an HTTP status and optional
// field errors. Controllers call into this layer; this layer never touches
// req/res.
//
// Data model note: the SharingSession schema stores one (ownerId, viewerId)
// pair per row. A single "sharing session" for an owner is therefore modelled
// as a set of rows that share the same startedAt — one row per accepted friend
// (viewer). This expresses "all accepted friends may view" using the existing
// schema without any change. "One active session at a time" means the owner
// has no active rows.

const prisma = require("../config/prisma");
const { ServiceError } = require("../utils/service-error");
const notificationService = require("./notification.service");

// Minimal, non-sensitive user shape embedded in sharing-session responses.
const USER_PREVIEW = {
  id: true,
  fullName: true,
  username: true,
  profilePicture: true,
};

const SESSION_FIELDS = {
  id: true,
  ownerId: true,
  viewerId: true,
  isActive: true,
  startedAt: true,
  endedAt: true,
  owner: { select: USER_PREVIEW },
  viewer: { select: USER_PREVIEW },
};

/**
 * Return the ids of users who are ACCEPTED friends of the given user, in
 * either friendship direction.
 * @param {string} userId
 * @returns {Promise<string[]>}
 */
async function getAcceptedFriendIds(userId) {
  const friendships = await prisma.friendship.findMany({
    where: {
      status: "ACCEPTED",
      OR: [{ requesterId: userId }, { addresseeId: userId }],
    },
    select: { requesterId: true, addresseeId: true },
  });

  return friendships.map((f) =>
    f.requesterId === userId ? f.addresseeId : f.requesterId
  );
}

/**
 * Start a sharing session for the authenticated user.
 *
 * Creates one active SharingSession row per accepted friend (viewer), and
 * one SHARE_SESSION notification per accepted friend. Enforces a single
 * active session per owner.
 *
 * Notification creation never breaks the main operation: a failure there is
 * logged and swallowed so the session still starts.
 *
 * @param {string} ownerId - The authenticated user's id.
 * @returns {Promise<object[]>} The created active session rows.
 */
async function startSession(ownerId) {
  // One active session at a time: reject if the owner already has active rows.
  const activeCount = await prisma.sharingSession.count({
    where: { ownerId, isActive: true },
  });
  if (activeCount > 0) {
    throw new ServiceError("You already have an active sharing session.", 409);
  }

  const friendIds = await getAcceptedFriendIds(ownerId);
  if (friendIds.length === 0) {
    throw new ServiceError(
      "You have no accepted friends to share your location with.",
      400
    );
  }

  // Shared start timestamp so all rows of this session group together.
  const startedAt = new Date();

  // Create one active row per accepted friend.
  await prisma.sharingSession.createMany({
    data: friendIds.map((viewerId) => ({
      ownerId,
      viewerId,
      isActive: true,
      startedAt,
    })),
  });

  const sessions = await prisma.sharingSession.findMany({
    where: { ownerId, isActive: true, startedAt },
    select: SESSION_FIELDS,
  });

  // Notify each accepted friend (one SHARE_SESSION notification per friend).
  // Routed through the notification service's best-effort bulk helper, which
  // logs and swallows any failure so the session start is never broken.
  const owner = await prisma.user.findUnique({
    where: { id: ownerId },
    select: { fullName: true },
  });
  const ownerName = owner ? owner.fullName : "A friend";

  await notificationService.createNotifications(
    friendIds.map((viewerId) => ({
      userId: viewerId,
      senderId: ownerId,
      type: "SHARE_SESSION",
      title: "Location sharing started",
      message: `${ownerName} started sharing their live location with you.`,
    }))
  );

  return sessions;
}

/**
 * List the authenticated user's currently active sharing sessions.
 *
 * Includes sessions the user owns and sessions where the user is the viewer.
 * @param {string} userId
 * @param {object} [pagination={}] - Normalized pagination options { skip, take }.
 * @returns {Promise<{ sessions: object[], total: number }>}
 */
async function getActiveSessions(userId, pagination = {}) {
  const { skip, take } = pagination;
  const where = {
    isActive: true,
    OR: [{ ownerId: userId }, { viewerId: userId }],
  };

  const [sessions, total] = await Promise.all([
    prisma.sharingSession.findMany({
      where,
      orderBy: { startedAt: "desc" },
      select: SESSION_FIELDS,
      ...(typeof skip === "number" ? { skip } : {}),
      ...(typeof take === "number" ? { take } : {}),
    }),
    prisma.sharingSession.count({ where }),
  ]);

  return { sessions, total };
}

/**
 * Get a single sharing session by id, enforcing visibility.
 *
 * The owner and the designated viewer (an accepted friend) may view the row.
 * Anyone else receives 404, so the existence of a session is never revealed.
 * @param {string} userId
 * @param {string} sessionId
 * @returns {Promise<object>}
 */
async function getSessionById(userId, sessionId) {
  const session = await prisma.sharingSession.findUnique({
    where: { id: sessionId },
    select: SESSION_FIELDS,
  });

  const canView =
    session && (session.ownerId === userId || session.viewerId === userId);

  if (!canView) {
    throw new ServiceError("Sharing session not found.", 404);
  }

  return session;
}

/**
 * Stop the sharing session that the given row belongs to.
 *
 * Only the owner may stop. Stopping deactivates every active row of the
 * owner's current session (all rows started together). Returns 404 when the
 * row does not exist or the user is not its owner (existence hidden), and 409
 * when the referenced row is already inactive.
 * @param {string} userId
 * @param {string} sessionId
 * @returns {Promise<object[]>} The deactivated session rows.
 */
async function stopSession(userId, sessionId) {
  const session = await prisma.sharingSession.findUnique({
    where: { id: sessionId },
    select: { id: true, ownerId: true, isActive: true, startedAt: true },
  });

  // Not found, or the user is not the owner → 404 either way.
  if (!session || session.ownerId !== userId) {
    throw new ServiceError("Sharing session not found.", 404);
  }

  if (!session.isActive) {
    throw new ServiceError("This sharing session is already stopped.", 409);
  }

  const endedAt = new Date();

  // Deactivate the whole session group (owner + shared startedAt).
  await prisma.sharingSession.updateMany({
    where: { ownerId: userId, startedAt: session.startedAt, isActive: true },
    data: { isActive: false, endedAt },
  });

  return prisma.sharingSession.findMany({
    where: { ownerId: userId, startedAt: session.startedAt },
    select: SESSION_FIELDS,
  });
}

module.exports = {
  ServiceError,
  startSession,
  getActiveSessions,
  getSessionById,
  stopSession,
};
