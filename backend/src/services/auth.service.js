// Authentication service.
//
// Holds the business logic for authentication (talking to the database via
// Prisma, hashing passwords, issuing tokens, sending verification emails).
// Controllers call into this layer; this layer never touches req/res.

const jwt = require("jsonwebtoken");
const prisma = require("../config/prisma");
const { ServiceError } = require("../utils/service-error");
const { hashPassword, comparePassword } = require("../utils/hash");
const {
  signAccessToken,
  signRefreshToken,
  verifyRefreshToken,
  signEmailVerificationToken,
  verifyEmailVerificationToken,
  signPasswordResetToken,
  verifyPasswordResetToken,
} = require("../utils/jwt");
const {
  sendVerificationEmail,
  sendPasswordResetEmail,
} = require("../utils/mailer");

// Fields of a user that may be returned to the client (includes `role`, which
// the mobile app uses after login to choose the user or admin home screen).
// Single source of truth shared by toPublicUser(), getProfile(), and
// updateProfile(). The password hash is deliberately absent.
const PUBLIC_USER_SELECT = {
  id: true,
  fullName: true,
  username: true,
  email: true,
  phoneNumber: true,
  profilePicture: true,
  role: true,
  isVerified: true,
  lastLatitude: true,
  lastLongitude: true,
  createdAt: true,
  updatedAt: true,
};

/**
 * Reduce a user record to the public whitelist (PUBLIC_USER_SELECT).
 *
 * Whitelist rather than "remove password", so a field added to the User model
 * later is never exposed by accident.
 * @param {object} user
 * @returns {object} user with only the allowed fields (including `role`).
 */
function toPublicUser(user) {
  if (!user) return user;
  const safe = {};
  for (const key of Object.keys(PUBLIC_USER_SELECT)) {
    if (key in user) safe[key] = user[key];
  }
  return safe;
}

/**
 * Register a new user.
 *
 * Steps: reject duplicate username/email/phone, hash the password, persist the
 * user with the default role USER and an inactive (unverified) status, generate
 * a stateless email-verification token, and send the verification email.
 *
 * Note: the User model has no verification-token column, so the token is a
 * self-contained JWT carrying the new user's id (see utils/jwt.js). The
 * account is "inactive" by way of isVerified = false.
 *
 * @param {object} payload - Sanitized { fullName, username, email, phoneNumber, password }.
 * @returns {Promise<object>} The created user, without the password hash.
 */
async function register(payload) {
  const { fullName, username, email, phoneNumber, password } = payload;

  // Reject duplicates. Check each field so we can report exactly which one
  // collided rather than a generic "already exists".
  const existing = await prisma.user.findFirst({
    where: {
      OR: [{ username }, { email }, { phoneNumber }],
    },
    select: { username: true, email: true, phoneNumber: true },
  });

  if (existing) {
    const errors = {};
    if (existing.username === username) errors.username = "Username is already taken.";
    if (existing.email === email) errors.email = "Email is already registered.";
    if (existing.phoneNumber === phoneNumber) {
      errors.phoneNumber = "Phone number is already registered.";
    }
    throw new ServiceError("An account with these details already exists.", 409, errors);
  }

  // Hash the password before persisting.
  const passwordHash = await hashPassword(password);

  // Persist the user: default role USER, inactive until email is verified.
  const user = await prisma.user.create({
    data: {
      fullName,
      username,
      email,
      phoneNumber,
      password: passwordHash,
      role: "USER",
      isVerified: false,
    },
    select: {
      id: true,
      fullName: true,
      username: true,
      email: true,
      phoneNumber: true,
      role: true,
      isVerified: true,
      createdAt: true,
    },
  });

  // Generate the stateless email-verification token and send the email.
  const verificationToken = signEmailVerificationToken({ sub: user.id });

  // If email delivery fails, surface it but keep the created account; the user
  // can request a resend later. We log and rethrow as a non-fatal warning path.
  try {
    await sendVerificationEmail({
      to: user.email,
      fullName: user.fullName,
      token: verificationToken,
    });
  } catch (mailError) {
    // Don't fail registration if SMTP is unavailable; log for diagnostics.
    console.error("Failed to send verification email:", mailError.message);
  }

  return user;
}

