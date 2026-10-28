// Authentication routes.
//
// Maps /api/auth/* endpoints to their controller handlers. Sensitive public
// authentication endpoints are protected by rate limiting.

const express = require("express");
const authController = require("../controllers/auth.controller");
const { authenticate } = require("../middleware/auth.middleware");
const {
  authLimiter,
  emailLimiter,
  refreshLimiter,
} = require("../middleware/rate-limit.middleware");

const router = express.Router();

// Public endpoints (rate-limited against brute-force attacks)
router.post("/register", authLimiter, authController.register);
router.post("/login", authLimiter, authController.login);
router.post("/refresh", refreshLimiter, authController.refreshToken);
router.post("/forgot-password", authLimiter, authController.forgotPassword);
router.post("/reset-password", authLimiter, authController.resetPassword);
router.post("/resend-verification", emailLimiter, authController.resendVerification);
router.get("/verify-email", authController.verifyEmail);

// Protected endpoints
router.get("/profile", authenticate, authController.getProfile);
router.put("/profile", authenticate, authController.updateProfile);
router.delete("/profile", authenticate, authController.deleteAccount);
router.post("/logout", authenticate, authController.logout);

module.exports = router;
