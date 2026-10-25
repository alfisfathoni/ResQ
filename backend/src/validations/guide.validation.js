// Guide request validation.
//
// Same hand-rolled pattern as the other validators: each function receives the
// request body and returns { valid, errors, data } where `data` holds the
// sanitized values the service should persist.

// Valid GuideCategory enum values (must match prisma/schema.prisma exactly).
const GUIDE_CATEGORIES = ["SAFETY", "FIRST_AID", "DISASTER", "EMERGENCY"];

function isNonEmptyString(value) {
  return typeof value === "string" && value.trim().length > 0;
}

/**
 * Validate the POST /api/guides payload.
 *
 * title, category, and content are required; thumbnail is optional and may be
 * null (to indicate "no thumbnail").
 * @param {object} body
 * @returns {{ valid: boolean, errors: Record<string,string>, data: object }}
 */
function validateCreateGuide(body = {}) {
  const errors = {};
  const data = {};

  // title
  const title = typeof body.title === "string" ? body.title.trim() : "";
  if (!isNonEmptyString(title)) {
    errors.title = "Title is required.";
  } else if (title.length > 255) {
    errors.title = "Title must be at most 255 characters.";
  } else {
    data.title = title;
  }

  // category
  if (!isNonEmptyString(body.category)) {
    errors.category = "Category is required.";
  } else if (!GUIDE_CATEGORIES.includes(body.category)) {
    errors.category = `Category must be one of: ${GUIDE_CATEGORIES.join(", ")}.`;
  } else {
    data.category = body.category;
  }

  // content
  const content = typeof body.content === "string" ? body.content.trim() : "";
  if (!isNonEmptyString(content)) {
    errors.content = "Content is required.";
  } else {
    data.content = content;
  }

  // thumbnail (optional, nullable to allow "no thumbnail")
  if (body.thumbnail !== undefined) {
    if (body.thumbnail === null) {
      data.thumbnail = null;
    } else if (typeof body.thumbnail === "string") {
      const thumbnail = body.thumbnail.trim();
      data.thumbnail = thumbnail.length > 0 ? thumbnail : null;
    } else {
      errors.thumbnail = "Thumbnail must be a string or null.";
    }
  }

  return {
    valid: Object.keys(errors).length === 0,
    errors,
    data,
  };
}

/**
 * Validate the PUT /api/guides/:id payload (partial update).
 *
 * Any subset of { title, category, content, thumbnail } may be provided, but
 * at least one is required and each supplied value must pass its field rules.
 * @param {object} body
 * @returns {{ valid: boolean, errors: Record<string,string>, data: object }}
 */
function validateUpdateGuide(body = {}) {
  const errors = {};
  const data = {};

  if (body.title !== undefined) {
    const title = typeof body.title === "string" ? body.title.trim() : "";
    if (!isNonEmptyString(title)) {
      errors.title = "Title cannot be empty.";
    } else if (title.length > 255) {
      errors.title = "Title must be at most 255 characters.";
    } else {
      data.title = title;
    }
  }

  if (body.category !== undefined) {
    if (!GUIDE_CATEGORIES.includes(body.category)) {
      errors.category = `Category must be one of: ${GUIDE_CATEGORIES.join(", ")}.`;
    } else {
      data.category = body.category;
    }
  }

  if (body.content !== undefined) {
    const content = typeof body.content === "string" ? body.content.trim() : "";
    if (!isNonEmptyString(content)) {
      errors.content = "Content cannot be empty.";
    } else {
      data.content = content;
    }
  }

  // thumbnail (optional, nullable to allow clearing it)
  if (body.thumbnail !== undefined) {
    if (body.thumbnail === null) {
      data.thumbnail = null;
    } else if (typeof body.thumbnail === "string") {
      const thumbnail = body.thumbnail.trim();
      data.thumbnail = thumbnail.length > 0 ? thumbnail : null;
    } else {
      errors.thumbnail = "Thumbnail must be a string or null.";
    }
  }

  // Reject an empty update body.
  if (Object.keys(data).length === 0 && Object.keys(errors).length === 0) {
    errors._ =
      "Provide at least one field to update (title, category, content, or thumbnail).";
  }

  return {
    valid: Object.keys(errors).length === 0,
    errors,
    data,
  };
}

module.exports = {
  validateCreateGuide,
  validateUpdateGuide,
};
