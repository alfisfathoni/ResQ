// Password hashing utility built on bcrypt.
//
// Centralizes bcrypt usage so the rest of the codebase never touches the
// library directly. The cost factor (salt rounds) is configurable via the
// BCRYPT_SALT_ROUNDS environment variable, defaulting to 10.

const bcrypt = require("bcrypt");

const SALT_ROUNDS = parseInt(process.env.BCRYPT_SALT_ROUNDS || "10", 10);

/**
 * Hash a plaintext password.
 * @param {string} plainPassword
 * @returns {Promise<string>} The bcrypt hash.
 */
async function hashPassword(plainPassword) {
  return bcrypt.hash(plainPassword, SALT_ROUNDS);
}

/**
 * Compare a plaintext password against a stored hash.
 * @param {string} plainPassword
 * @param {string} hashedPassword
 * @returns {Promise<boolean>} True when the password matches.
 */
async function comparePassword(plainPassword, hashedPassword) {
  return bcrypt.compare(plainPassword, hashedPassword);
}

module.exports = {
  hashPassword,
  comparePassword,
};
