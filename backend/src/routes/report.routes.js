// Incident report routes.
//
// All endpoints require authentication via the existing authenticate
// middleware. Mounted at /api/reports in app.js.

const express = require("express");
const reportController = require("../controllers/report.controller");
const { authenticate } = require("../middleware/auth.middleware");

const router = express.Router();

/**
 * @swagger
 * tags:
 *   name: Reports
 *   description: Incident reporting, geolocated hazard tracking, and status updates
 */

/**
 * @swagger
 * components:
 *   schemas:
 *     IncidentCategory:
 *       type: string
 *       enum: [CRIME, ACCIDENT, FIRE, MEDICAL, DISASTER, SUSPICIOUS, OTHER]
 *       example: "ACCIDENT"
 *     IncidentReport:
 *       type: object
 *       properties:
 *         id:
 *           type: string
 *           format: uuid
 *           example: "a89d7b42-1c23-4567-89ab-cdef01234567"
 *         userId:
 *           type: string
 *           format: uuid
 *           example: "d3b07384-d113-4944-9c8e-aa67c09e3e76"
 *         category:
 *           $ref: '#/components/schemas/IncidentCategory'
 *         title:
 *           type: string
 *           example: "Traffic collision at intersection"
 *         description:
 *           type: string
 *           nullable: true
 *           example: "Two motorbikes collided. Medical assistance requested."
 *         latitude:
 *           type: number
 *           format: float
 *           minimum: -90
 *           maximum: 90
 *           example: 10.762622
 *         longitude:
 *           type: number
 *           format: float
 *           minimum: -180
 *           maximum: 180
 *           example: 106.660172
 *         createdAt:
 *           type: string
 *           format: date-time
 *           example: "2026-10-24T00:00:00.000Z"
 *         updatedAt:
 *           type: string
 *           format: date-time
 *           example: "2026-10-24T00:00:00.000Z"
 *     CreateReportRequest:
 *       type: object
 *       required:
 *         - category
 *         - title
 *         - latitude
 *         - longitude
 *       properties:
 *         category:
 *           $ref: '#/components/schemas/IncidentCategory'
 *         title:
 *           type: string
 *           maxLength: 255
 *           example: "Traffic collision at intersection"
 *         description:
 *           type: string
 *           nullable: true
 *           example: "Two motorbikes collided. Medical assistance requested."
 *         latitude:
 *           type: number
 *           format: float
 *           minimum: -90
 *           maximum: 90
 *           example: 10.762622
 *         longitude:
 *           type: number
 *           format: float
 *           minimum: -180
 *           maximum: 180
 *           example: 106.660172
 *     UpdateReportRequest:
 *       type: object
 *       description: "Provide at least one field to update"
 *       properties:
 *         category:
 *           $ref: '#/components/schemas/IncidentCategory'
 *         title:
 *           type: string
 *           maxLength: 255
 *           example: "Updated accident report"
 *         description:
 *           type: string
 *           nullable: true
 *           example: "Situation resolved by local responders."
 *         latitude:
 *           type: number
 *           format: float
 *           minimum: -90
 *           maximum: 90
 *           example: 10.762622
 *         longitude:
 *           type: number
 *           format: float
 *           minimum: -180
 *           maximum: 180
 *           example: 106.660172
 */

router.use(authenticate);

/**
 * @swagger
 * /api/reports:
 *   post:
 *     summary: Create an incident report
 *     description: Submits a new incident report with category, title, coordinates, and optional description.
 *     tags: [Reports]
 *     security:
 *       - bearerAuth: []
 *     requestBody:
 *       required: true
 *       content:
 *         application/json:
 *           schema:
 *             $ref: '#/components/schemas/CreateReportRequest'
 *     responses:
 *       200:
 *         description: Incident report created successfully (alternative success code)
 *         content:
 *           application/json:
 *             schema:
 *               allOf:
 *                 - $ref: '#/components/schemas/SuccessResponse'
 *                 - properties:
 *                     data:
 *                       $ref: '#/components/schemas/IncidentReport'
 *       201:
 *         description: Incident report created successfully
 *         content:
 *           application/json:
 *             schema:
 *               allOf:
 *                 - $ref: '#/components/schemas/SuccessResponse'
 *                 - properties:
 *                     data:
 *                       $ref: '#/components/schemas/IncidentReport'
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
 *         description: User not found
 *         content:
 *           application/json:
 *             schema:
 *               $ref: '#/components/schemas/ErrorResponse'
 *       422:
 *         description: Validation failed on payload fields
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
 *   get:
 *     summary: List all incident reports
 *     description: Retrieves all incident reports owned by the authenticated user, ordered newest first.
 *     tags: [Reports]
 *     security:
 *       - bearerAuth: []
 *     responses:
 *       200:
 *         description: Incident reports retrieved successfully
 *         content:
 *           application/json:
 *             schema:
 *               allOf:
 *                 - $ref: '#/components/schemas/SuccessResponse'
 *                 - properties:
 *                     data:
 *                       type: array
 *                       items:
 *                         $ref: '#/components/schemas/IncidentReport'
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
 *         description: User not found
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
router.post("/", reportController.create);
router.get("/", reportController.list);

/**
 * @swagger
 * /api/reports/{id}:
 *   get:
 *     summary: Get incident report by ID
 *     description: Retrieves the detailed record of an incident report owned by the authenticated user.
 *     tags: [Reports]
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: path
 *         name: id
 *         required: true
 *         schema:
 *           type: string
 *           format: uuid
 *         description: Incident report ID
 *         example: "a89d7b42-1c23-4567-89ab-cdef01234567"
 *     responses:
 *       200:
 *         description: Incident report retrieved successfully
 *         content:
 *           application/json:
 *             schema:
 *               allOf:
 *                 - $ref: '#/components/schemas/SuccessResponse'
 *                 - properties:
 *                     data:
 *                       $ref: '#/components/schemas/IncidentReport'
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
 *   put:
 *     summary: Update an incident report
 *     description: Partially updates an existing incident report owned by the authenticated user.
 *     tags: [Reports]
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: path
 *         name: id
 *         required: true
 *         schema:
 *           type: string
 *           format: uuid
 *         description: Incident report ID
 *         example: "a89d7b42-1c23-4567-89ab-cdef01234567"
 *     requestBody:
 *       required: true
 *       content:
 *         application/json:
 *           schema:
 *             $ref: '#/components/schemas/UpdateReportRequest'
 *     responses:
 *       200:
 *         description: Incident report updated successfully
 *         content:
 *           application/json:
 *             schema:
 *               allOf:
 *                 - $ref: '#/components/schemas/SuccessResponse'
 *                 - properties:
 *                     data:
 *                       $ref: '#/components/schemas/IncidentReport'
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
 *         description: Validation failed on update fields
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
 *   delete:
 *     summary: Delete an incident report
 *     description: Deletes an incident report owned by the authenticated user and cascades to all its uploaded media.
 *     tags: [Reports]
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: path
 *         name: id
 *         required: true
 *         schema:
 *           type: string
 *           format: uuid
 *         description: Incident report ID
 *         example: "a89d7b42-1c23-4567-89ab-cdef01234567"
 *     responses:
 *       200:
 *         description: Incident report deleted successfully
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
router.get("/:id", reportController.detail);
router.put("/:id", reportController.update);
router.delete("/:id", reportController.remove);

module.exports = router;
