// SOS routes.
//
// All endpoints require authentication via the existing authenticate
// middleware. Mounted at /api/sos in app.js. Literal paths (/active) are
// declared before parameterized ones (/:id/resolve).

const express = require("express");
const sosController = require("../controllers/sos.controller");
const { authenticate } = require("../middleware/auth.middleware");
const { sosLimiter } = require("../middleware/rate-limit.middleware");

const router = express.Router();

router.use(authenticate);

// The limiter runs after authenticate because it keys on req.user.id.
router.post("/", sosLimiter, sosController.trigger);
router.get("/active", sosController.getActive);
router.get("/", sosController.list);
router.patch("/:id/resolve", sosController.resolve);

module.exports = router;
