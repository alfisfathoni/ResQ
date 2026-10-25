// Sharing session routes.
//
// All endpoints require authentication via the existing authenticate
// middleware. Mounted at /api/sharing-sessions in app.js.
//
// The literal /active path is declared before /:id so Express does not
// capture it as an :id value.

const express = require("express");
const sharingSessionController = require("../controllers/sharing-session.controller");
const { authenticate } = require("../middleware/auth.middleware");

const router = express.Router();

router.use(authenticate);

router.post("/", sharingSessionController.create);
router.get("/active", sharingSessionController.listActive);
router.get("/:id", sharingSessionController.detail);
router.patch("/:id/stop", sharingSessionController.stop);

module.exports = router;
