// User utility routes.
//
// All endpoints require authentication via the existing authenticate
// middleware. Mounted at /api/users in app.js.
//
// The literal /search path is declared before the /:id routes so Express
// does not capture "search" as an :id value.

const express = require("express");
const userController = require("../controllers/user.controller");
const { authenticate } = require("../middleware/auth.middleware");

const router = express.Router();

router.use(authenticate);

router.get("/search", userController.search);
router.get("/:id", userController.detail);
router.get("/:id/friends", userController.friends);

module.exports = router;
