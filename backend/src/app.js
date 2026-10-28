const express = require("express");
const cors = require("cors");
const helmet = require("helmet");

const authRoutes = require("./routes/auth.routes");
const emergencyContactRoutes = require("./routes/emergency-contact.routes");
const reportRoutes = require("./routes/report.routes");
const reportMediaRoutes = require("./routes/report-media.routes");
const guideRoutes = require("./routes/guide.routes");
const friendshipRoutes = require("./routes/friendship.routes");
const notificationRoutes = require("./routes/notification.routes");
const sharingSessionRoutes = require("./routes/sharing-session.routes");
const userRoutes = require("./routes/user.routes");
const pageRoutes = require("./routes/page.routes");
const { errorHandler } = require("./middleware/error.middleware");
const { createRequestLogger } = require("./middleware/request-logger.middleware");
const { parseTrustProxy } = require("./config/env");

// Create the Express application instance
const app = express();

// Behind a hosting proxy every request appears to come from the proxy's IP,
// which breaks per-IP rate limiting. TRUST_PROXY (a hop count such as 1, or
// true/false) makes Express read the client IP from X-Forwarded-For.
// When the variable is unset nothing changes (Express default: not trusted).
const trustProxy = parseTrustProxy();
if (trustProxy !== undefined) {
  app.set("trust proxy", trustProxy);
}

// --- Global Middleware ---

// Secure HTTP headers
app.use(helmet());

// HTTP request logging. Uses redacting tokens so ?token=... values from
// email links never reach the logs.
app.use(createRequestLogger());

// Enable Cross-Origin Resource Sharing so the mobile client can call the API
app.use(cors());

// Parse incoming JSON request bodies (Content-Type: application/json)
app.use(express.json());

// Parse URL-encoded request bodies (Content-Type: application/x-www-form-urlencoded)
app.use(express.urlencoded({ extended: true }));

// --- Routes ---

// Health check / root endpoint
app.get("/", (req, res) => {
  res.status(200).json({
    success: true,
    message: "ResQ Backend API is running",
  });
});

// Feature routes
app.use("/api/auth", authRoutes);
app.use("/api/emergency-contacts", emergencyContactRoutes);
app.use("/api/reports", reportRoutes);
app.use("/api/reports/:reportId/media", reportMediaRoutes);
app.use("/api/guides", guideRoutes);
app.use("/api/friendships", friendshipRoutes);
app.use("/api/notifications", notificationRoutes);
app.use("/api/sharing-sessions", sharingSessionRoutes);
app.use("/api/users", userRoutes);

// Browser pages opened from email links (e.g. GET /reset-password)
app.use("/", pageRoutes);

// --- Global error handler (must be registered last, after all routes) ---
app.use(errorHandler);

module.exports = app;
