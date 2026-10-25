// asyncHandler.
//
// Wraps an async Express handler so that any rejected promise (including a
// thrown ServiceError) is forwarded to Express's error pipeline via next(),
// where the global error handler turns it into a standardized response. This
// removes the repetitive try/catch that previously wrapped every controller
// handler, without changing any behavior.
//
// Explicit validation logic inside controllers is unaffected: handlers still
// validate and return a 422 directly; only error propagation is centralized.

/**
 * @param {import("express").RequestHandler} fn - An async route handler.
 * @returns {import("express").RequestHandler}
 */
function asyncHandler(fn) {
  return function wrapped(req, res, next) {
    return Promise.resolve(fn(req, res, next)).catch(next);
  };
}

module.exports = { asyncHandler };
