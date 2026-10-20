const { PrismaClient } = require("@prisma/client");

// A single, shared PrismaClient instance for the whole application.
//
// Creating one instance and reusing it avoids exhausting the database
// connection pool — which matters especially for serverless Postgres
// providers like Neon. The connection string is read from the
// DATABASE_URL environment variable (configured in schema.prisma via
// `url = env("DATABASE_URL")`), so no credentials are hardcoded here.
const prisma = new PrismaClient();

module.exports = prisma;
