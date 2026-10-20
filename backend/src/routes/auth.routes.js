// Authentication routes.
//
// Maps /api/auth/* endpoints to their controller handlers. Validation and
// auth middleware will be attached here as the endpoints are implemented.

const express = require("express");
const authController = require("../controllers/auth.controller");
const { authenticate } = require("../middleware/auth.middleware");

const router = express.Router();

// Public endpoints
router.post("/register", authController.register);
router.post("/login", authController.login);
router.post("/refresh", authController.refreshToken);
router.post("/forgot-password", authController.forgotPassword);
router.post("/reset-password", authController.resetPassword);
router.get("/verify-email", authController.verifyEmail);

// Protected endpoints
router.get("/profile", authenticate, authController.getProfile);
router.put("/profile", authenticate, authController.updateProfile);
router.delete("/profile", authenticate, authController.deleteAccount);
router.post("/logout", authenticate, authController.logout);

module.exports = router;
