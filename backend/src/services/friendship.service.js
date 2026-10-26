// Friendship service.
//
// Business logic for the Friendship module. Mirrors the other services'
// pattern, including a local ServiceError carrying an HTTP status and optional
// field errors. Controllers call into this layer; this layer never touches
// req/res.
//
// A friendship is a single directional row (requester -> addressee) with a
// status of PENDING, ACCEPTED, or REJECTED. The schema enforces uniqueness on
// (requesterId, addresseeId), but a relationship between two users can exist
// in either direction, so existence checks here consider BOTH directions.

const prisma = require("../config/prisma");
const { ServiceError } = require("../utils/service-error");
const notificationService = require("./notification.service");

// Minimal, non-sensitive user shape embedded in friendship responses.
const USER_PREVIEW = {
  id: true,
  fullName: true,
  username: true,
  profilePicture: true,
};

const FRIENDSHIP_FIELDS = {
  id: true,
  requesterId: true,
  addresseeId: true,
  status: true,
  createdAt: true,
  updatedAt: true,
  requester: { select: USER_PREVIEW },
  addressee: { select: USER_PREVIEW },
};

/**
 * Send a friend request from the authenticated user to another user.
 *
 * Business rules:
 *  - cannot friend yourself (400),
 *  - the target user must exist (404),
 *  - if an ACCEPTED relationship already exists in either direction → 409,
 *  - if a PENDING request already exists in either direction → 409,
 *  - if a REJECTED row exists in the same direction, it is revived to PENDING;
 *    otherwise a new PENDING row is created.
 *
 * @param {string} requesterId - The authenticated user's id.
 * @param {string} addresseeId - The target user's id.
 * @returns {Promise<object>} The created or revived friendship.
 */
async function sendRequest(requesterId, addresseeId) {
  if (requesterId === addresseeId) {
    throw new ServiceError("You cannot send a friend request to yourself.", 400, {
      addresseeId: "You cannot send a friend request to yourself.",
    });
  }

  const target = await prisma.user.findUnique({
    where: { id: addresseeId },
    select: { id: true },
  });
  if (!target) {
    throw new ServiceError("User not found.", 404);
  }

  // Look for any relationship between the two users, in either direction.
  const existing = await prisma.friendship.findFirst({
    where: {
      OR: [
        { requesterId, addresseeId },
        { requesterId: addresseeId, addresseeId: requesterId },
      ],
    },
  });

  if (existing) {
    if (existing.status === "ACCEPTED") {
      throw new ServiceError("You are already friends with this user.", 409);
    }
    if (existing.status === "PENDING") {
      throw new ServiceError("A friend request already exists between you and this user.", 409);
    }
    // existing.status === "REJECTED"
    if (existing.requesterId === requesterId) {
      // Same direction: revive the rejected row as a fresh pending request.
      const revived = await prisma.friendship.update({
        where: { id: existing.id },
        data: { status: "PENDING" },
        select: FRIENDSHIP_FIELDS,
      });
      await notifyFriendRequest(revived);
      return revived;
    }
    // Reverse direction was rejected; the (requesterId, addresseeId) slot is
    // free, so a new request in this direction can be created below.
  }

  const created = await prisma.friendship.create({
    data: {
      requesterId,
      addresseeId,
      status: "PENDING",
    },
    select: FRIENDSHIP_FIELDS,
  });

  await notifyFriendRequest(created);
  return created;
}

/**
 * Best-effort notification for a newly created/revived friend request.
 *
 * Receiver = addressee, sender = requester, type = FRIEND_REQUEST. Delegates
 * to the notification service, which swallows and logs any failure so the
 * friend request itself is never broken by a notification problem.
 * @param {object} friendship - A row selected with FRIENDSHIP_FIELDS.
 * @returns {Promise<void>}
 */
async function notifyFriendRequest(friendship) {
  const requesterName = friendship.requester
    ? friendship.requester.fullName
    : "Someone";

  await notificationService.createNotification({
    userId: friendship.addresseeId,
    senderId: friendship.requesterId,
    type: "FRIEND_REQUEST",
    title: "New friend request",
    message: `${requesterName} sent you a friend request.`,
  });
}

/**
 * List the authenticated user's accepted friends.
 *
 * Returns only ACCEPTED friendships where the user is either the requester or
 * the addressee, newest first.
 * @param {string} userId
 * @param {object} [pagination={}] - Normalized pagination options { skip, take }.
 * @returns {Promise<{ friends: object[], total: number }>}
 */
