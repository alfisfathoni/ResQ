// Report media routes.
//
// Mounted at /api/reports/:reportId/media. mergeParams lets handlers read the
// parent :reportId. All endpoints require authentication.

const express = require("express");
const multer = require("multer");
const reportMediaController = require("../controllers/report-media.controller");
const { authenticate } = require("../middleware/auth.middleware");
const { upload } = require("../middleware/upload.middleware");
const { sendError } = require("../utils/response");

const router = express.Router({ mergeParams: true });

router.use(authenticate);

/**
 * Run multer for a single "file" field, translating multer-specific errors
 * (size limit exceeded, unsupported MIME type) into a standardized 422 so the
 * client gets a validation error rather than a 500.
 */
function uploadSingle(req, res, next) {
  upload.single("file")(req, res, (err) => {
    if (!err) return next();

    if (err instanceof multer.MulterError) {
      if (err.code === "LIMIT_FILE_SIZE") {
        return sendError(res, {
          statusCode: 422,
          message: "Validation failed.",
          errors: { file: "File must be at most 20 MB." },
        });
      }
      return sendError(res, {
        statusCode: 422,
        message: "Validation failed.",
        errors: { file: err.message },
      });
    }

    if (err.code === "UNSUPPORTED_MEDIA_TYPE") {
      return sendError(res, {
        statusCode: 422,
        message: "Validation failed.",
        errors: { file: "Only image and video files are allowed." },
      });
    }

    return sendError(res, {
      statusCode: 500,
      message: "File upload failed.",
    });
  });
}

router.post("/", uploadSingle, reportMediaController.create);
router.get("/", reportMediaController.list);
router.delete("/:mediaId", reportMediaController.remove);

module.exports = router;
