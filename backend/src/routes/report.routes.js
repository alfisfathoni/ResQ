// Incident report routes.
//
// All endpoints require authentication via the existing authenticate
// middleware. Mounted at /api/reports in app.js.

const express = require("express");
const reportController = require("../controllers/report.controller");
const { authenticate } = require("../middleware/auth.middleware");

const router = express.Router();

router.use(authenticate);

router.post("/", reportController.create);
router.get("/", reportController.list);
router.get("/:id", reportController.detail);
router.put("/:id", reportController.update);
router.delete("/:id", reportController.remove);

module.exports = router;