/**
 * Authenticate a user by email-or-username + password and issue tokens.
 *
 * Steps: find the user by email OR username, reject if missing, compare the
 * password with bcrypt, reject on mismatch, reject if the account is not yet
 * verified, sign an access and a refresh token, persist the refresh token in
 * the RefreshToken table, and return both tokens plus the sanitized user.
 *
 * To avoid leaking which accounts exist, a missing user and a wrong password
 * both return the same generic "Invalid credentials" error.
 *
 * @param {object} credentials - { identifier, password }.
 * @returns {Promise<{ accessToken: string, refreshToken: string, user: object }>}
 */
async function login({ identifier, password }) {
  // Find by email or username in a single query.
  const user = await prisma.user.findFirst({
    where: {
      OR: [{ email: identifier.toLowerCase() }, { username: identifier }],
    },
  });

  // Same error for "no such user" and "wrong password" to prevent enumeration.
  if (!user) {
    throw new ServiceError("Invalid credentials.", 401);
  }

  const passwordMatches = await comparePassword(password, user.password);
  if (!passwordMatches) {
    throw new ServiceError("Invalid credentials.", 401);
  }

  // Block unverified accounts from logging in.
  if (!user.isVerified) {
    throw new ServiceError(
      "Please verify your email before logging in.",
      403
    );
  }

  // Issue tokens. The access token carries the role for authorization.
  const accessToken = signAccessToken({ sub: user.id, role: user.role });
  const refreshToken = signRefreshToken({ sub: user.id });

  // Derive the stored expiry from the refresh token's own exp claim so the
  // database row and the JWT always agree.
  const decoded = jwt.decode(refreshToken);
  const expiresAt =
    decoded && decoded.exp
      ? new Date(decoded.exp * 1000)
      : new Date(Date.now() + 7 * 24 * 60 * 60 * 1000); // fallback: 7 days

  await prisma.refreshToken.create({
    data: {
      userId: user.id,
      token: refreshToken,
      expiresAt,
    },
  });

  return {
    accessToken,
    refreshToken,
    user: toPublicUser(user),
  };
}

/**
 * Issue a new access token from a valid, non-revoked refresh token.
 *
 * Steps: verify the refresh JWT (signature + expiry), confirm the exact token
 * string still exists in the RefreshToken table (so logged-out / revoked
 * tokens are rejected even when still cryptographically valid), load the user,
 * and sign a fresh access token. No new refresh token is issued.
 *
 * @param {string} token - The refresh token string.
 * @returns {Promise<{ accessToken: string }>}
 */
async function refreshToken(token) {
  // Verify signature and expiry. Map JWT failures to 401.
  let payload;
  try {
    payload = verifyRefreshToken(token);
  } catch (error) {
    if (error instanceof jwt.TokenExpiredError) {
      throw new ServiceError("Refresh token has expired.", 401);
    }
    if (error instanceof jwt.JsonWebTokenError) {
      throw new ServiceError("Invalid refresh token.", 401);
    }
    throw error;
  }

  // The token must still be present in the store (not revoked / logged out).
  const stored = await prisma.refreshToken.findFirst({
    where: { token, userId: payload.sub },
    select: { id: true },
  });

  if (!stored) {
    throw new ServiceError("Refresh token is no longer valid.", 401);
  }

  // Load the user to embed current role in the new access token.
  const user = await prisma.user.findUnique({
    where: { id: payload.sub },
    select: { id: true, role: true },
  });

  if (!user) {
    throw new ServiceError("User not found.", 401);
  }

  // Issue only a new access token; the refresh token is unchanged.
  const accessToken = signAccessToken({ sub: user.id, role: user.role });

  return { accessToken };
}

