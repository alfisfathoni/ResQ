// Sharing session service.
//
// Business logic for the Sharing Session (Safe Companion) module. Controllers
// call into this layer; this layer never touches req/res.
//
// Data model note: the SharingSession schema stores one (ownerId, viewerId)
// pair per row. A single "sharing session" for an owner is therefore modelled
// as a set of rows that share the same startedAt — one row per viewer. All
// rows of a session share the same expiresAt and destination. "One active
// session at a time" means the owner has no active rows.
//
// Live location lives on the owner's User row (lastLatitude, lastLongitude,
// locationUpdatedAt). It is only written while the owner has an active
// session (see location.service.js) and is cleared when the session stops or
// expires, so viewers never see a position after sharing has ended.
//
// Expiry is lazy (no cron): every read of a session or a location first
// deactivates rows whose expiresAt is in the past (isActive=false, endedAt
// set) and clears the affected owners' location.
//
// Concurrency: starting, stopping and location writes for one owner run in a
// transaction holding a per-owner PostgreSQL advisory lock, because the schema
// has no unique constraint for "one active session per owner".

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

// Owner's live-location fields. Only selected where the result is shaped by
// serializeSession, which strips them and exposes ownerLocation to viewers.
const OWNER_LOCATION = {
  lastLatitude: true,
  lastLongitude: true,
  locationUpdatedAt: true,
};

const SESSION_FIELDS = {
  id: true,
  ownerId: true,
  viewerId: true,
  isActive: true,
  startedAt: true,
  endedAt: true,
  expiresAt: true,
  destinationName: true,
  destinationLatitude: true,
  destinationLongitude: true,
  owner: { select: USER_PREVIEW },
  viewer: { select: USER_PREVIEW },
};

// Same as SESSION_FIELDS, plus the owner's live location (for viewer rows).
const SESSION_FIELDS_WITH_OWNER_LOCATION = {
  ...SESSION_FIELDS,
  owner: { select: { ...USER_PREVIEW, ...OWNER_LOCATION } },
};

// Location fields reset when an owner stops sharing.
const CLEARED_LOCATION = {
  lastLatitude: null,
  lastLongitude: null,
  locationUpdatedAt: null,
};

/**
 * Convert a nullable Prisma Decimal to a number.
 * @param {*} value
 * @returns {number|null}
 */
function toNumber(value) {
  return value === null || value === undefined ? null : Number(value);
}

/**
 * True when an active row is past its expiresAt (expiresAt < now).
 * @param {{ isActive: boolean, expiresAt: Date|null }} session
 * @param {Date} [now=new Date()]
 * @returns {boolean}
 */
function isExpired(session, now = new Date()) {
  return Boolean(session.isActive && session.expiresAt && session.expiresAt < now);
}

/**
 * Build the public location object from a user's location fields.
 * Returns null when the owner has not sent a position yet.
 * @param {{ lastLatitude: *, lastLongitude: *, locationUpdatedAt: Date|null }} user
 * @returns {{ latitude: number, longitude: number, updatedAt: Date|null }|null}
 */
function toLocation(user) {
  if (!user || user.lastLatitude === null || user.lastLongitude === null) {
    return null;
  }
  return {
    latitude: toNumber(user.lastLatitude),
    longitude: toNumber(user.lastLongitude),
    updatedAt: user.locationUpdatedAt,
  };
}

/**
 * Shape a session row for the API: nest the destination, convert Decimals to
 * numbers, and expose the owner's live location as `ownerLocation` only on
 * rows where `forUserId` is the viewer. The owner's raw location fields are
 * never returned inside `owner`.
 * @param {object} row - Selected with SESSION_FIELDS(_WITH_OWNER_LOCATION).
 * @param {string} [forUserId] - The requesting user.
 * @returns {object}
 */
