// Standardized pagination utilities.
//
// Normalizes query parameters (page, limit) with a hard cap at 50, calculates
// database offsets (skip/take), and constructs standardized metadata objects.

const DEFAULT_PAGE = 1;
const DEFAULT_LIMIT = 10;
const MAX_LIMIT = 50;

/**
 * Normalize and validate pagination query parameters.
 *
 * @param {object} [query={}] - Express req.query object.
 * @param {object} [options={}]
 * @param {number} [options.defaultPage=1]
 * @param {number} [options.defaultLimit=10]
 * @param {number} [options.maxLimit=50]
 * @returns {{ page: number, limit: number, skip: number, take: number }}
 */
function parsePagination(query = {}, {
  defaultPage = DEFAULT_PAGE,
  defaultLimit = DEFAULT_LIMIT,
  maxLimit = MAX_LIMIT,
} = {}) {
  const parsedPage = parseInt(query?.page, 10);
  const parsedLimit = parseInt(query?.limit, 10);

  const page = Number.isInteger(parsedPage) && parsedPage >= 1 ? parsedPage : defaultPage;

  let limit = Number.isInteger(parsedLimit) && parsedLimit >= 1 ? parsedLimit : defaultLimit;
  if (limit > maxLimit) {
    limit = maxLimit;
  }

  const skip = (page - 1) * limit;

  return {
    page,
    limit,
    skip,
    take: limit,
  };
}

/**
 * Build standardized pagination metadata.
 *
 * @param {number} page - Current page number.
 * @param {number} limit - Items per page.
 * @param {number} total - Total number of items across all pages.
 * @returns {{ page: number, limit: number, total: number, totalPages: number }}
 */
function buildPaginationMeta(page, limit, total) {
  const safeTotal = typeof total === "number" && total >= 0 ? total : 0;
  const safeLimit = typeof limit === "number" && limit > 0 ? limit : DEFAULT_LIMIT;
  const safePage = typeof page === "number" && page >= 1 ? page : DEFAULT_PAGE;
  const totalPages = Math.ceil(safeTotal / safeLimit);

  return {
    page: safePage,
    limit: safeLimit,
    total: safeTotal,
    totalPages,
  };
}

module.exports = {
  DEFAULT_PAGE,
  DEFAULT_LIMIT,
  MAX_LIMIT,
  parsePagination,
  buildPaginationMeta,
};
