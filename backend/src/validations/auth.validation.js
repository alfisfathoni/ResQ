// Authentication request validation.
//
// Hand-rolled validators (no external dependency) that return a normalized
// result: { valid: boolean, errors: object, data: object }. `errors` maps a
// field name to a human-readable message; `data` holds the sanitized values
// (trimmed, lowercased email) that the service should persist.

const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
// E.164-ish: optional leading +, then 7-15 digits.
const PHONE_REGEX = /^\+?\d{7,15}$/;
const USERNAME_REGEX = /^[a-zA-Z0-9_.]+$/;

function isNonEmptyString(value) {
  return typeof value === "string" && value.trim().length > 0;
}

/**
 * Validate the POST /api/auth/register payload.
 * @param {object} body
 * @returns {{ valid: boolean, errors: Record<string,string>, data: object }}
 */
function validateRegister(body = {}) {
  const errors = {};

  const fullName = typeof body.fullName === "string" ? body.fullName.trim() : "";
  const username = typeof body.username === "string" ? body.username.trim() : "";
  const email = typeof body.email === "string" ? body.email.trim().toLowerCase() : "";
  const phoneNumber = typeof body.phoneNumber === "string" ? body.phoneNumber.trim() : "";
  const password = typeof body.password === "string" ? body.password : "";

  // fullName
  if (!isNonEmptyString(fullName)) {
    errors.fullName = "Full name is required.";
  } else if (fullName.length > 255) {
    errors.fullName = "Full name must be at most 255 characters.";
  }

  // username
  if (!isNonEmptyString(username)) {
    errors.username = "Username is required.";
  } else if (username.length < 3 || username.length > 100) {
    errors.username = "Username must be between 3 and 100 characters.";
  } else if (!USERNAME_REGEX.test(username)) {
    errors.username = "Username may only contain letters, numbers, underscores, and dots.";
  }

  // email
  if (!isNonEmptyString(email)) {
    errors.email = "Email is required.";
  } else if (!EMAIL_REGEX.test(email) || email.length > 255) {
    errors.email = "A valid email address is required.";
  }

  // phoneNumber
  if (!isNonEmptyString(phoneNumber)) {
    errors.phoneNumber = "Phone number is required.";
  } else if (!PHONE_REGEX.test(phoneNumber)) {
    errors.phoneNumber = "A valid phone number is required (7-15 digits, optional leading +).";
  }

  // password
  if (!isNonEmptyString(password)) {
    errors.password = "Password is required.";
  } else if (password.length < 8 || password.length > 72) {
    // bcrypt only uses the first 72 bytes, so cap there.
    errors.password = "Password must be between 8 and 72 characters.";
  }

  return {
    valid: Object.keys(errors).length === 0,
    errors,
    data: { fullName, username, email, phoneNumber, password },
  };
}

/**
 * Validate the POST /api/auth/login payload.
 *
 * Accepts an `identifier` (email or username) plus a password. For backward
 * compatibility it also reads `email` or `username` if `identifier` is absent.
 * @param {object} body
 * @returns {{ valid: boolean, errors: Record<string,string>, data: { identifier: string, password: string } }}
 */
function validateLogin(body = {}) {
  const errors = {};

  const rawIdentifier =
    (typeof body.identifier === "string" && body.identifier) ||
    (typeof body.email === "string" && body.email) ||
    (typeof body.username === "string" && body.username) ||
    "";
  const identifier = rawIdentifier.trim();
  const password = typeof body.password === "string" ? body.password : "";

  if (!isNonEmptyString(identifier)) {
    errors.identifier = "Email or username is required.";
  }

  if (!isNonEmptyString(password)) {
    errors.password = "Password is required.";
  }

  return {
    valid: Object.keys(errors).length === 0,
    errors,
    data: { identifier, password },
  };
}

/**
 * Validate the POST /api/auth/refresh payload.
 * @param {object} body
 * @returns {{ valid: boolean, errors: Record<string,string>, data: { refreshToken: string } }}
 */
function validateRefreshToken(body = {}) {
  const errors = {};

  const refreshToken =
    typeof body.refreshToken === "string" ? body.refreshToken.trim() : "";

  if (!isNonEmptyString(refreshToken)) {
    errors.refreshToken = "Refresh token is required.";
  }

  return {
    valid: Object.keys(errors).length === 0,
    errors,
    data: { refreshToken },
  };
}

