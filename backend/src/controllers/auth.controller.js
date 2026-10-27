// Authentication controller.
//
// Thin HTTP layer: reads request input, delegates to auth.service, and
// shapes the response via the response utility. No business logic lives
// here. All handlers are placeholders for now.

const { sendSuccess, sendError } = require("../utils/response");
const { renderMessagePage, sendHtml } = require("../utils/html-page");
const authService = require("../services/auth.service");
const {
  validateRegister,
  validateLogin,
  validateRefreshToken,
  validateForgotPassword,
  validateResendVerification,
  validateResetPassword,
  validateLogout,
  validateUpdateProfile,
} = require("../validations/auth.validation");

/**
 * POST /api/auth/register
 * Validates input, delegates creation to the service, returns the new user.
 */
async function register(req, res) {
  try {
    const { valid, errors, data } = validateRegister(req.body);
    if (!valid) {
      return sendError(res, {
        statusCode: 422,
        message: "Validation failed.",
        errors,
      });
    }

    const user = await authService.register(data);

    return sendSuccess(res, {
      statusCode: 201,
      // isVerified is already true when the AUTO_VERIFY_EMAIL demo flag is on.
      message: user.isVerified
        ? "Registration successful. You can now log in."
        : "Registration successful. Please check your email to verify your account.",
      data: user,
    });
  } catch (error) {
    // Known, expected service errors (e.g. duplicates, validation).
    if (error instanceof authService.ServiceError) {
      return sendError(res, {
        statusCode: error.statusCode,
        message: error.message,
        errors: error.errors,
      });
    }

    // Unexpected error — log and return a generic 500.
    console.error("Register error:", error);
    return sendError(res, {
      statusCode: 500,
      message: "Something went wrong during registration.",
    });
  }
}

/**
 * POST /api/auth/login
 * Validates input, delegates authentication to the service, returns tokens
 * and the sanitized user.
 */
async function login(req, res) {
  try {
    const { valid, errors, data } = validateLogin(req.body);
    if (!valid) {
      return sendError(res, {
        statusCode: 422,
        message: "Validation failed.",
        errors,
      });
    }

    const result = await authService.login(data);

    return sendSuccess(res, {
      statusCode: 200,
      message: "Login successful.",
      data: result,
    });
  } catch (error) {
    if (error instanceof authService.ServiceError) {
      return sendError(res, {
        statusCode: error.statusCode,
        message: error.message,
        errors: error.errors,
      });
    }

    console.error("Login error:", error);
    return sendError(res, {
      statusCode: 500,
      message: "Something went wrong during login.",
    });
  }
}

/**
 * POST /api/auth/refresh
 * Validates the refresh token, delegates to the service, returns a new
 * access token only.
 */
async function refreshToken(req, res) {
  try {
    const { valid, errors, data } = validateRefreshToken(req.body);
    if (!valid) {
      return sendError(res, {
        statusCode: 422,
        message: "Validation failed.",
        errors,
      });
    }

    const result = await authService.refreshToken(data.refreshToken);

    return sendSuccess(res, {
      statusCode: 200,
      message: "Access token refreshed.",
      data: result,
    });
  } catch (error) {
    if (error instanceof authService.ServiceError) {
      return sendError(res, {
        statusCode: error.statusCode,
        message: error.message,
        errors: error.errors,
      });
    }

    console.error("Refresh token error:", error);
    return sendError(res, {
      statusCode: 500,
      message: "Something went wrong while refreshing the token.",
    });
  }
}

/**
 * POST /api/auth/forgot-password
 * Validates the email, delegates to the service, and always returns the same
 * success response so the existence of an account is never revealed.
 */
async function forgotPassword(req, res) {
  // A single, generic message returned in every non-validation case.
  const genericMessage =
    "If an account with that email exists, a password reset link has been sent.";

  try {
    const { valid, errors, data } = validateForgotPassword(req.body);
    if (!valid) {
      return sendError(res, {
        statusCode: 422,
        message: "Validation failed.",
        errors,
      });
    }

    await authService.forgotPassword(data);

    return sendSuccess(res, {
      statusCode: 200,
      message: genericMessage,
    });
  } catch (error) {
    // Even on unexpected errors, avoid leaking account state. Log internally
    // and still return the generic success response.
    console.error("Forgot password error:", error);
    return sendSuccess(res, {
      statusCode: 200,
      message: genericMessage,
    });
  }
}

/**
 * POST /api/auth/resend-verification
 * Validates the email, delegates to the service, and always returns the same
 * success response so the existence or verification state of an account is
 * never revealed.
 */
async function resendVerification(req, res) {
  const genericMessage =
    "If an unverified account exists with that email, a new verification link has been sent.";

  try {
    const { valid, errors, data } = validateResendVerification(req.body);
    if (!valid) {
      return sendError(res, {
        statusCode: 422,
        message: "Validation failed.",
        errors,
      });
    }

    await authService.resendVerification(data);

    return sendSuccess(res, {
      statusCode: 200,
      message: genericMessage,
    });
  } catch (error) {
    // Even on unexpected errors, avoid leaking account state.
    console.error("Resend verification error:", error);
    return sendSuccess(res, {
      statusCode: 200,
      message: genericMessage,
    });
  }
}

/**
 * POST /api/auth/reset-password
 * Validates input, delegates the reset to the service, and confirms success.
 */
