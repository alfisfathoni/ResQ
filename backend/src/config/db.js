const prisma = require("./prisma");

// Verifies that the backend can reach the PostgreSQL (Neon) database.
//
// This runs a harmless `SELECT 1` query. It does NOT create, read, or
// modify any tables — it only confirms that DATABASE_URL points to a
// reachable database and that credentials are valid. Returns true on
// success, false on failure (the error is logged, not thrown, so the
// server can still start even if the database is not yet configured).
async function verifyDatabaseConnection() {
  try {
    await prisma.$queryRaw`SELECT 1`;
    console.log("Database connection OK (Neon PostgreSQL reachable).");
    return true;
  } catch (error) {
    console.warn(
      "Database connection check failed. " +
        "Make sure DATABASE_URL in .env points to your Neon database."
    );
    console.warn(`Reason: ${error.message}`);
    return false;
  }
}

module.exports = { verifyDatabaseConnection };
