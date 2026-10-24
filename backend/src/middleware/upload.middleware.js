// File upload middleware (multer).
//
// Uses in-memory storage so the file buffer can be streamed straight to
// Cloudinary without touching disk. Enforces a 20 MB size limit and accepts
// only image/* and video/* MIME types. A rejected MIME type is surfaced via
// a flagged error so the route can return 422 rather than 500.

const multer = require("multer");

const MAX_FILE_SIZE = 20 * 1024 * 1024; // 20 MB

function fileFilter(req, file, cb) {
  const mime = file.mimetype || "";
  if (mime.startsWith("image/") || mime.startsWith("video/")) {
    return cb(null, true);
  }
  const err = new Error("Only image and video files are allowed.");
  err.code = "UNSUPPORTED_MEDIA_TYPE";
  return cb(err, false);
}

const upload = multer({
  storage: multer.memoryStorage(),
  limits: { fileSize: MAX_FILE_SIZE },
  fileFilter,
});

module.exports = {
  upload,
  MAX_FILE_SIZE,
};
