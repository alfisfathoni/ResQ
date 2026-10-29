// Phone number helpers.
//
// Phone numbers are stored in free-ish formats (the register validator accepts
// an optional "+" followed by 7-15 digits), so the same person may appear as
// "0812...", "62812..." or "+62812...". These helpers normalize a number to one
// canonical form and expand it to every stored variant, so two numbers can be
// compared with a single `IN (...)` query instead of scanning a whole table.

// Canonical form: "+" followed by 7-15 digits (E.164 style).
const NORMALIZED_PATTERN = /^\+\d{7,15}$/;

/**
 * Normalize a phone number to "+<digits>" form.
 *
 * Rules: spaces, hyphens and parentheses are removed; "08..." becomes
 * "+628..."; "628..." becomes "+628..."; "+62..." stays as is; any other
 * "+<digits>" number stays as is; "00<digits>" (international prefix) becomes
 * "+<digits>". Anything else (no recognizable prefix, letters, wrong length)
 * is invalid.
 *
 * @param {string} input - Raw phone number.
 * @returns {string|null} Normalized "+<digits>" number, or null when invalid.
 */
function normalizePhone(input) {
  if (typeof input !== "string") return null;

  const cleaned = input.trim().replace(/[\s\-()]/g, "");
  if (cleaned === "") return null;

  let candidate;
  if (cleaned.startsWith("+")) {
    candidate = cleaned;
  } else if (cleaned.startsWith("00")) {
    candidate = `+${cleaned.slice(2)}`;
  } else if (cleaned.startsWith("0")) {
    candidate = `+62${cleaned.slice(1)}`;
  } else if (cleaned.startsWith("62")) {
    candidate = `+${cleaned}`;
  } else {
    return null;
  }

  return NORMALIZED_PATTERN.test(candidate) ? candidate : null;
}

/**
 * List every stored spelling of a normalized number, for use in a Prisma
 * `phoneNumber: { in: [...] }` filter.
 *
 * Indonesian numbers ("+62...") expand to "+62...", "62..." and "0...".
 * Other international numbers expand to "+<digits>" and "<digits>".
 *
 * @param {string} normalized - A value returned by normalizePhone().
 * @returns {string[]} Variants (empty array when the input is not normalized).
 */
function phoneVariants(normalized) {
  if (typeof normalized !== "string" || !NORMALIZED_PATTERN.test(normalized)) {
    return [];
  }

  if (normalized.startsWith("+62")) {
    const national = normalized.slice(3);
    return [`+62${national}`, `62${national}`, `0${national}`];
  }

  return [normalized, normalized.slice(1)];
}

module.exports = { normalizePhone, phoneVariants };
