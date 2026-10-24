// Cloudinary utility.
//
// Configures the Cloudinary SDK from the existing environment variables and
// exposes thin helpers for uploading an in-memory buffer and deleting an
// asset. Configuration happens once at module load.
//
// Environment variables used:
//   CLOUDINARY_CLOUD_NAME
//   CLOUDINARY_API_KEY
//   CLOUDINARY_API_SECRET

const { v2: cloudinary } = require("cloudinary");

cloudinary.config({
  cloud_name: process.env.CLOUDINARY_CLOUD_NAME,
  api_key: process.env.CLOUDINARY_API_KEY,
  api_secret: process.env.CLOUDINARY_API_SECRET,
});

// Folder in Cloudinary where report media is stored.
const REPORT_MEDIA_FOLDER = process.env.CLOUDINARY_REPORT_FOLDER || "resq/reports";

/**
 * Upload an in-memory file buffer to Cloudinary.
 * @param {Buffer} buffer - The file contents (from multer memoryStorage).
 * @param {object} options
 * @param {"image"|"video"} options.resourceType - Cloudinary resource type.
 * @param {string} [options.folder] - Target folder.
 * @returns {Promise<{ secure_url: string, public_id: string, resource_type: string }>}
 */
function uploadBuffer(buffer, { resourceType, folder = REPORT_MEDIA_FOLDER }) {
  return new Promise((resolve, reject) => {
    const stream = cloudinary.uploader.upload_stream(
      { resource_type: resourceType, folder },
      (error, result) => {
        if (error) return reject(error);
        resolve(result);
      }
    );
    stream.end(buffer);
  });
}

/**
 * Delete an asset from Cloudinary.
 * @param {string} publicId - The asset's public_id.
 * @param {"image"|"video"} resourceType - Must match the asset's type.
 * @returns {Promise<object>}
 */
function deleteAsset(publicId, resourceType) {
  return cloudinary.uploader.destroy(publicId, { resource_type: resourceType });
}

/**
 * Derive a Cloudinary public_id from a stored secure URL.
 *
 * The ReportMedia schema has no public_id column, so we recover it from the
 * URL at delete time. A Cloudinary delivery URL looks like:
 *   https://res.cloudinary.com/<cloud>/<resource_type>/upload/v123/<folder>/<name>.<ext>
 * The public_id is everything after the version segment, without the file
 * extension (e.g. "resq/reports/abc123").
 *
 * @param {string} url
 * @returns {string|null} The public_id, or null if it can't be parsed.
 */
function publicIdFromUrl(url) {
  if (typeof url !== "string") return null;
  const match = url.match(/\/upload\/(?:v\d+\/)?(.+)$/);
  if (!match) return null;
  let path = match[1];
  // Strip a trailing file extension from the last segment only.
  path = path.replace(/\.[^/.]+$/, "");
  return path || null;
}

module.exports = {
  cloudinary,
  uploadBuffer,
  deleteAsset,
  publicIdFromUrl,
  REPORT_MEDIA_FOLDER,
};

