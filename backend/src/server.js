// Load environment variables from the .env file into process.env
require("dotenv").config();

// Fail fast when required variables (DATABASE_URL, JWT_*_SECRET) are missing.
// This must run BEFORE ./app is loaded, because modules loaded by the app
// (e.g. utils/jwt.js) read secrets from process.env at load time.
require("./config/env").validateEnv();

const app = require("./app");
const prisma = require("./config/prisma");
const { verifyDatabaseConnection } = require("./config/db");

// Read the port from the environment, defaulting to 3000 when not provided
const PORT = process.env.PORT || 3000;

// Start the HTTP server
const server = app.listen(PORT, async () => {
  console.log(`ResQ Backend API is running on port ${PORT}`);

  // Confirm the backend can reach Neon once DATABASE_URL is provided.
  // This does not block startup and does not create any tables.
  await verifyDatabaseConnection();
});

// Gracefully close the database connection when the process is terminated
const shutdown = async () => {
  await prisma.$disconnect();
  server.close(() => process.exit(0));
};

process.on("SIGINT", shutdown);
process.on("SIGTERM", shutdown);
