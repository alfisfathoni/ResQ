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

/**
 * Read a positive integer from the environment, falling back to a default
 * when the variable is unset, blank, or not a positive whole number.
 *
 * Used for tunable limits (e.g. rate limits) so a typo never disables a limit.
 * @param {string} name - Environment variable name.
 * @param {number} defaultValue - Value used when the variable is unusable.
 * @param {NodeJS.ProcessEnv} [env=process.env]
 * @returns {number}
 */
function readPositiveInt(name, defaultValue, env = process.env) {
  const raw = env[name];
  if (typeof raw !== "string" || raw.trim() === "") return defaultValue;

  const value = Number(raw.trim());
  if (!Number.isInteger(value) || value < 1) {
    // Name only; never print the raw value.
    console.warn(`${name} is not a positive integer; using the default (${defaultValue}).`);
    return defaultValue;
  }
  return value;
}

/**
 * Convert the TRUST_PROXY variable into a value accepted by
 * `app.set("trust proxy", ...)`.
 *
 * - unset or blank  -> undefined (the setting is left untouched)
 * - "true" / "false" -> boolean
 * - whole number     -> number of proxy hops to trust (e.g. 1)
 * - anything else    -> passed through as a string (Express also accepts
 *                       values such as "loopback" or a subnet list)
 *
 * Prefer a number. `true` trusts every hop, which lets a client spoof its IP
 * through X-Forwarded-For, and express-rate-limit warns about it.
 * @param {NodeJS.ProcessEnv} [env=process.env]
 * @returns {boolean|number|string|undefined}
 */
function parseTrustProxy(env = process.env) {
  const raw = env.TRUST_PROXY;
  if (typeof raw !== "string" || raw.trim() === "") return undefined;

  const value = raw.trim();
  const lower = value.toLowerCase();
  if (lower === "true") return true;
  if (lower === "false") return false;
  if (/^\d+$/.test(value)) return Number(value);
  return value;
}

module.exports = {
  REQUIRED_ENV,
  getMissingEnv,
  validateEnv,
  readPositiveInt,
  parseTrustProxy,
};
