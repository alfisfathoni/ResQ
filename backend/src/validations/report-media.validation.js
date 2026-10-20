// Report media upload validation.
//
// Validates the uploaded file (from multer's req.file). Multer already
// enforces size and MIME at the middleware boundary; this validator is the
// defense-in-depth check that also resolves the MediaType enum value.

const MAX_FILE_SIZE = 20 * 1024 * 1024; // 20 MB

/**
 * Validate an uploaded report-media file.
 * @param {Express.Multer.File|undefined} file
 * @returns {{ valid: boolean, errors: Record<string,string>, data: { mediaType: "IMAGE"|"VIDEO", resourceType: "image"|"video" } }}
 */
function validateUploadMedia(file) {
  const errors = {};
  const data = {};

  if (!file) {
    errors.file = "A file is required.";
    return { valid: false, errors, data };
  }

  const mime = file.mimetype || "";

  if (mime.startsWith("image/")) {
    data.mediaType = "IMAGE";
    data.resourceType = "image";
  } else if (mime.startsWith("video/")) {
    data.mediaType = "VIDEO";
    data.resourceType = "video";
  } else {
    errors.file = "Only image and video files are allowed.";
  }

  if (file.size !== undefined && file.size > MAX_FILE_SIZE) {
    errors.file = "File must be at most 20 MB.";
  }

  return {
    valid: Object.keys(errors).length === 0,
    errors,
    data,
  };
}

module.exports = {
  validateUploadMedia,
  MAX_FILE_SIZE,
};