/**
 * Begin the password-reset flow for an email address.
 *
 * If a user with the email exists, sign a stateless password-reset token and
 * send the reset email. If no user exists, do nothing. This function never
 * throws or signals which case occurred, so the controller can always return
 * an identical response and avoid account enumeration.
 *
 * Note: the User model has no reset-token column, so the token is a
 * self-contained JWT (see utils/jwt.js).
 *
 * @param {object} payload - { email } (already lowercased/trimmed).
 * @returns {Promise<void>}
 */
async function forgotPassword({ email }) {
  const user = await prisma.user.findUnique({
    where: { email },
    select: { id: true, fullName: true, email: true },
  });

  // Silently stop when the account does not exist (no enumeration).
  if (!user) {
    return;
  }

  const resetToken = signPasswordResetToken({ sub: user.id });

  try {
    await sendPasswordResetEmail({
      to: user.email,
      fullName: user.fullName,
      token: resetToken,
    });
  } catch (mailError) {
    // Never surface mail failures to the caller; log for diagnostics only.
    console.error("Failed to send password reset email:", mailError.message);
  }
}

/**
 * Complete a password reset using a stateless reset token.
 *
 * Steps: verify the reset JWT (signature + expiry + expected type), load the
 * user, hash the new password, and — in a single transaction — update the
 * password and delete all of the user's refresh tokens so every existing
 * session is invalidated.
 *
 * @param {object} payload - { resetToken, newPassword }.
 * @returns {Promise<void>}
 */
async function resetPassword({ resetToken, newPassword }) {
  // Verify signature and expiry. Map JWT failures to 400.
  let decoded;
  try {
    decoded = verifyPasswordResetToken(resetToken);
  } catch (error) {
    if (error instanceof jwt.TokenExpiredError) {
      throw new ServiceError("Reset link has expired.", 400);
    }
    if (error instanceof jwt.JsonWebTokenError) {
      throw new ServiceError("Invalid reset token.", 400);
    }
    throw error;
  }

  // Guard against tokens of a different purpose being replayed here.
  if (decoded.type !== "password_reset" || !decoded.sub) {
    throw new ServiceError("Invalid reset token.", 400);
  }

  const user = await prisma.user.findUnique({
    where: { id: decoded.sub },
    select: { id: true },
  });

  if (!user) {
    throw new ServiceError("User not found.", 404);
  }

  // Hash the new password with the existing utility.
  const passwordHash = await hashPassword(newPassword);

  // Update the password and revoke all sessions atomically.
  await prisma.$transaction([
    prisma.user.update({
      where: { id: user.id },
      data: { password: passwordHash },
    }),
    prisma.refreshToken.deleteMany({
      where: { userId: user.id },
    }),
  ]);
}

/**
 * Fetch the current profile for an authenticated user.
 *
 * Always reads the latest data from the database (not the token claims) so
 * the response reflects any changes since the access token was issued.
 *
 * @param {string} userId - The authenticated user's id (from req.user).
 * @returns {Promise<object>} The user without the password.
 */
async function getProfile(userId) {
  const user = await prisma.user.findUnique({
    where: { id: userId },
    select: PUBLIC_USER_SELECT,
  });

  if (!user) {
    throw new ServiceError("User not found.", 404);
  }

  return user;
}

/**
 * Update an authenticated user's profile.
 *
 * Only fullName, phoneNumber, and profilePicture are updatable (the caller
 * has already restricted the payload to these). If the phone number changes,
 * reject it when another user already owns it. Returns the updated user
 * without the password.
 *
 * @param {string} userId - The authenticated user's id.
 * @param {object} updates - Sanitized subset of { fullName, phoneNumber, profilePicture }.
 * @returns {Promise<object>} The updated user without the password.
 */
async function updateProfile(userId, updates) {
  const existing = await prisma.user.findUnique({
    where: { id: userId },
    select: { id: true },
  });

  if (!existing) {
    throw new ServiceError("User not found.", 404);
  }

  // If the phone number is changing, ensure no other user already has it.
  if (updates.phoneNumber !== undefined) {
    const phoneOwner = await prisma.user.findFirst({
      where: {
        phoneNumber: updates.phoneNumber,
        NOT: { id: userId },
      },
      select: { id: true },
    });

    if (phoneOwner) {
      throw new ServiceError("Phone number is already registered.", 409, {
        phoneNumber: "Phone number is already registered.",
      });
    }
  }

  const user = await prisma.user.update({
    where: { id: userId },
    data: updates,
    select: PUBLIC_USER_SELECT,
  });

  return user;
}

