// Browser page routes.
//
// HTML pages opened from links in ResQ emails. Mounted at the application
// root (not under /api) so the links stay short and readable.

const express = require("express");
const pageController = require("../controllers/page.controller");

const router = express.Router();

router.get("/reset-password", pageController.resetPasswordPage);

module.exports = router;
