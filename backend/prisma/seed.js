// Database seed.
//
// Run manually with `npm run seed`. Safe to run many times, including against a
// shared database, because it is IDEMPOTENT and ADDITIVE ONLY:
//   - It never deletes anything (no delete/deleteMany/truncate/reset anywhere).
//   - It never overwrites existing guides or demo users.
//   - An existing admin account is only promoted (role/isVerified); its
//     password is changed solely when SEED_RESET_ADMIN_PASSWORD=true.
//
// What it seeds:
//   1. ADMIN account (from ADMIN_* env vars).
//   2. Safety guides from prisma/seed/guides.json (created only if missing,
//      matched by title + category so admin edits are never overwritten).
//   3. Two demo users, only when SEED_DEMO_PASSWORD is set.
//
// Output is a summary only. Passwords and hashes are never printed.

require("dotenv").config();

const path = require("path");
const prisma = require("../src/config/prisma");
const { hashPassword } = require("../src/utils/hash");

// Same rules as src/validations/auth.validation.js.
const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const PHONE_REGEX = /^\+?\d{7,15}$/;
const USERNAME_REGEX = /^[a-zA-Z0-9_.]+$/;

// bcrypt only uses the first 72 bytes of a password.
const MAX_PASSWORD_BYTES = 72;
const RECOMMENDED_ADMIN_PASSWORD_LENGTH = 12;

const GUIDE_CATEGORIES = ["SAFETY", "FIRST_AID", "DISASTER", "EMERGENCY", "LEGAL"];

const DEFAULT_ADMIN = {
  username: "admin",
  fullName: "ResQ Admin",
  phoneNumber: "+628000000001",
};

const DEMO_USERS = [
  {
    email: "demo.user1@example.com",
    username: "demo_user1",
    fullName: "Demo User 1",
    phoneNumber: "+628111000001",
  },
  {
    email: "demo.user2@example.com",
    username: "demo_user2",
    fullName: "Demo User 2",
    phoneNumber: "+628111000002",
  },
];

/** Expected, user-fixable problem. Its message is safe to print. */
class SeedError extends Error {}

/**
 * Read an env var as a trimmed string ("" when unset).
 * @param {string} name
 * @returns {string}
 */
function readEnv(name) {
  const value = process.env[name];
  return typeof value === "string" ? value.trim() : "";
}

/**
 * Ensure a plaintext password can be hashed safely by bcrypt.
 * Error messages name the variable only, never the value.
 * @param {string} password
 * @param {string} envName
 * @param {number} minLength
 */
function assertPasswordUsable(password, envName, minLength) {
  if (password.length < minLength) {
    throw new SeedError(`${envName} must be at least ${minLength} characters.`);
  }
  if (Buffer.byteLength(password, "utf8") > MAX_PASSWORD_BYTES) {
    throw new SeedError(`${envName} must be at most ${MAX_PASSWORD_BYTES} bytes (bcrypt limit).`);
  }
}

/**
 * Read and validate every setting up front, before any database write.
 * @returns {{ admin: object, resetAdminPassword: boolean, demoPassword: string }}
 */
