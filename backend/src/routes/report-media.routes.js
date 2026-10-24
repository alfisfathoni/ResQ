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

/**
 * @swagger
 * tags:
 *   name: Report Media
 *   description: Upload, retrieval, and deletion of multimedia attachments for incident reports
 */

/**
 * @swagger
 * components:
 *   schemas:
 *     MediaType:
 *       type: string
 *       enum: [IMAGE, VIDEO]
 *       example: "IMAGE"
 *     ReportMedia:
 *       type: object
 *       properties:
 *         id:
 *           type: string
 *           format: uuid
 *           example: "f2c3d4e5-6789-01bc-def2-34567890abcd"
 *         reportId:
 *           type: string
 *           format: uuid
 *           example: "a89d7b42-1c23-4567-89ab-cdef01234567"
 *         mediaUrl:
 *           type: string
 *           format: uri
 *           example: "https://res.cloudinary.com/demo/image/upload/v1234567890/report_sample.jpg"
 *         mediaType:
 *           $ref: '#/components/schemas/MediaType'
 *         createdAt:
 *           type: string
 *           format: date-time
 *           example: "2026-10-24T00:00:00.000Z"
 *     UploadReportMediaRequest:
 *       type: object
 *       required:
 *         - file
 *       properties:
 *         file:
 *           type: string
 *           format: binary
 *           description: "Image or video file attachment (max 20 MB, JPEG/PNG/WebP/MP4/MOV)"
 */

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

/**
 * @swagger
 * /api/reports/{reportId}/media:
 *   post:
 *     summary: Upload media to incident report
 *     description: Uploads an image or video file (up to 20 MB) to Cloudinary and links it to the specified incident report.
 *     tags: [Report Media]
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: path
 *         name: reportId
 *         required: true
 *         schema:
 *           type: string
 *           format: uuid
 *         description: Incident report ID
 *         example: "a89d7b42-1c23-4567-89ab-cdef01234567"
 *     requestBody:
 *       required: true
 *       content:
 *         multipart/form-data:
 *           schema:
 *             $ref: '#/components/schemas/UploadReportMediaRequest'
 *     responses:
 *       200:
 *         description: Media uploaded successfully (alternative success code)
 *         content:
 *           application/json:
 *             schema:
 *               allOf:
 *                 - $ref: '#/components/schemas/SuccessResponse'
 *                 - properties:
 *                     data:
 *                       $ref: '#/components/schemas/ReportMedia'
 *       201:
 *         description: Media uploaded successfully
 *         content:
 *           application/json:
 *             schema:
 *               allOf:
 *                 - $ref: '#/components/schemas/SuccessResponse'
 *                 - properties:
 *                     data:
 *                       $ref: '#/components/schemas/ReportMedia'
 *       400:
 *         description: Bad request
 *         content:
 *           application/json:
 *             schema:
 *               $ref: '#/components/schemas/ErrorResponse'
 *       401:
 *         description: Authentication required or invalid token
 *         content:
 *           application/json:
 *             schema:
 *               $ref: '#/components/schemas/ErrorResponse'
 *       404:
 *         description: Incident report not found or not owned by user
 *         content:
 *           application/json:
 *             schema:
 *               $ref: '#/components/schemas/ErrorResponse'
 *       422:
 *         description: Validation failed (e.g. file missing, exceeding 20 MB, or unsupported MIME type)
 *         content:
 *           application/json:
 *             schema:
 *               $ref: '#/components/schemas/ErrorResponse'
 *       500:
 *         description: Internal server error
 *         content:
 *           application/json:
 *             schema:
 *               $ref: '#/components/schemas/ErrorResponse'
 *       502:
 *         description: Bad gateway - Cloudinary asset upload failed
 *         content:
 *           application/json:
 *             schema:
 *               $ref: '#/components/schemas/ErrorResponse'
 *   get:
 *     summary: List media for an incident report
 *     description: Retrieves all media items uploaded for the specified incident report owned by the authenticated user, ordered oldest first.
 *     tags: [Report Media]
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: path
 *         name: reportId
 *         required: true
 *         schema:
 *           type: string
 *           format: uuid
 *         description: Incident report ID
 *         example: "a89d7b42-1c23-4567-89ab-cdef01234567"
 *     responses:
 *       200:
 *         description: Media attachments retrieved successfully
 *         content:
 *           application/json:
 *             schema:
 *               allOf:
 *                 - $ref: '#/components/schemas/SuccessResponse'
 *                 - properties:
 *                     data:
 *                       type: array
 *                       items:
 *                         $ref: '#/components/schemas/ReportMedia'
 *       400:
 *         description: Bad request
 *         content:
 *           application/json:
 *             schema:
 *               $ref: '#/components/schemas/ErrorResponse'
 *       401:
 *         description: Authentication required or invalid token
 *         content:
 *           application/json:
 *             schema:
 *               $ref: '#/components/schemas/ErrorResponse'
 *       404:
 *         description: Incident report not found or not owned by user
 *         content:
 *           application/json:
 *             schema:
 *               $ref: '#/components/schemas/ErrorResponse'
 *       500:
 *         description: Internal server error
 *         content:
 *           application/json:
 *             schema:
 *               $ref: '#/components/schemas/ErrorResponse'
 */
router.post("/", uploadSingle, reportMediaController.create);
router.get("/", reportMediaController.list);

/**
 * @swagger
 * /api/reports/{reportId}/media/{mediaId}:
 *   delete:
 *     summary: Delete media from an incident report
 *     description: Deletes an uploaded media item from Cloudinary and removes its record from the database.
 *     tags: [Report Media]
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: path
 *         name: reportId
 *         required: true
 *         schema:
 *           type: string
 *           format: uuid
 *         description: Incident report ID
 *         example: "a89d7b42-1c23-4567-89ab-cdef01234567"
 *       - in: path
 *         name: mediaId
 *         required: true
 *         schema:
 *           type: string
 *           format: uuid
 *         description: Report media ID
 *         example: "f2c3d4e5-6789-01bc-def2-34567890abcd"
 *     responses:
 *       200:
 *         description: Media deleted successfully
 *         content:
 *           application/json:
 *             schema:
 *               $ref: '#/components/schemas/SuccessResponse'
 *       400:
 *         description: Bad request
 *         content:
 *           application/json:
 *             schema:
 *               $ref: '#/components/schemas/ErrorResponse'
 *       401:
 *         description: Authentication required or invalid token
 *         content:
 *           application/json:
 *             schema:
 *               $ref: '#/components/schemas/ErrorResponse'
 *       404:
 *         description: Incident report or media item not found or not owned by user
 *         content:
 *           application/json:
 *             schema:
 *               $ref: '#/components/schemas/ErrorResponse'
 *       500:
 *         description: Internal server error
 *         content:
 *           application/json:
 *             schema:
 *               $ref: '#/components/schemas/ErrorResponse'
 */
router.delete("/:mediaId", reportMediaController.remove);

module.exports = router;
