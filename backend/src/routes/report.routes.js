// Incident report routes.
//
// All endpoints require authentication via the existing authenticate
// middleware. Reads (feed, detail) are open to every authenticated user;
// PUT/DELETE are owner-only. Mounted at /api/reports in app.js.

const express = require("express");
const reportController = require("../controllers/report.controller");
const { authenticate } = require("../middleware/auth.middleware");

const router = express.Router();

router.use(authenticate);

router.post("/", reportController.create);
router.get("/", reportController.list);
// Must stay before "/:id" so "mine" is not captured as an id.
router.get("/mine", reportController.listMine);
router.get("/:id", reportController.detail);
router.put("/:id", reportController.update);
router.delete("/:id", reportController.remove);

module.exports = router;