async function getFriends(userId, pagination = {}) {
  const { skip, take } = pagination;
  const where = {
    status: "ACCEPTED",
    OR: [{ requesterId: userId }, { addresseeId: userId }],
  };

  const [friends, total] = await Promise.all([
    prisma.friendship.findMany({
      where,
      orderBy: { updatedAt: "desc" },
      select: FRIENDSHIP_FIELDS,
      ...(typeof skip === "number" ? { skip } : {}),
      ...(typeof take === "number" ? { take } : {}),
    }),
    prisma.friendship.count({ where }),
  ]);

  return { friends, total };
}

/**
 * List pending friend requests addressed TO the authenticated user.
 *
 * Only PENDING requests where the user is the addressee are returned, so a
 * user never sees requests meant for someone else.
 * @param {string} userId
 * @param {object} [pagination={}] - Normalized pagination options { skip, take }.
 * @returns {Promise<{ requests: object[], total: number }>}
 */
async function getIncomingRequests(userId, pagination = {}) {
  const { skip, take } = pagination;
  const where = {
    status: "PENDING",
    addresseeId: userId,
  };

  const [requests, total] = await Promise.all([
    prisma.friendship.findMany({
      where,
      orderBy: { createdAt: "desc" },
      select: FRIENDSHIP_FIELDS,
      ...(typeof skip === "number" ? { skip } : {}),
      ...(typeof take === "number" ? { take } : {}),
    }),
    prisma.friendship.count({ where }),
  ]);

  return { requests, total };
}

/**
 * Load a friendship and assert it can be acted on by this recipient.
 *
 * Returns 404 when the row does not exist OR the user is not its addressee, so
 * the existence of other users' requests is never revealed. Returns 409 when
 * the row exists and is addressed to the user but is not PENDING.
 * @param {string} userId
 * @param {string} friendshipId
 * @returns {Promise<object>} The pending friendship row.
 */
async function loadActionableRequest(userId, friendshipId) {
  const friendship = await prisma.friendship.findUnique({
    where: { id: friendshipId },
    select: { id: true, addresseeId: true, status: true },
  });

  // Not found, or the user is not the recipient → 404 either way.
  if (!friendship || friendship.addresseeId !== userId) {
    throw new ServiceError("Friend request not found.", 404);
  }

  if (friendship.status !== "PENDING") {
    throw new ServiceError("This friend request is no longer pending.", 409);
  }

  return friendship;
}

/**
 * Accept a pending friend request. Only the recipient (addressee) may accept.
 * @param {string} userId
 * @param {string} friendshipId
 * @returns {Promise<object>} The accepted friendship.
 */
async function acceptRequest(userId, friendshipId) {
  await loadActionableRequest(userId, friendshipId);

  const accepted = await prisma.friendship.update({
    where: { id: friendshipId },
    data: { status: "ACCEPTED" },
    select: FRIENDSHIP_FIELDS,
  });

  // Notify the original requester that their request was accepted.
  // Receiver = requester, sender = accepter (the addressee). Best-effort.
  const accepterName = accepted.addressee
    ? accepted.addressee.fullName
    : "Someone";
  await notificationService.createNotification({
    userId: accepted.requesterId,
    senderId: accepted.addresseeId,
    type: "FRIEND_ACCEPTED",
    title: "Friend request accepted",
    message: `${accepterName} accepted your friend request.`,
  });

  return accepted;
}

/**
 * Reject a pending friend request. Only the recipient (addressee) may reject.
 * @param {string} userId
 * @param {string} friendshipId
 * @returns {Promise<object>} The rejected friendship.
 */
async function rejectRequest(userId, friendshipId) {
  await loadActionableRequest(userId, friendshipId);

  return prisma.friendship.update({
    where: { id: friendshipId },
    data: { status: "REJECTED" },
    select: FRIENDSHIP_FIELDS,
  });
}

/**
 * Delete (unfriend) an accepted friendship involving the authenticated user.
 *
 * Returns 404 when the row does not exist, is not ACCEPTED, or does not
 * involve the user (as requester or addressee).
 * @param {string} userId
 * @param {string} friendshipId
 * @returns {Promise<void>}
 */
async function deleteFriendship(userId, friendshipId) {
  const friendship = await prisma.friendship.findUnique({
    where: { id: friendshipId },
    select: { id: true, requesterId: true, addresseeId: true, status: true },
  });

  const involvesUser =
    friendship &&
    (friendship.requesterId === userId || friendship.addresseeId === userId);

  if (!friendship || !involvesUser || friendship.status !== "ACCEPTED") {
    throw new ServiceError("Friendship not found.", 404);
  }

  await prisma.friendship.delete({ where: { id: friendshipId } });
}

module.exports = {
  ServiceError,
  sendRequest,
  getFriends,
  getIncomingRequests,
  acceptRequest,
  rejectRequest,
  deleteFriendship,
};
