// Facility routes.
//
// Read-only endpoints for authenticated users. Mounted at /api/facilities in
// app.js. Admin create/update/delete come in roadmap task 2.3.

const express = require("express");
const facilityController = require("../controllers/facility.controller");
const { authenticate } = require("../middleware/auth.middleware");

const router = express.Router();

router.use(authenticate);

router.get("/", facilityController.listNearby);
router.get("/:id", facilityController.detail);

module.exports = router;