function loadConfig() {
  const missing = [];
  if (!readEnv("DATABASE_URL")) missing.push("DATABASE_URL");
  if (!readEnv("ADMIN_EMAIL")) missing.push("ADMIN_EMAIL");
  // Do not trim the password: leading/trailing spaces may be intentional.
  const adminPassword = process.env.ADMIN_PASSWORD || "";
  if (adminPassword.trim() === "") missing.push("ADMIN_PASSWORD");
  if (missing.length > 0) {
    throw new SeedError(
      `Required environment variables are missing or empty: ${missing.join(", ")}.`
    );
  }

  const email = readEnv("ADMIN_EMAIL").toLowerCase();
  const username = readEnv("ADMIN_USERNAME") || DEFAULT_ADMIN.username;
  const fullName = readEnv("ADMIN_FULL_NAME") || DEFAULT_ADMIN.fullName;
  const phoneNumber = readEnv("ADMIN_PHONE") || DEFAULT_ADMIN.phoneNumber;

  if (!EMAIL_REGEX.test(email) || email.length > 255) {
    throw new SeedError("ADMIN_EMAIL is not a valid email address.");
  }
  if (username.length < 3 || username.length > 100 || !USERNAME_REGEX.test(username)) {
    throw new SeedError(
      "ADMIN_USERNAME must be 3-100 characters: letters, numbers, underscores, dots."
    );
  }
  if (fullName.length > 255) {
    throw new SeedError("ADMIN_FULL_NAME must be at most 255 characters.");
  }
  if (!PHONE_REGEX.test(phoneNumber)) {
    throw new SeedError("ADMIN_PHONE must match ^\\+?\\d{7,15}$ (7-15 digits, optional leading +).");
  }

  // Length below 12 is only a warning; the byte limit is a hard technical limit.
  assertPasswordUsable(adminPassword, "ADMIN_PASSWORD", 1);
  if (adminPassword.length < RECOMMENDED_ADMIN_PASSWORD_LENGTH) {
    console.warn(
      `[seed] WARNING: ADMIN_PASSWORD is shorter than ${RECOMMENDED_ADMIN_PASSWORD_LENGTH} characters. ` +
        "Use a longer password for any shared or deployed environment."
    );
  }

  const demoPassword = process.env.SEED_DEMO_PASSWORD || "";
  if (demoPassword !== "") {
    assertPasswordUsable(demoPassword, "SEED_DEMO_PASSWORD", 8);
  }

  return {
    admin: { email, username, fullName, phoneNumber, password: adminPassword },
    resetAdminPassword: readEnv("SEED_RESET_ADMIN_PASSWORD").toLowerCase() === "true",
    demoPassword,
  };
}

/**
 * Describe which unique fields are already used by ANOTHER account.
 * @param {{ email?: string, username: string, phoneNumber: string }} candidate
 * @returns {Promise<string[]>} e.g. ["username", "phone number"]
 */
async function findUniqueConflicts({ email, username, phoneNumber }) {
  const rows = await prisma.user.findMany({
    where: { OR: [{ username }, { phoneNumber }, ...(email ? [{ email }] : [])] },
    select: { email: true, username: true, phoneNumber: true },
  });
  const conflicts = new Set();
  for (const row of rows) {
    if (row.username === username) conflicts.add("username");
    if (row.phoneNumber === phoneNumber) conflicts.add("phone number");
    if (email && row.email === email) conflicts.add("email");
  }
  return [...conflicts];
}

/**
 * Create the ADMIN account, or promote the existing account with that email.
 * @param {object} config - config.admin and config.resetAdminPassword.
 * @returns {Promise<string>} Outcome label for the summary.
 */
async function seedAdmin({ admin, resetAdminPassword }) {
  const existing = await prisma.user.findUnique({
    where: { email: admin.email },
    select: { id: true, role: true, isVerified: true },
  });

  if (existing) {
    const data = {};
    if (existing.role !== "ADMIN") data.role = "ADMIN";
    if (!existing.isVerified) data.isVerified = true;
    if (resetAdminPassword) data.password = await hashPassword(admin.password);

    if (Object.keys(data).length === 0) {
      return "already an admin and verified, nothing to change";
    }
    await prisma.user.update({ where: { id: existing.id }, data });

    const changes = [];
    if (data.role) changes.push("promoted to ADMIN");
    if (data.isVerified) changes.push("marked as verified");
    if (data.password) changes.push("password reset (SEED_RESET_ADMIN_PASSWORD=true)");
    return `existing account updated: ${changes.join(", ")}`;
  }

  const conflicts = await findUniqueConflicts(admin);
  if (conflicts.length > 0) {
    const vars = conflicts.map((c) => (c === "username" ? "ADMIN_USERNAME" : "ADMIN_PHONE"));
    throw new SeedError(
      `Cannot create the admin: ${conflicts.join(" and ")} already used by another account. ` +
        `Change ${vars.join(" / ")} and run the seed again.`
    );
  }

  await prisma.user.create({
    data: {
      fullName: admin.fullName,
      username: admin.username,
      email: admin.email,
      phoneNumber: admin.phoneNumber,
      password: await hashPassword(admin.password),
      role: "ADMIN",
      isVerified: true,
    },
  });
  return "created";
}

