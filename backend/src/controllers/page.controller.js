// Browser page controller.
//
// Serves the minimal HTML pages that users open from links in ResQ emails.
// These pages hold no business logic: the reset form posts to the existing
// JSON endpoint POST /api/auth/reset-password from the browser.

const { renderPage, sendHtml, createNonce } = require("../utils/html-page");

// Client-side logic for the reset form. Static code only (no interpolation),
// so it can be safely inlined; the token is read from the page URL.
const RESET_PASSWORD_SCRIPT = `
(function () {
  var form = document.getElementById("reset-form");
  var statusBox = document.getElementById("status");
  var button = document.getElementById("submit");
  var token = new URLSearchParams(window.location.search).get("token") || "";

  function showStatus(message, variant) {
    statusBox.textContent = message;
    statusBox.className = "status " + variant;
    statusBox.hidden = false;
  }

  if (!token) {
    showStatus("This reset link is invalid. Please request a new one from the app.", "error");
    form.hidden = true;
    return;
  }

  form.addEventListener("submit", function (event) {
    event.preventDefault();
    var newPassword = form.newPassword.value;
    var confirmPassword = form.confirmPassword.value;

    if (newPassword.length < 8 || newPassword.length > 72) {
      showStatus("Password must be between 8 and 72 characters.", "error");
      return;
    }
    if (newPassword !== confirmPassword) {
      showStatus("Passwords do not match.", "error");
      return;
    }

    button.disabled = true;
    showStatus("Resetting your password...", "info");

    fetch("/api/auth/reset-password", {
      method: "POST",
      headers: { "Content-Type": "application/json", Accept: "application/json" },
      body: JSON.stringify({ resetToken: token, newPassword: newPassword }),
    })
      .then(function (response) {
        return response.json().catch(function () { return {}; }).then(function (body) {
          return { ok: response.ok, body: body };
        });
      })
      .then(function (result) {
        if (result.ok) {
          showStatus("Password reset successfully. You can now log in from the app.", "success");
          form.hidden = true;
          return;
        }
        var body = result.body || {};
        var detail = body.errors && body.errors.newPassword;
        showStatus(detail || body.message || "Could not reset your password. Please try again.", "error");
        button.disabled = false;
      })
      .catch(function () {
        showStatus("Network error. Please check your connection and try again.", "error");
        button.disabled = false;
      });
  });
})();
`;

/**
 * GET /reset-password?token=...
 * Serves the password reset form. The token is validated by the API when the
 * form is submitted, not here.
 */
function resetPasswordPage(req, res) {
  const nonce = createNonce();
  const html = renderPage({
    title: "Reset password - ResQ",
    nonce,
    script: RESET_PASSWORD_SCRIPT,
    content: `
<h1>Reset your password</h1>
<p>Choose a new password for your ResQ account.</p>
<div id="status" class="status" role="status" aria-live="polite" hidden></div>
<form id="reset-form" novalidate>
  <label for="newPassword">New password</label>
  <input id="newPassword" name="newPassword" type="password" autocomplete="new-password" minlength="8" maxlength="72" required>
  <label for="confirmPassword">Confirm password</label>
  <input id="confirmPassword" name="confirmPassword" type="password" autocomplete="new-password" minlength="8" maxlength="72" required>
  <button id="submit" type="submit">Reset password</button>
</form>`,
  });

  return sendHtml(res, { html, nonce });
}

module.exports = {
  resetPasswordPage,
};
