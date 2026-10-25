// User utility service.
//
// Business logic for the User Utility module (search, public detail with
// friendship status, and a user's accepted friends). Mirrors the other
// services' pattern, including a local ServiceError carrying an HTTP status.
// Controllers call into this layer; this layer never touches req/res.
//
// Only public profile fields are ever returned. Sensitive fields (password,
// email, phoneNumber, role, isVerified, location) are never selected, so they
// can never leak through this module.

const prisma = require("../config/prisma");
const { ServiceError } = require("../utils/service-error");

// The only fields considered public for this module.
const PUBLIC_USER_FIELDS = {
  id: true,
  fullName: true,
  username: true,
  profilePicture: true,
  createdAt: true,
};

/**
 * Search users by username or full name (case-insensitive), excluding the
 * authenticated user. Results are ordered alphabetically by username. Returns
 * an empty array when nothing matches.
 * @param {string} userId - The authenticated user's id (excluded from results).
 * @param {string} keyword - The sanitized, non-empty search term.
 * @returns {Promise<object[]>}
 */
async function searchUsers(userId, keyword) {
  return prisma.user.findMany({
    where: {
      id: { not: userId },
      OR: [
        { username: { contains: keyword, mode: "insensitive" } },
        { fullName: { contains: keyword, mode: "insensitive" } },
      ],
    },
    orderBy: { username: "asc" },
    select: PUBLIC_USER_FIELDS,
  });
}

/**
 * Resolve the friendship status between the authenticated user and another
 * user, from the authenticated user's perspective.
 *
 * Returns one of: FRIENDS, PENDING_SENT, PENDING_RECEIVED, NONE. Rejected
 * relationships are never exposed (reported as NONE).
 * @param {string} meId
 * @param {string} otherId
 * @returns {Promise<"FRIENDS"|"PENDING_SENT"|"PENDING_RECEIVED"|"NONE">}
 */
async function getFriendshipStatus(meId, otherId) {
  const friendship = await prisma.friendship.findFirst({
    where: {
      OR: [
        { requesterId: meId, addresseeId: otherId },
        { requesterId: otherId, addresseeId: meId },
      ],
    },
    select: { requesterId: true, status: true },
  });

  if (!friendship) return "NONE";

  if (friendship.status === "ACCEPTED") return "FRIENDS";

  if (friendship.status === "PENDING") {
    return friendship.requesterId === meId ? "PENDING_SENT" : "PENDING_RECEIVED";
  }

  // REJECTED (or anything else) is not exposed.
  return "NONE";
}

/**
 * Get a user's public profile plus the friendship status relative to the
 * authenticated user.
 *
 * Returns 404 if the user does not exist.
 * @param {string} meId - The authenticated user's id.
 * @param {string} targetId - The user being viewed.
 * @returns {Promise<object>} Public fields + friendshipStatus.
 */
async function getUserById(meId, targetId) {
  const user = await prisma.user.findUnique({
    where: { id: targetId },
    select: PUBLIC_USER_FIELDS,
  });

  if (!user) {
    throw new ServiceError("User not found.", 404);
  }

  // A user viewing their own profile has no friendship with themselves.
  const friendshipStatus =
    meId === targetId ? "NONE" : await getFriendshipStatus(meId, targetId);

  return { ...user, friendshipStatus };
}

/**
 * List a user's accepted friends (public fields only), ordered alphabetically
 * by username.
 *
 * Returns 404 if the user does not exist. Only ACCEPTED friendships are
 * considered; pending and rejected relationships are excluded.
 * @param {string} targetId
 * @returns {Promise<object[]>}
 */
async function getUserFriends(targetId) {
  const user = await prisma.user.findUnique({
    where: { id: targetId },
    select: { id: true },
  });

  if (!user) {
    throw new ServiceError("User not found.", 404);
  }

  const friendships = await prisma.friendship.findMany({
    where: {
      status: "ACCEPTED",
      OR: [{ requesterId: targetId }, { addresseeId: targetId }],
    },
    select: {
      requester: { select: PUBLIC_USER_FIELDS },
      addressee: { select: PUBLIC_USER_FIELDS },
    },
  });

  // Map each accepted friendship to the "other" user relative to targetId.
  const friends = friendships.map((f) =>
    f.requester.id === targetId ? f.addressee : f.requester
  );

  // Order alphabetically by username.
  friends.sort((a, b) => a.username.localeCompare(b.username));

  return friends;
}

module.exports = {
  ServiceError,
  searchUsers,
  getUserById,
  getUserFriends,
};
