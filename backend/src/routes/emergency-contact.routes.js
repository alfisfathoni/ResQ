// Emergency contact routes.
//
// All endpoints require authentication via the existing authenticate
// middleware. Mounted at /api/emergency-contacts in app.js.

const express = require("express");
const emergencyContactController = require("../controllers/emergency-contact.controller");
const { authenticate } = require("../middleware/auth.middleware");

const router = express.Router();

router.use(authenticate);

router.post("/", emergencyContactController.create);
router.get("/", emergencyContactController.list);
router.put("/:id", emergencyContactController.update);
router.delete("/:id", emergencyContactController.remove);

module.exports = router;
