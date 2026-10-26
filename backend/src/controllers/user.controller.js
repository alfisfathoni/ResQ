// User utility controller.
//
// Thin HTTP layer: validates input, delegates to the service, and shapes the
// response. No business logic here. All handlers run behind authenticate, so
// req.user is always populated. Handlers are wrapped with asyncHandler, so
// thrown ServiceErrors (and any unexpected error) propagate to the global
// error handler.

const { sendSuccess, sendPaginated, sendError } = require("../utils/response");
const { asyncHandler } = require("../utils/async-handler");
const { parsePagination, buildPaginationMeta } = require("../utils/pagination");
const userService = require("../services/user.service");
const { validateUserSearch } = require("../validations/user.validation");

/**
 * GET /api/users/search?q=<keyword>
 */
const search = asyncHandler(async (req, res) => {
  const { valid, errors, data } = validateUserSearch(req.query);
  if (!valid) {
    return sendError(res, {
      statusCode: 422,
      message: "Validation failed.",
      errors,
    });
  }

  const pagination = parsePagination(req.query);
  const { users, total } = await userService.searchUsers(
    req.user.id,
    data.q,
    pagination
  );
  const meta = buildPaginationMeta(pagination.page, pagination.limit, total);

  return sendPaginated(res, {
    statusCode: 200,
    message: "Users retrieved successfully.",
    data: users,
    meta,
  });
});

/**
 * GET /api/users/:id
 */
const detail = asyncHandler(async (req, res) => {
  const user = await userService.getUserById(req.user.id, req.params.id);

  return sendSuccess(res, {
    statusCode: 200,
    message: "User retrieved successfully.",
    data: user,
  });
});

/**
 * GET /api/users/:id/friends
 */
const friends = asyncHandler(async (req, res) => {
  const pagination = parsePagination(req.query);
  const { friends: friendList, total } = await userService.getUserFriends(
    req.params.id,
    pagination
  );
  const meta = buildPaginationMeta(pagination.page, pagination.limit, total);

  return sendPaginated(res, {
    statusCode: 200,
    message: "User friends retrieved successfully.",
    data: friendList,
    meta,
  });
});

module.exports = {
  search,
  detail,
  friends,
};