function serializeSession(row, forUserId) {
  const {
    destinationName,
    destinationLatitude,
    destinationLongitude,
    owner,
    ...rest
  } = row;
  const { lastLatitude, lastLongitude, locationUpdatedAt, ...ownerPreview } = owner || {};

  const result = {
    ...rest,
    destination:
      destinationLatitude === null || destinationLongitude === null
        ? null
        : {
            name: destinationName,
            latitude: toNumber(destinationLatitude),
            longitude: toNumber(destinationLongitude),
          },
    owner: owner ? ownerPreview : owner,
  };

  if (forUserId && row.viewerId === forUserId && owner && "lastLatitude" in owner) {
    result.ownerLocation = toLocation({ lastLatitude, lastLongitude, locationUpdatedAt });
  }

  return result;
}

/**
 * Run `fn` in a transaction holding the per-owner advisory lock, so start,
 * stop and location writes of the same owner are serialized.
 * @template T
 * @param {string} ownerId
 * @param {(tx: import("@prisma/client").Prisma.TransactionClient) => Promise<T>} fn
 * @returns {Promise<T>}
 */
function withOwnerLock(ownerId, fn) {
  return prisma.$transaction(async (tx) => {
    // Released automatically at commit/rollback.
    await tx.$executeRaw`SELECT pg_advisory_xact_lock(hashtext(${`sharing:${ownerId}`}::text))`;
    return fn(tx);
  });
}

/**
 * Clear the live location of the given owners, but only for owners that no
 * longer have any active session (guards against clearing the location of a
 * session started concurrently).
 * @param {string[]} ownerIds
 * @param {object} [client=prisma] - Prisma client or transaction client.
 * @returns {Promise<void>}
 */
async function clearOwnerLocations(ownerIds, client = prisma) {
  if (ownerIds.length === 0) return;
  await client.user.updateMany({
    where: {
      id: { in: ownerIds },
      ownedSharingSessions: { none: { isActive: true } },
    },
    data: CLEARED_LOCATION,
  });
}

/**
 * Lazy expiry. Deactivates every active row past its expiresAt for the owners
 * that have such a row within `scope`, then clears those owners' location.
 * Expiring by owner (not by row) keeps all rows of one session consistent.
 * @param {object} [scope={}] - Extra Prisma `where` to limit which rows are inspected.
 * @returns {Promise<string[]>} Ids of owners whose session expired.
 */
async function expireStaleSessions(scope = {}) {
  const now = new Date();

  const stale = await prisma.sharingSession.findMany({
    where: { ...scope, isActive: true, expiresAt: { lt: now } },
    select: { ownerId: true },
    distinct: ["ownerId"],
  });
  if (stale.length === 0) return [];

  const ownerIds = stale.map((s) => s.ownerId);

  await prisma.sharingSession.updateMany({
    where: { ownerId: { in: ownerIds }, isActive: true, expiresAt: { lt: now } },
    data: { isActive: false, endedAt: now },
  });
  await clearOwnerLocations(ownerIds);

  return ownerIds;
}

/**
 * Return the ids of users who are ACCEPTED friends of the given user, in
 * either friendship direction.
 * @param {string} userId
 * @param {object} [client=prisma] - Prisma client or transaction client.
 * @returns {Promise<string[]>}
 */