/**
 * Load and sanity-check the guide content file.
 * `content` may be an array of lines (joined with newlines) or a string.
 * @returns {{ title: string, category: string, content: string }[]}
 */
function loadGuides() {
  const raw = require(path.join(__dirname, "seed", "guides.json"));
  if (!Array.isArray(raw)) throw new SeedError("prisma/seed/guides.json must be an array.");

  const seen = new Set();
  return raw.map((item, index) => {
    const label = `prisma/seed/guides.json item #${index + 1}`;
    const title = typeof item.title === "string" ? item.title.trim() : "";
    const content = Array.isArray(item.content) ? item.content.join("\n") : item.content;

    if (!title || title.length > 255) throw new SeedError(`${label}: invalid title.`);
    if (!GUIDE_CATEGORIES.includes(item.category)) {
      throw new SeedError(`${label}: category must be one of ${GUIDE_CATEGORIES.join(", ")}.`);
    }
    if (typeof content !== "string" || content.trim() === "") {
      throw new SeedError(`${label}: content is empty.`);
    }
    const key = `${item.category}::${title}`;
    if (seen.has(key)) throw new SeedError(`${label}: duplicate title + category.`);
    seen.add(key);

    return { title, category: item.category, content: content.trim() };
  });
}

/**
 * Create guides that do not exist yet (matched by title + category).
 * Existing guides are never modified, so admin edits are preserved.
 * @returns {Promise<{ created: number, skipped: number }>}
 */
async function seedGuides() {
  let created = 0;
  let skipped = 0;
  for (const guide of loadGuides()) {
    const exists = await prisma.guide.findFirst({
      where: { title: guide.title, category: guide.category },
      select: { id: true },
    });
    if (exists) {
      skipped += 1;
      continue;
    }
    await prisma.guide.create({ data: guide });
    created += 1;
  }
  return { created, skipped };
}

/**
 * Create the two demo users when SEED_DEMO_PASSWORD is set. Existing users
 * (by email) are skipped untouched.
 * @param {string} demoPassword
 * @returns {Promise<{ created: number, skipped: number }|null>} null when disabled.
 */
async function seedDemoUsers(demoPassword) {
  if (demoPassword === "") return null;

  const passwordHash = await hashPassword(demoPassword);
  let created = 0;
  let skipped = 0;
  for (const demo of DEMO_USERS) {
    const existing = await prisma.user.findUnique({
      where: { email: demo.email },
      select: { id: true },
    });
    if (existing) {
      skipped += 1;
      continue;
    }

    const conflicts = await findUniqueConflicts({
      username: demo.username,
      phoneNumber: demo.phoneNumber,
    });
    if (conflicts.length > 0) {
      throw new SeedError(
        `Cannot create demo user ${demo.email}: ${conflicts.join(" and ")} already used by another account.`
      );
    }

    await prisma.user.create({
      data: { ...demo, password: passwordHash, role: "USER", isVerified: true },
    });
    created += 1;
  }
  return { created, skipped };
}

async function main() {
  const config = loadConfig();

  const adminResult = await seedAdmin(config);
  console.log(`[seed] Admin account: ${adminResult}.`);

  const guides = await seedGuides();
  console.log(`[seed] Guides: ${guides.created} created, ${guides.skipped} already existed (skipped).`);

  const demo = await seedDemoUsers(config.demoPassword);
  if (demo === null) {
    console.log("[seed] Demo users: skipped (SEED_DEMO_PASSWORD not set).");
  } else {
    console.log(`[seed] Demo users: ${demo.created} created, ${demo.skipped} already existed (skipped).`);
  }

  console.log("[seed] Done.");
}

main()
  .catch((error) => {
    if (error instanceof SeedError) {
      console.error(`[seed] ERROR: ${error.message}`);
    } else {
      // Do not print error.message: Prisma errors can echo query arguments
      // (which may include a password hash). Name and code are enough to debug.
      console.error(
        `[seed] ERROR: unexpected failure (${error && error.name}${
          error && error.code ? `, code ${error.code}` : ""
        }). Check DATABASE_URL and that migrations are applied.`
      );
    }
    process.exitCode = 1;
  })
  .finally(async () => {
    await prisma.$disconnect();
  });
