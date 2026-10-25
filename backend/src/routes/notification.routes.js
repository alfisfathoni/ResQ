// Notification routes.
//
// All endpoints require authentication via the existing authenticate
// middleware. Mounted at /api/notifications in app.js.
//
// Literal paths (/unread-count, /read-all) are declared before the /:id
// routes so Express does not capture them as an :id value.

const express = require("express");
const notificationController = require("../controllers/notification.controller");
const { authenticate } = require("../middleware/auth.middleware");

const router = express.Router();

router.use(authenticate);

router.get("/", notificationController.list);
router.get("/unread-count", notificationController.unreadCount);
router.patch("/read-all", notificationController.markAllRead);
router.patch("/:id/read", notificationController.markRead);
router.delete("/:id", notificationController.remove);

module.exports = router;
