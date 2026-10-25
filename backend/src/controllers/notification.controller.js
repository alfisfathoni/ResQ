// Notification controller.
//
// Thin HTTP layer: validates input, delegates to the service, and shapes the
// response. No business logic here. All handlers run behind authenticate, so
// req.user is always populated. Handlers are wrapped with asyncHandler, so
// thrown ServiceErrors (and any unexpected error) propagate to the global
// error handler.

const { sendSuccess, sendError } = require("../utils/response");
const { asyncHandler } = require("../utils/async-handler");
const notificationService = require("../services/notification.service");
const {
  validateNotificationId,
} = require("../validations/notification.validation");

/**
 * GET /api/notifications
 */
const list = asyncHandler(async (req, res) => {
  const notifications = await notificationService.getNotifications(req.user.id);

  return sendSuccess(res, {
    statusCode: 200,
    message: "Notifications retrieved successfully.",
    data: notifications,
  });
});

/**
 * GET /api/notifications/unread-count
 */
const unreadCount = asyncHandler(async (req, res) => {
  const result = await notificationService.getUnreadCount(req.user.id);

  return sendSuccess(res, {
    statusCode: 200,
    message: "Unread notification count retrieved successfully.",
    data: result,
  });
});

/**
 * PATCH /api/notifications/:id/read
 */
const markRead = asyncHandler(async (req, res) => {
  const { valid, errors, data } = validateNotificationId(req.params);
  if (!valid) {
    return sendError(res, {
      statusCode: 422,
      message: "Validation failed.",
      errors,
    });
  }

  const notification = await notificationService.markAsRead(
    req.user.id,
    data.id
  );

  return sendSuccess(res, {
    statusCode: 200,
    message: "Notification marked as read.",
    data: notification,
  });
});

/**
 * PATCH /api/notifications/read-all
 */
const markAllRead = asyncHandler(async (req, res) => {
  const result = await notificationService.markAllAsRead(req.user.id);

  return sendSuccess(res, {
    statusCode: 200,
    message: "All notifications marked as read.",
    data: result,
  });
});

/**
 * DELETE /api/notifications/:id
 */
const remove = asyncHandler(async (req, res) => {
  const { valid, errors, data } = validateNotificationId(req.params);
  if (!valid) {
    return sendError(res, {
      statusCode: 422,
      message: "Validation failed.",
      errors,
    });
  }

  await notificationService.deleteNotification(req.user.id, data.id);

  return sendSuccess(res, {
    statusCode: 200,
    message: "Notification deleted successfully.",
  });
});

module.exports = {
  list,
  unreadCount,
  markRead,
  markAllRead,
  remove,
};