/**
 * Delete the authenticated user's own account.
 *
 * Rejects deletion for ADMIN accounts (they must be removed through a
 * dedicated admin flow, not self-service). Returns 404 if the user no longer
 * exists. Related rows (refresh tokens, emergency contacts, incident reports,
 * notifications, friendships, sharing sessions) are removed automatically by
 * the database via the schema's onDelete: Cascade relations.
 *
 * @param {string} userId - The authenticated user's id.
 * @returns {Promise<void>}
 */
async function deleteAccount(userId) {
  const user = await prisma.user.findUnique({
    where: { id: userId },
    select: { id: true, role: true },
  });

  if (!user) {
    throw new ServiceError("User not found.", 404);
  }

  if (user.role === "ADMIN") {
    throw new ServiceError("Admin accounts cannot be deleted.", 403);
  }

  // Cascade deletes related rows per the schema's relation rules.
  await prisma.user.delete({
    where: { id: userId },
  });
}

/**
 * Log a user out by deleting the given refresh token.
 *
 * Scoped to the authenticated user so one user can never revoke another
 * user's token. Idempotent: deleting a token that is already gone is not an
 * error — the end state (token absent) is the same either way.
 *
 * @param {object} params
 * @param {string} params.userId - The authenticated user's id.
 * @param {string} params.refreshToken - The refresh token to revoke.
 * @returns {Promise<void>}
 */
async function logout({ userId, refreshToken }) {
  await prisma.refreshToken.deleteMany({
    where: { token: refreshToken, userId },
  });
}

/**
 * Verify a user's email address from a stateless verification token.
 *
 * Steps: verify the JWT (signature + expiry + expected type), look up the
 * user, reject if already verified, then set isVerified = true.
 *
 * @param {string} token - The email-verification JWT from the link.
 * @returns {Promise<object>} The verified user (safe fields only).
 */
async function verifyEmail(token) {
  if (!token || typeof token !== "string") {
    throw new ServiceError("Verification token is required.", 400);
  }

  // Verify signature and expiry. Map JWT failures to a 400 so we never leak
  // internal error shapes to the client.
  let payload;
  try {
    payload = verifyEmailVerificationToken(token);
  } catch (error) {
    if (error instanceof jwt.TokenExpiredError) {
      throw new ServiceError("Verification link has expired.", 400);
    }
    if (error instanceof jwt.JsonWebTokenError) {
      throw new ServiceError("Invalid verification token.", 400);
    }
    throw error;
  }

  // Guard against tokens of a different purpose being replayed here.
  if (payload.type !== "email_verification" || !payload.sub) {
    throw new ServiceError("Invalid verification token.", 400);
  }

  const user = await prisma.user.findUnique({
    where: { id: payload.sub },
    select: {
      id: true,
      fullName: true,
      username: true,
      email: true,
      phoneNumber: true,
      role: true,
      isVerified: true,
    },
  });

  if (!user) {
    throw new ServiceError("User not found.", 404);
  }

  // Reject if the account is already verified.
  if (user.isVerified) {
    throw new ServiceError("This account is already verified.", 409);
  }

  // Activate the account.
  const updated = await prisma.user.update({
    where: { id: user.id },
    data: { isVerified: true },
    select: {
      id: true,
      fullName: true,
      username: true,
      email: true,
      phoneNumber: true,
      role: true,
      isVerified: true,
    },
  });

  return updated;
}

module.exports = {
  ServiceError,
  register,
  login,
  refreshToken,
  forgotPassword,
  resetPassword,
  logout,
  verifyEmail,
  getProfile,
  updateProfile,
  deleteAccount,
};
