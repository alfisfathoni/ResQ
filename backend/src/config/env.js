// Environment validation.
//
// The server refuses to start when a required variable is missing or blank.
// This applies to every environment (development, test, production): there
// are no insecure fallbacks for secrets anywhere in the codebase.
//
// Error output only ever names the variables. It never prints their values.

// Variables the application cannot run without.
const REQUIRED_ENV = [
  "DATABASE_URL",
  "JWT_SECRET",
  "JWT_REFRESH_SECRET",
  "JWT_EMAIL_VERIFICATION_SECRET",
  "JWT_PASSWORD_RESET_SECRET",
];

/**
 * List the required variables that are missing or blank.
 * @param {NodeJS.ProcessEnv} [env=process.env]
 * @returns {string[]} Names of the missing variables.
 */
function getMissingEnv(env = process.env) {
  return REQUIRED_ENV.filter((name) => {
    const value = env[name];
    return typeof value !== "string" || value.trim() === "";
  });
}

/**
 * Validate required environment variables; exit the process when any is missing.
 *
 * Call this right after `dotenv.config()` and before loading modules that read
 * secrets (for example `utils/jwt.js`). Only variable names are printed.
 * @param {NodeJS.ProcessEnv} [env=process.env]
 * @returns {void}
 */
function validateEnv(env = process.env) {
  const missing = getMissingEnv(env);
  if (missing.length === 0) return;

  console.error("Server cannot start: required environment variables are missing or empty:");
  missing.forEach((name) => console.error(`  - ${name}`));
  console.error("Set them in your .env file (see .env.example) or in the hosting environment.");
  process.exit(1);
}

module.exports = { REQUIRED_ENV, getMissingEnv, validateEnv };
