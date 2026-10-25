// Friendship controller.
//
// Thin HTTP layer: validates input, delegates to the service, and shapes the
// response. No business logic here. All handlers run behind authenticate, so
// req.user is always populated. Handlers are wrapped with asyncHandler, so
// thrown ServiceErrors (and any unexpected error) propagate to the global
// error handler.

const { sendSuccess, sendError } = require("../utils/response");
const { asyncHandler } = require("../utils/async-handler");
const friendshipService = require("../services/friendship.service");
const {
  validateCreateFriendRequest,
} = require("../validations/friendship.validation");

/**
 * POST /api/friendships/request
 */
const sendRequest = asyncHandler(async (req, res) => {
  const { valid, errors, data } = validateCreateFriendRequest(req.body);
  if (!valid) {
    return sendError(res, {
      statusCode: 422,
      message: "Validation failed.",
      errors,
    });
  }

  const friendship = await friendshipService.sendRequest(
    req.user.id,
    data.addresseeId
  );

  return sendSuccess(res, {
    statusCode: 201,
    message: "Friend request sent successfully.",
    data: friendship,
  });
});

/**
 * GET /api/friendships
 */
const listFriends = asyncHandler(async (req, res) => {
  const friends = await friendshipService.getFriends(req.user.id);

  return sendSuccess(res, {
    statusCode: 200,
    message: "Friends retrieved successfully.",
    data: friends,
  });
});

/**
 * GET /api/friendships/requests
 */
const listIncomingRequests = asyncHandler(async (req, res) => {
  const requests = await friendshipService.getIncomingRequests(req.user.id);

  return sendSuccess(res, {
    statusCode: 200,
    message: "Incoming friend requests retrieved successfully.",
    data: requests,
  });
});

/**
 * PATCH /api/friendships/:id/accept
 */
const accept = asyncHandler(async (req, res) => {
  const friendship = await friendshipService.acceptRequest(
    req.user.id,
    req.params.id
  );

  return sendSuccess(res, {
    statusCode: 200,
    message: "Friend request accepted.",
    data: friendship,
  });
});

/**
 * PATCH /api/friendships/:id/reject
 */
const reject = asyncHandler(async (req, res) => {
  const friendship = await friendshipService.rejectRequest(
    req.user.id,
    req.params.id
  );

  return sendSuccess(res, {
    statusCode: 200,
    message: "Friend request rejected.",
    data: friendship,
  });
});

/**
 * DELETE /api/friendships/:id
 */
const remove = asyncHandler(async (req, res) => {
  await friendshipService.deleteFriendship(req.user.id, req.params.id);

  return sendSuccess(res, {
    statusCode: 200,
    message: "Friendship removed successfully.",
  });
});

module.exports = {
  sendRequest,
  listFriends,
  listIncomingRequests,
  accept,
  reject,
  remove,
};
