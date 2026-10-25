// Shared ServiceError.
//
// A single error type used by the service layer across all modules. Services
// throw `new ServiceError(message, statusCode, errors)` for expected failures
// (e.g. 404 not found, 409 conflict, 403 forbidden); the global error handler
// (and, for the auth module, its controller) maps it to a standardized JSON
// response via the response helper.
//
// Previously every service declared its own identical ServiceError class. This
// module replaces that duplication with one shared definition. Each service
// re-exports it, so existing imports like `authService.ServiceError` and the
// `instanceof` checks that depend on them keep working unchanged.

/**
 * Error carrying an HTTP-ish status code and optional field-level details.
 */
class ServiceError extends Error {
  constructor(message, statusCode = 400, errors = null) {
    super(message);
    this.name = "ServiceError";
    this.statusCode = statusCode;
    this.errors = errors;
  }
}

module.exports = { ServiceError };
