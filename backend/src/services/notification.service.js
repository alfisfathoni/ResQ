// Notification service.
//
// Business logic for the Notification module. Mirrors the other services'
// pattern, including a local ServiceError carrying an HTTP status and optional
// field errors. Controllers call into this layer; this layer never touches
// req/res.
//
// Reading and managing notifications, plus best-effort creation helpers that
// other business services (friendship, sharing session) call when a business
// event occurs. Creation is deliberately best-effort: a notification failure
// is logged and swallowed so it can never break the primary operation that
// triggered it.

const prisma = require("../config/prisma");
const { ServiceError } = require("../utils/service-error");

// Minimal, non-sensitive sender shape embedded in notification responses.
const SENDER_PREVIEW = {
  id: true,
  fullName: true,
  username: true,
  profilePicture: true,
};

const NOTIFICATION_FIELDS = {
  id: true,
  userId: true,
  senderId: true,
  type: true,
  title: true,
  message: true,
  isRead: true,
  createdAt: true,
  sender: { select: SENDER_PREVIEW },
};

/**
 * Create a single notification for a business event. Best-effort.
 *
 * Intended to be called from other services (e.g. friendship) after their
 * primary operation has succeeded. Any failure is logged and swallowed so the
 * caller's operation is never broken by a notification problem. Returns the
 * created notification on success, or null on failure.
 *
 * @param {object} params
 * @param {string} params.userId - The receiver's id.
 * @param {string|null} [params.senderId] - The sender's id (optional).
 * @param {string} params.type - A NotificationType value.
 * @param {string} params.title
 * @param {string} params.message
 * @returns {Promise<object|null>}
 */
async function createNotification({ userId, senderId = null, type, title, message }) {
  try {
    return await prisma.notification.create({
      data: { userId, senderId, type, title, message, isRead: false },
    });
  } catch (error) {
    console.error("Failed to create notification:", error.message);
    return null;
  }
}

/**
 * Create many notifications in one call for a fan-out business event (e.g. a
 * sharing session notifying every accepted friend). Best-effort: a failure is
 * logged and swallowed so the caller's operation is never broken. `isRead`
 * defaults to false for each row. No-ops on an empty list.
 *
 * @param {Array<{ userId: string, senderId?: string|null, type: string, title: string, message: string }>} items
 * @returns {Promise<{ created: number }>} How many rows were created.
 */
async function createNotifications(items = []) {
  if (!Array.isArray(items) || items.length === 0) {
    return { created: 0 };
  }
  try {
    const result = await prisma.notification.createMany({
      data: items.map((n) => ({
        userId: n.userId,
        senderId: n.senderId ?? null,
        type: n.type,
        title: n.title,
        message: n.message,
        isRead: false,
      })),
    });
    return { created: result.count };
  } catch (error) {
    console.error("Failed to create notifications:", error.message);
    return { created: 0 };
  }
}

/**
 * List the authenticated user's notifications, newest first.
 * @param {string} userId
 * @param {object} [pagination={}] - Normalized pagination options { skip, take }.
 * @returns {Promise<{ notifications: object[], total: number }>}
 */
async function getNotifications(userId, pagination = {}) {
  const { skip, take } = pagination;
  const where = { userId };

  const [notifications, total] = await Promise.all([
    prisma.notification.findMany({
      where,
      orderBy: { createdAt: "desc" },
      select: NOTIFICATION_FIELDS,
      ...(typeof skip === "number" ? { skip } : {}),
      ...(typeof take === "number" ? { take } : {}),
    }),
    prisma.notification.count({ where }),
  ]);

  return { notifications, total };
}

/**
 * Count the authenticated user's unread notifications.
 * @param {string} userId
 * @returns {Promise<{ count: number }>}
 */
async function getUnreadCount(userId) {
  const count = await prisma.notification.count({
    where: { userId, isRead: false },
  });
  return { count };
}

/**
 * Mark one of the authenticated user's notifications as read.
 *
 * Returns 404 if the notification does not exist OR belongs to another user.
 * Idempotent: marking an already-read notification returns it unchanged rather
 * than erroring.
 * @param {string} userId
 * @param {string} notificationId
 * @returns {Promise<object>} The notification (isRead = true).
 */
async function markAsRead(userId, notificationId) {
  const existing = await prisma.notification.findUnique({
    where: { id: notificationId },
    select: { id: true, userId: true, isRead: true },
  });

  if (!existing || existing.userId !== userId) {
    throw new ServiceError("Notification not found.", 404);
  }

  // Already read → idempotent: return the current row without a write.
  if (existing.isRead) {
    return prisma.notification.findUnique({
      where: { id: notificationId },
      select: NOTIFICATION_FIELDS,
    });
  }

  return prisma.notification.update({
    where: { id: notificationId },
    data: { isRead: true },
    select: NOTIFICATION_FIELDS,
  });
}

/**
 * Mark all of the authenticated user's unread notifications as read.
 *
 * Only affects rows owned by the user that are currently unread.
 * @param {string} userId
 * @returns {Promise<{ updated: number }>} How many notifications were updated.
 */
async function markAllAsRead(userId) {
  const result = await prisma.notification.updateMany({
    where: { userId, isRead: false },
    data: { isRead: true },
  });
  return { updated: result.count };
}

/**
 * Delete one of the authenticated user's notifications.
 *
 * Returns 404 if the notification does not exist OR belongs to another user.
 * @param {string} userId
 * @param {string} notificationId
 * @returns {Promise<void>}
 */
async function deleteNotification(userId, notificationId) {
  const existing = await prisma.notification.findUnique({
    where: { id: notificationId },
    select: { id: true, userId: true },
  });

  if (!existing || existing.userId !== userId) {
    throw new ServiceError("Notification not found.", 404);
  }

  await prisma.notification.delete({ where: { id: notificationId } });
}

module.exports = {
  ServiceError,
  createNotification,
  createNotifications,
  getNotifications,
  getUnreadCount,
  markAsRead,
  markAllAsRead,
  deleteNotification,
};