/**
 * Validate the POST /api/auth/forgot-password payload.
 * @param {object} body
 * @returns {{ valid: boolean, errors: Record<string,string>, data: { email: string } }}
 */
function validateForgotPassword(body = {}) {
  const errors = {};

  const email = typeof body.email === "string" ? body.email.trim().toLowerCase() : "";

  if (!isNonEmptyString(email)) {
    errors.email = "Email is required.";
  } else if (!EMAIL_REGEX.test(email) || email.length > 255) {
    errors.email = "A valid email address is required.";
  }

  return {
    valid: Object.keys(errors).length === 0,
    errors,
    data: { email },
  };
}

/**
 * Validate the POST /api/auth/reset-password payload.
 * @param {object} body
 * @returns {{ valid: boolean, errors: Record<string,string>, data: { resetToken: string, newPassword: string } }}
 */
function validateResetPassword(body = {}) {
  const errors = {};

  const resetToken =
    typeof body.resetToken === "string" ? body.resetToken.trim() : "";
  const newPassword =
    typeof body.newPassword === "string" ? body.newPassword : "";

  if (!isNonEmptyString(resetToken)) {
    errors.resetToken = "Reset token is required.";
  }

  if (!isNonEmptyString(newPassword)) {
    errors.newPassword = "New password is required.";
  } else if (newPassword.length < 8 || newPassword.length > 72) {
    // bcrypt only uses the first 72 bytes, so cap there.
    errors.newPassword = "New password must be between 8 and 72 characters.";
  }

  return {
    valid: Object.keys(errors).length === 0,
    errors,
    data: { resetToken, newPassword },
  };
}

/**
 * Validate the POST /api/auth/logout payload.
 * @param {object} body
 * @returns {{ valid: boolean, errors: Record<string,string>, data: { refreshToken: string } }}
 */
function validateLogout(body = {}) {
  const errors = {};

  const refreshToken =
    typeof body.refreshToken === "string" ? body.refreshToken.trim() : "";

  if (!isNonEmptyString(refreshToken)) {
    errors.refreshToken = "Refresh token is required.";
  }

  return {
    valid: Object.keys(errors).length === 0,
    errors,
    data: { refreshToken },
  };
}

/**
 * Validate the PUT /api/auth/profile payload.
 *
 * Only fullName, phoneNumber, and profilePicture may be updated. The update
 * is partial: any subset of those three may be provided, but at least one is
 * required, and each provided value must pass its field rules.
 * @param {object} body
 * @returns {{ valid: boolean, errors: Record<string,string>, data: object }}
 */
function validateUpdateProfile(body = {}) {
  const errors = {};
  const data = {};

  // fullName (optional)
  if (body.fullName !== undefined) {
    const fullName = typeof body.fullName === "string" ? body.fullName.trim() : "";
    if (!isNonEmptyString(fullName)) {
      errors.fullName = "Full name cannot be empty.";
    } else if (fullName.length > 255) {
      errors.fullName = "Full name must be at most 255 characters.";
    } else {
      data.fullName = fullName;
    }
  }

  // phoneNumber (optional)
  if (body.phoneNumber !== undefined) {
    const phoneNumber =
      typeof body.phoneNumber === "string" ? body.phoneNumber.trim() : "";
    if (!isNonEmptyString(phoneNumber)) {
      errors.phoneNumber = "Phone number cannot be empty.";
    } else if (!PHONE_REGEX.test(phoneNumber)) {
      errors.phoneNumber =
        "A valid phone number is required (7-15 digits, optional leading +).";
    } else {
      data.phoneNumber = phoneNumber;
    }
  }

  // profilePicture (optional, nullable to allow clearing it)
  if (body.profilePicture !== undefined) {
    if (body.profilePicture === null) {
      data.profilePicture = null;
    } else if (typeof body.profilePicture === "string") {
      const profilePicture = body.profilePicture.trim();
      data.profilePicture = profilePicture.length > 0 ? profilePicture : null;
    } else {
      errors.profilePicture = "Profile picture must be a string or null.";
    }
  }

  // Require at least one updatable field.
  if (Object.keys(data).length === 0 && Object.keys(errors).length === 0) {
    errors._ =
      "Provide at least one field to update (fullName, phoneNumber, or profilePicture).";
  }

  return {
    valid: Object.keys(errors).length === 0,
    errors,
    data,
  };
}

module.exports = {
  validateRegister,
  validateLogin,
  validateRefreshToken,
  validateForgotPassword,
  validateResetPassword,
  validateLogout,
  validateUpdateProfile,
};
