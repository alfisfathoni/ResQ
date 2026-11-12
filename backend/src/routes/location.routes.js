// Location routes.
//
// Requires authentication. Mounted at /api/location in app.js.

const express = require("express");
const locationController = require("../controllers/location.controller");
const { authenticate } = require("../middleware/auth.middleware");

const router = express.Router();

router.use(authenticate);

router.put("/", locationController.update);

module.exports = router;