async function resetPassword(req, res) {
  try {
    const { valid, errors, data } = validateResetPassword(req.body);
    if (!valid) {
      return sendError(res, {
        statusCode: 422,
        message: "Validation failed.",
        errors,
      });
    }

    await authService.resetPassword(data);

    return sendSuccess(res, {
      statusCode: 200,
      message:
        "Password reset successfully. Please log in with your new password.",
    });
  } catch (error) {
    if (error instanceof authService.ServiceError) {
      return sendError(res, {
        statusCode: error.statusCode,
        message: error.message,
        errors: error.errors,
      });
    }

    console.error("Reset password error:", error);
    return sendError(res, {
      statusCode: 500,
      message: "Something went wrong while resetting the password.",
    });
  }
}

/**
 * GET /api/auth/profile
 * Requires authentication. Returns the authenticated user's latest profile.
 */
async function getProfile(req, res) {
  try {
    const user = await authService.getProfile(req.user.id);

    return sendSuccess(res, {
      statusCode: 200,
      message: "Profile retrieved successfully.",
      data: user,
    });
  } catch (error) {
    if (error instanceof authService.ServiceError) {
      return sendError(res, {
        statusCode: error.statusCode,
        message: error.message,
        errors: error.errors,
      });
    }

    console.error("Get profile error:", error);
    return sendError(res, {
      statusCode: 500,
      message: "Something went wrong while retrieving the profile.",
    });
  }
}

/**
 * PUT /api/auth/profile
 * Requires authentication. Validates the allowed fields and updates the
 * authenticated user's profile.
 */
async function updateProfile(req, res) {
  try {
    const { valid, errors, data } = validateUpdateProfile(req.body);
    if (!valid) {
      return sendError(res, {
        statusCode: 422,
        message: "Validation failed.",
        errors,
      });
    }

    const user = await authService.updateProfile(req.user.id, data);

    return sendSuccess(res, {
      statusCode: 200,
      message: "Profile updated successfully.",
      data: user,
    });
  } catch (error) {
    if (error instanceof authService.ServiceError) {
      return sendError(res, {
        statusCode: error.statusCode,
        message: error.message,
        errors: error.errors,
      });
    }

    console.error("Update profile error:", error);
    return sendError(res, {
      statusCode: 500,
      message: "Something went wrong while updating the profile.",
    });
  }
}

/**
 * DELETE /api/auth/profile
 * Requires authentication. Deletes the authenticated user's own account
 * (rejected for ADMIN accounts); related data is removed by DB cascade.
 */
async function deleteAccount(req, res) {
  try {
    await authService.deleteAccount(req.user.id);

    return sendSuccess(res, {
      statusCode: 200,
      message: "Account deleted successfully.",
    });
  } catch (error) {
    if (error instanceof authService.ServiceError) {
      return sendError(res, {
        statusCode: error.statusCode,
        message: error.message,
        errors: error.errors,
      });
    }

    console.error("Delete account error:", error);
    return sendError(res, {
      statusCode: 500,
      message: "Something went wrong while deleting the account.",
    });
  }
}

/**
 * POST /api/auth/logout
 * Requires authentication. Validates the refresh token, revokes it for the
 * authenticated user, and returns success even if it was already gone.
 */
async function logout(req, res) {
  try {
    const { valid, errors, data } = validateLogout(req.body);
    if (!valid) {
      return sendError(res, {
        statusCode: 422,
        message: "Validation failed.",
        errors,
      });
    }

    await authService.logout({
      userId: req.user.id,
      refreshToken: data.refreshToken,
    });

    return sendSuccess(res, {
      statusCode: 200,
      message: "Logged out successfully.",
    });
  } catch (error) {
    if (error instanceof authService.ServiceError) {
      return sendError(res, {
        statusCode: error.statusCode,
        message: error.message,
        errors: error.errors,
      });
    }

    console.error("Logout error:", error);
    return sendError(res, {
      statusCode: 500,
      message: "Something went wrong during logout.",
    });
  }
}

/**
 * GET /api/auth/verify-email?token=...
 * Reads the token from the query string, delegates verification to the
 * service, and returns the verified user.
 *
 * Content negotiation: a browser opening the email link (Accept: text/html)
 * gets a friendly HTML page; API clients (Accept: application/json, or a
 * wildcard such as Postman's default) keep the JSON envelope. Status codes
 * are identical in both formats.
 */
async function verifyEmail(req, res) {
  const wantsHtml = req.accepts(["json", "html"]) === "html";

  try {
    const { token } = req.query;

    const user = await authService.verifyEmail(token);
    const message = "Email verified successfully. You can now log in.";

    if (wantsHtml) {
      return sendHtml(res, {
        html: renderMessagePage({
          title: "Email verified",
          message: "Your email has been verified. You can now log in from the ResQ app.",
          variant: "success",
        }),
      });
    }

    return sendSuccess(res, {
      statusCode: 200,
      message,
      data: user,
    });
  } catch (error) {
    const known = error instanceof authService.ServiceError;
    if (!known) {
      console.error("Verify email error:", error);
    }

    const statusCode = known ? error.statusCode : 500;
    const message = known
      ? error.message
      : "Something went wrong during email verification.";

    if (wantsHtml) {
      // 409 means the account is already verified: not a failure for the user.
      const alreadyVerified = statusCode === 409;
      return sendHtml(res, {
        statusCode,
        html: renderMessagePage({
          title: alreadyVerified ? "Already verified" : "Verification failed",
          message: alreadyVerified
            ? "This account is already verified. You can log in from the ResQ app."
            : `${message} You can request a new verification link from the ResQ app.`,
          variant: alreadyVerified ? "info" : "error",
        }),
      });
    }

    return sendError(res, {
      statusCode,
      message,
      errors: known ? error.errors : null,
    });
  }
}

module.exports = {
  register,
  login,
  refreshToken,
  forgotPassword,
  resendVerification,
  resetPassword,
  logout,
  verifyEmail,
  getProfile,
  updateProfile,
  deleteAccount,
};
