const express = require("express");
const cors = require("cors");

const authRoutes = require("./routes/auth.routes");
const emergencyContactRoutes = require("./routes/emergency-contact.routes");
const reportRoutes = require("./routes/report.routes");
const reportMediaRoutes = require("./routes/report-media.routes");

// Create the Express application instance
const app = express();

// --- Global Middleware ---

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

module.exports = app;