async function getAcceptedFriendIds(userId, client = prisma) {
  const friendships = await client.friendship.findMany({
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
 * Look up a user's display name for notification text.
 * @param {string} userId
 * @returns {Promise<string>}
 */
async function getDisplayName(userId) {
  const user = await prisma.user.findUnique({
    where: { id: userId },
    select: { fullName: true },
  });
  return user ? user.fullName : "A friend";
}

/**
 * Start a sharing session for the authenticated user.
 *
 * Creates one active SharingSession row per viewer and one SHARE_SESSION
 * notification per viewer. Viewers default to all ACCEPTED friends; when
 * `viewerIds` is given, every id must be an ACCEPTED friend (else 422).
 * Enforces a single active session per owner (409).
 *
 * Notification creation never breaks the main operation: a failure there is
 * logged and swallowed so the session still starts.
 *
 * @param {string} ownerId - The authenticated user's id.
 * @param {object} [options={}]
 * @param {string[]} [options.viewerIds] - Subset of ACCEPTED friends.
 * @param {number} [options.durationMinutes=120] - Lifetime; sets expiresAt.
 * @param {{ name: string|null, latitude: number, longitude: number }|null} [options.destination]
 * @returns {Promise<object[]>} The created active session rows.
 */
async function startSession(ownerId, options = {}) {
  const { viewerIds, durationMinutes = 120, destination = null } = options;

  // An expired-but-not-yet-deactivated session must not block a new one.
  await expireStaleSessions({ ownerId });

  const { startedAt, selectedIds } = await withOwnerLock(ownerId, async (tx) => {
    const activeCount = await tx.sharingSession.count({
      where: { ownerId, isActive: true },
    });
    if (activeCount > 0) {
      throw new ServiceError("You already have an active sharing session.", 409);
    }

    const friendIds = await getAcceptedFriendIds(ownerId, tx);
    if (friendIds.length === 0) {
      throw new ServiceError(
        "You have no accepted friends to share your location with.",
        400
      );
    }

    let selected = friendIds;
    if (viewerIds) {
      const friendSet = new Set(friendIds);
      const invalid = viewerIds.filter((id) => !friendSet.has(id));
      if (invalid.length > 0) {
        throw new ServiceError("Validation failed.", 422, {
          viewerIds: `These users are not your accepted friends: ${invalid.join(", ")}.`,
        });
      }
      selected = viewerIds;
    }

    // Shared timestamps so all rows of this session group together.
    const start = new Date();
    const expiresAt = new Date(start.getTime() + durationMinutes * 60 * 1000);

    await tx.sharingSession.createMany({
      data: selected.map((viewerId) => ({
        ownerId,
        viewerId,
        isActive: true,
        startedAt: start,
        expiresAt,
        destinationName: destination ? destination.name : null,
        destinationLatitude: destination ? destination.latitude : null,
        destinationLongitude: destination ? destination.longitude : null,
      })),
    });

    return { startedAt: start, selectedIds: selected };
  });

  const sessions = await prisma.sharingSession.findMany({
    where: { ownerId, isActive: true, startedAt },
    select: SESSION_FIELDS,
  });

  // Best-effort: createNotifications logs and swallows failures.
  const ownerName = await getDisplayName(ownerId);
  await notificationService.createNotifications(
    selectedIds.map((viewerId) => ({
      userId: viewerId,
      senderId: ownerId,
      type: "SHARE_SESSION",
      title: "Location sharing started",
      message: `${ownerName} started sharing their live location with you.`,
    }))
  );

  return sessions.map((s) => serializeSession(s, ownerId));
}

/**
 * List the authenticated user's currently active sharing sessions.
 *
 * Includes sessions the user owns and sessions where the user is the viewer.
 * Rows where the user is the viewer carry `ownerLocation`
 * ({ latitude, longitude, updatedAt }, or null before the first update).
 * @param {string} userId
 * @param {object} [pagination={}] - Normalized pagination options { skip, take }.
 * @returns {Promise<{ sessions: object[], total: number }>}
 */
async function getActiveSessions(userId, pagination = {}) {
  const { skip, take } = pagination;
  const participant = { OR: [{ ownerId: userId }, { viewerId: userId }] };

  await expireStaleSessions(participant);

  const where = { isActive: true, ...participant };

  const [sessions, total] = await Promise.all([
    prisma.sharingSession.findMany({
      where,
      orderBy: { startedAt: "desc" },
      select: SESSION_FIELDS_WITH_OWNER_LOCATION,
      ...(typeof skip === "number" ? { skip } : {}),
      ...(typeof take === "number" ? { take } : {}),
    }),
    prisma.sharingSession.count({ where }),
  ]);

  return {
    sessions: sessions.map((s) => serializeSession(s, userId)),
    total,
  };
}

/**
 * Read a session row, applying lazy expiry to it first.
 * @param {string} sessionId
 * @param {object} select - Prisma select; must include isActive, expiresAt, ownerId.
 * @returns {Promise<object|null>}
 */
async function findSessionFresh(sessionId, select) {
  const session = await prisma.sharingSession.findUnique({
    where: { id: sessionId },
    select,
  });

  if (session && isExpired(session)) {
    await expireStaleSessions({ ownerId: session.ownerId });
    return prisma.sharingSession.findUnique({ where: { id: sessionId }, select });
  }

  return session;
}

/**
 * Get a single sharing session by id, enforcing visibility.
 *
 * The owner and the designated viewer may view the row. Anyone else receives
 * 404, so the existence of a session is never revealed.
 * @param {string} userId
 * @param {string} sessionId
 * @returns {Promise<object>}
 */
async function getSessionById(userId, sessionId) {
  const session = await findSessionFresh(sessionId, SESSION_FIELDS);

  const canView =
    session && (session.ownerId === userId || session.viewerId === userId);

  if (!canView) {
    throw new ServiceError("Sharing session not found.", 404);
  }

  return serializeSession(session, userId);
}

/**
 * Get the owner's latest location for a session the user is viewing.
 *
 * 404 when the session does not exist or the user is not its viewer (the
 * owner is not a viewer either); 409 when the session is stopped or expired.
 * @param {string} userId - The viewer.
 * @param {string} sessionId
 * @returns {Promise<{ sessionId: string, ownerId: string, ownerLocation: object|null }>}
 */
async function getSessionLocation(userId, sessionId) {
  const session = await findSessionFresh(sessionId, {
    id: true,
    ownerId: true,
    viewerId: true,
    isActive: true,
    expiresAt: true,
    owner: { select: OWNER_LOCATION },
  });

  if (!session || session.viewerId !== userId) {
    throw new ServiceError("Sharing session not found.", 404);
  }

  if (!session.isActive) {
    throw new ServiceError("This sharing session has ended.", 409);
  }

  return {
    sessionId: session.id,
    ownerId: session.ownerId,
    ownerLocation: toLocation(session.owner),
  };
}

/**
 * Stop the sharing session that the given row belongs to.
 *
 * Only the owner may stop. Stopping deactivates every active row of the
 * owner's current session (all rows started together) and clears the owner's
 * live location. With `arrived: true`, each viewer gets a SHARE_ARRIVED
 * notification (best-effort). Returns 404 when the row does not exist or the
 * user is not its owner (existence hidden), and 409 when the session is
 * already stopped or expired.
 * @param {string} userId
 * @param {string} sessionId
 * @param {object} [options={}]
 * @param {boolean} [options.arrived=false]
 * @returns {Promise<object[]>} The deactivated session rows.
 */
async function stopSession(userId, sessionId, options = {}) {
  const { arrived = false } = options;

  const session = await findSessionFresh(sessionId, {
    id: true,
    ownerId: true,
    isActive: true,
    startedAt: true,
    expiresAt: true,
    destinationName: true,
  });

  // Not found, or the user is not the owner → 404 either way.
  if (!session || session.ownerId !== userId) {
    throw new ServiceError("Sharing session not found.", 404);
  }

  if (!session.isActive) {
    throw new ServiceError("This sharing session is already stopped.", 409);
  }

  await withOwnerLock(userId, async (tx) => {
    // Conditional update: a concurrent stop leaves nothing to deactivate.
    const { count } = await tx.sharingSession.updateMany({
      where: { ownerId: userId, startedAt: session.startedAt, isActive: true },
      data: { isActive: false, endedAt: new Date() },
    });
    if (count === 0) {
      throw new ServiceError("This sharing session is already stopped.", 409);
    }

    await clearOwnerLocations([userId], tx);
  });

  const rows = await prisma.sharingSession.findMany({
    where: { ownerId: userId, startedAt: session.startedAt },
    select: SESSION_FIELDS,
  });

  if (arrived) {
    const ownerName = await getDisplayName(userId);
    const where = session.destinationName ? ` at ${session.destinationName}` : "";
    await notificationService.createNotifications(
      rows.map((row) => ({
        userId: row.viewerId,
        senderId: userId,
        type: "SHARE_ARRIVED",
        title: "Arrived safely",
        message: `${ownerName} has arrived safely${where}.`,
      }))
    );
  }

  return rows.map((s) => serializeSession(s, userId));
}

module.exports = {
  ServiceError,
  withOwnerLock,
  expireStaleSessions,
  startSession,
  getActiveSessions,
  getSessionById,
  getSessionLocation,
  stopSession,
};
