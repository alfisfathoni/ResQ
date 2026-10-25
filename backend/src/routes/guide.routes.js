// Guide routes.
//
// Public reads (list, detail) and admin-only writes (create, update, delete).
// Admin routes compose the existing authenticate middleware with the isAdmin
// role guard. Mounted at /api/guides in app.js.

const express = require("express");
const guideController = require("../controllers/guide.controller");
const { authenticate } = require("../middleware/auth.middleware");
const { isAdmin } = require("../middleware/role.middleware");

const router = express.Router();

// Public endpoints
router.get("/", guideController.list);
router.get("/:id", guideController.detail);

// Admin-only endpoints
router.post("/", authenticate, isAdmin, guideController.create);
router.put("/:id", authenticate, isAdmin, guideController.update);
router.delete("/:id", authenticate, isAdmin, guideController.remove);

module.exports = router;
