// Minimal HTML page renderer.
//
// Builds the small, self-contained pages that are opened from links in emails
// (email verification result, password reset form). Everything is inline so
// the pages work on a phone browser without extra assets. All dynamic text is
// escaped; callers never pass raw user input as HTML.

const crypto = require("crypto");

/**
 * Escape a value for safe use in HTML text and attribute contexts.
 * @param {*} value
 * @returns {string}
 */
function escapeHtml(value) {
  return String(value)
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#39;");
}

const STYLES = `
  :root {
    color-scheme: light dark;
    --bg: #f4f5f7; --card: #ffffff; --text: #1f2328; --muted: #5b616b;
    --border: #d9dce1; --brand: #d62828; --brand-text: #ffffff;
    --ok-bg: #e7f6ec; --ok-text: #136c34; --err-bg: #fdecec; --err-text: #a11c1c;
    --info-bg: #eaf2fd; --info-text: #1d4f91;
  }
  @media (prefers-color-scheme: dark) {
    :root {
      --bg: #121417; --card: #1c1f24; --text: #e8eaed; --muted: #a0a6ae;
      --border: #343941; --brand: #ef4444; --brand-text: #ffffff;
      --ok-bg: #12301d; --ok-text: #7ddc9b; --err-bg: #3a1616; --err-text: #f6a3a3;
      --info-bg: #142740; --info-text: #9cc3f5;
    }
  }
  * { box-sizing: border-box; }
  body {
    margin: 0; min-height: 100vh; display: flex; align-items: center; justify-content: center;
    padding: 16px; background: var(--bg); color: var(--text);
    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
    line-height: 1.5;
  }
  .card {
    width: 100%; max-width: 420px; background: var(--card); border: 1px solid var(--border);
    border-radius: 12px; padding: 28px 24px;
  }
  .brand { font-weight: 700; font-size: 20px; color: var(--brand); margin: 0 0 16px; letter-spacing: 0.5px; }
  h1 { font-size: 22px; margin: 0 0 8px; }
  p { margin: 0 0 16px; color: var(--muted); }
  label { display: block; font-size: 14px; font-weight: 600; margin: 0 0 6px; }
  input {
    width: 100%; font-size: 16px; padding: 12px; margin: 0 0 16px; border-radius: 8px;
    border: 1px solid var(--border); background: var(--card); color: var(--text);
  }
  input:focus { outline: 2px solid var(--brand); outline-offset: 1px; }
  button {
    width: 100%; font-size: 16px; font-weight: 600; padding: 12px; border: 0; border-radius: 8px;
    background: var(--brand); color: var(--brand-text); cursor: pointer;
  }
  button:disabled { opacity: 0.6; cursor: default; }
  .status { padding: 12px; border-radius: 8px; margin: 0 0 16px; font-size: 15px; }
  .status[hidden] { display: none; }
  .status.success { background: var(--ok-bg); color: var(--ok-text); }
  .status.error { background: var(--err-bg); color: var(--err-text); }
  .status.info { background: var(--info-bg); color: var(--info-text); }
`;

/**
 * Render a full HTML document.
 * @param {object} options
 * @param {string} options.title - Document title (escaped).
 * @param {string} options.content - Inner HTML of the card. Must already be safe.
 * @param {string} [options.script] - Inline script body (trusted, static code only).
 * @param {string} [options.nonce] - CSP nonce applied to the inline script.
 * @returns {string}
 */
function renderPage({ title, content, script, nonce }) {
  const scriptTag = script ? `<script nonce="${escapeHtml(nonce)}">${script}</script>` : "";
  return `<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<meta name="robots" content="noindex">
<title>${escapeHtml(title)}</title>
<style>${STYLES}</style>
</head>
<body>
<main class="card">
<p class="brand">ResQ</p>
${content}
</main>
${scriptTag}
</body>
</html>`;
}

/**
 * Render a simple result page (heading + message in a coloured status box).
 * @param {object} options
 * @param {string} options.title - Page heading and document title.
 * @param {string} options.message - Message shown in the status box.
 * @param {"success"|"error"|"info"} [options.variant="info"]
 * @returns {string}
 */
function renderMessagePage({ title, message, variant = "info" }) {
  return renderPage({
    title: `${title} - ResQ`,
    content:
      `<h1>${escapeHtml(title)}</h1>` +
      `<div class="status ${escapeHtml(variant)}" role="status">${escapeHtml(message)}</div>`,
  });
}

/**
 * Send an HTML page with security headers suited to one-off email-link pages:
 * no caching (the URL carries a token) and a CSP that only allows the page's
 * own inline script (by nonce) and same-origin requests.
 * @param {import("express").Response} res
 * @param {object} options
 * @param {number} [options.statusCode=200]
 * @param {string} options.html
 * @param {string} [options.nonce] - Nonce used by the page's inline script, if any.
 */
function sendHtml(res, { statusCode = 200, html, nonce }) {
  const scriptSrc = nonce ? `'nonce-${nonce}'` : "'none'";
  res.set({
    "Content-Security-Policy":
      `default-src 'self'; script-src ${scriptSrc}; style-src 'unsafe-inline'; ` +
      "img-src 'self' data:; connect-src 'self'; form-action 'self'; " +
      "base-uri 'none'; frame-ancestors 'none'; object-src 'none'",
    "Cache-Control": "no-store",
  });
  return res.status(statusCode).type("html").send(html);
}

/**
 * Generate a fresh CSP nonce.
 * @returns {string}
 */
function createNonce() {
  return crypto.randomBytes(16).toString("base64");
}

module.exports = {
  escapeHtml,
  renderPage,
  renderMessagePage,
  sendHtml,
  createNonce,
};
