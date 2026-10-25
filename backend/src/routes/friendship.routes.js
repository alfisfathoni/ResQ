// Friendship routes.
//
// All endpoints require authentication via the existing authenticate
// middleware. Mounted at /api/friendships in app.js.

const express = require("express");
const friendshipController = require("../controllers/friendship.controller");
const { authenticate } = require("../middleware/auth.middleware");

const router = express.Router();

router.use(authenticate);

router.post("/request", friendshipController.sendRequest);
router.get("/", friendshipController.listFriends);
router.get("/requests", friendshipController.listIncomingRequests);
router.patch("/:id/accept", friendshipController.accept);
router.patch("/:id/reject", friendshipController.reject);
router.delete("/:id", friendshipController.remove);

module.exports = router;
