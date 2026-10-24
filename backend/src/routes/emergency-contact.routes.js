// Emergency contact routes.
//
// All endpoints require authentication via the existing authenticate
// middleware. Mounted at /api/emergency-contacts in app.js.

const express = require("express");
const emergencyContactController = require("../controllers/emergency-contact.controller");
const { authenticate } = require("../middleware/auth.middleware");

const router = express.Router();

/**
 * @swagger
 * tags:
 *   name: Emergency Contacts
 *   description: Management of personal emergency contacts and priority levels
 */

/**
 * @swagger
 * components:
 *   schemas:
 *     EmergencyContact:
 *       type: object
 *       properties:
 *         id:
 *           type: string
 *           format: uuid
 *           example: "4c7a6e38-958b-4a55-89b1-b92476b7db01"
 *         userId:
 *           type: string
 *           format: uuid
 *           example: "d3b07384-d113-4944-9c8e-aa67c09e3e76"
 *         contactName:
 *           type: string
 *           example: "Jane Doe"
 *         phoneNumber:
 *           type: string
 *           example: "+12345678901"
 *         priority:
 *           type: integer
 *           minimum: 1
 *           maximum: 5
 *           example: 1
 *         createdAt:
 *           type: string
 *           format: date-time
 *           example: "2026-10-24T00:00:00.000Z"
 *     CreateEmergencyContactRequest:
 *       type: object
 *       required:
 *         - contactName
 *         - phoneNumber
 *         - priority
 *       properties:
 *         contactName:
 *           type: string
 *           maxLength: 255
 *           example: "Jane Doe"
 *         phoneNumber:
 *           type: string
 *           pattern: "^\\+?\\d{7,15}$"
 *           example: "+12345678901"
 *         priority:
 *           type: integer
 *           minimum: 1
 *           maximum: 5
 *           description: "Priority level from 1 (highest) to 5 (lowest). Must be unique per user."
 *           example: 1
 *     UpdateEmergencyContactRequest:
 *       type: object
 *       description: "Provide at least one field to update"
 *       properties:
 *         contactName:
 *           type: string
 *           maxLength: 255
 *           example: "Jane Doe (Home)"
 *         phoneNumber:
 *           type: string
 *           pattern: "^\\+?\\d{7,15}$"
 *           example: "+10987654321"
 *         priority:
 *           type: integer
 *           minimum: 1
 *           maximum: 5
 *           example: 2
 */

router.use(authenticate);

/**
 * @swagger
 * /api/emergency-contacts:
 *   post:
 *     summary: Create an emergency contact
 *     description: Adds a new emergency contact for the authenticated user. Max 5 contacts per user.
 *     tags: [Emergency Contacts]
 *     security:
 *       - bearerAuth: []
 *     requestBody:
 *       required: true
 *       content:
 *         application/json:
 *           schema:
 *             $ref: '#/components/schemas/CreateEmergencyContactRequest'
 *     responses:
 *       200:
 *         description: Emergency contact created successfully (alternative success code)
 *         content:
 *           application/json:
 *             schema:
 *               allOf:
 *                 - $ref: '#/components/schemas/SuccessResponse'
 *                 - properties:
 *                     data:
 *                       $ref: '#/components/schemas/EmergencyContact'
 *       201:
 *         description: Emergency contact created successfully
 *         content:
 *           application/json:
 *             schema:
 *               allOf:
 *                 - $ref: '#/components/schemas/SuccessResponse'
 *                 - properties:
 *                     data:
 *                       $ref: '#/components/schemas/EmergencyContact'
 *       400:
 *         description: Bad request (e.g. max limit of 5 contacts reached)
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
 *       409:
 *         description: Priority or phone number already in use among user's contacts
 *         content:
 *           application/json:
 *             schema:
 *               $ref: '#/components/schemas/ErrorResponse'
 *       422:
 *         description: Validation failed on input fields
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
 *     summary: List all emergency contacts
 *     description: Retrieves the authenticated user's emergency contacts ordered by priority ASC.
 *     tags: [Emergency Contacts]
 *     security:
 *       - bearerAuth: []
 *     responses:
 *       200:
 *         description: Emergency contacts retrieved successfully
 *         content:
 *           application/json:
 *             schema:
 *               allOf:
 *                 - $ref: '#/components/schemas/SuccessResponse'
 *                 - properties:
 *                     data:
 *                       type: array
 *                       items:
 *                         $ref: '#/components/schemas/EmergencyContact'
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
router.post("/", emergencyContactController.create);
router.get("/", emergencyContactController.list);

/**
 * @swagger
 * /api/emergency-contacts/{id}:
 *   put:
 *     summary: Update an emergency contact
 *     description: Partially updates an existing emergency contact owned by the authenticated user.
 *     tags: [Emergency Contacts]
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: path
 *         name: id
 *         required: true
 *         schema:
 *           type: string
 *           format: uuid
 *         description: Emergency contact ID
 *         example: "4c7a6e38-958b-4a55-89b1-b92476b7db01"
 *     requestBody:
 *       required: true
 *       content:
 *         application/json:
 *           schema:
 *             $ref: '#/components/schemas/UpdateEmergencyContactRequest'
 *     responses:
 *       200:
 *         description: Emergency contact updated successfully
 *         content:
 *           application/json:
 *             schema:
 *               allOf:
 *                 - $ref: '#/components/schemas/SuccessResponse'
 *                 - properties:
 *                     data:
 *                       $ref: '#/components/schemas/EmergencyContact'
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
 *         description: Emergency contact not found or not owned by user
 *         content:
 *           application/json:
 *             schema:
 *               $ref: '#/components/schemas/ErrorResponse'
 *       409:
 *         description: Priority or phone number already in use by another contact
 *         content:
 *           application/json:
 *             schema:
 *               $ref: '#/components/schemas/ErrorResponse'
 *       422:
 *         description: Validation failed on input fields
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
 *     summary: Delete an emergency contact
 *     description: Removes an emergency contact owned by the authenticated user.
 *     tags: [Emergency Contacts]
 *     security:
 *       - bearerAuth: []
 *     parameters:
 *       - in: path
 *         name: id
 *         required: true
 *         schema:
 *           type: string
 *           format: uuid
 *         description: Emergency contact ID
 *         example: "4c7a6e38-958b-4a55-89b1-b92476b7db01"
 *     responses:
 *       200:
 *         description: Emergency contact deleted successfully
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
 *         description: Emergency contact not found or not owned by user
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
router.put("/:id", emergencyContactController.update);
router.delete("/:id", emergencyContactController.remove);

module.exports = router;
