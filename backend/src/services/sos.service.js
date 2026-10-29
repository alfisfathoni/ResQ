// SOS service.
//
// Business logic for the SOS module. Controllers call into this layer; this
// layer never touches req/res. Mirrors the other services' pattern, including
// the shared ServiceError.
//
// Triggering an SOS is idempotent per user: while the user already has an
// ACTIVE alert, calling it again updates that alert instead of creating a
// second one. A brand-new alert notifies the user's accepted friends and any
// registered user whose phone number matches one of the user's emergency
// contacts. Notifications are best-effort: a failure there is logged and
// swallowed so the SOS itself always succeeds.

const prisma = require("../config/prisma");
const { ServiceError } = require("../utils/service-error");
const notificationService = require("./notification.service");
const { normalizePhone, phoneVariants } = require("../utils/phone");

// Fields returned for an alert. userId is deliberately left out: the caller
// is always the owner.
const ALERT_FIELDS = {
  id: true,
  category: true,
  status: true,
  latitude: true,
  longitude: true,
  message: true,
  createdAt: true,
  resolvedAt: true,
};

// Human-readable (Indonesian) label per category, used in notifications and
// in the shareText returned to the client.
const CATEGORY_LABELS = {
  POLICE: "Kejahatan/Pelecehan",
  MEDICAL: "Medis",
  FIRE: "Kebakaran",
};

/**
 * Convert an alert's Decimal latitude/longitude to plain numbers so the JSON
 * response contains numeric coordinates rather than Decimal strings.
 * @param {object|null} alert
 * @returns {object|null}
 */
function serializeAlert(alert) {
  if (!alert) return alert;
  return {
    ...alert,
    latitude: alert.latitude === null ? null : Number(alert.latitude),
    longitude: alert.longitude === null ? null : Number(alert.longitude),
  };
}

/**
 * Format a coordinate for a URL: at most 7 decimals, no trailing zeros and no
 * exponent notation (which Number#toString can produce for tiny values).
 * @param {number} value
 * @returns {string}
 */
function formatCoordinate(value) {
  return value
    .toFixed(7)
    .replace(/(\.\d*?)0+$/, "$1")
    .replace(/\.$/, "");
}

/**
 * Build the Google Maps link for a position.
 * @param {number} latitude
 * @param {number} longitude
 * @returns {string}
 */
function buildMapUrl(latitude, longitude) {
  return `https://maps.google.com/?q=${formatCoordinate(latitude)},${formatCoordinate(longitude)}`;
}

/**
 * Build the ready-to-send text the client shares by SMS/WhatsApp.
 * @param {object} params
 * @param {string} params.senderName
 * @param {string} params.category - SosCategory value.
 * @param {number} params.latitude
 * @param {number} params.longitude
 * @param {string|null} [params.message]
 * @returns {string}
 */
function buildShareText({ senderName, category, latitude, longitude, message }) {
  const label = CATEGORY_LABELS[category] || category;
  let text =
    `SOS ResQ: ${senderName} membutuhkan bantuan (${label}). ` +
    `Lokasi: ${buildMapUrl(latitude, longitude)}`;
  if (message) {
    text += ` Pesan: ${message}`;
  }
  return text;
}

/**
 * Return the user's emergency contacts, lowest priority number first.
 * @param {string} userId
 * @returns {Promise<Array<{ contactName: string, phoneNumber: string, priority: number }>>}
 */
function getEmergencyContactsOf(userId) {
  return prisma.emergencyContact.findMany({
    where: { userId },
    orderBy: { priority: "asc" },
    select: { contactName: true, phoneNumber: true, priority: true },
  });
}

/**
 * Work out who must be notified about a user's SOS.
 *
 * Recipients are the user's ACCEPTED friends (either friendship direction)
 * plus registered users whose phone number matches an emergency contact of the
 * user and who are verified (isVerified = true) accounts with role USER.
 * Contact numbers are normalized and expanded to every stored variant, then
 * matched with a single `IN` query. The result is de-duplicated and never
 * contains the sender.
 *
 * @param {string} userId - The sender.
 * @param {Array<{ phoneNumber: string }>} contacts - The sender's emergency contacts.
 * @returns {Promise<string[]>} Distinct recipient user ids.
 */
async function findRecipientIds(userId, contacts) {
  const recipients = new Set();

  const friendships = await prisma.friendship.findMany({
    where: {
      status: "ACCEPTED",
      OR: [{ requesterId: userId }, { addresseeId: userId }],
    },
    select: { requesterId: true, addresseeId: true },
  });
  for (const f of friendships) {
    recipients.add(f.requesterId === userId ? f.addresseeId : f.requesterId);
  }

  const phones = new Set();
  for (const contact of contacts) {
    const normalized = normalizePhone(contact.phoneNumber);
    if (normalized) {
      phoneVariants(normalized).forEach((variant) => phones.add(variant));
    }
  }
  if (phones.size > 0) {
    // Phone numbers are not verified (no OTP), so anyone could register with
    // someone else's number. Only verified, regular (USER) accounts may
    // receive a location through a number match. Accepted friends (above) are
    // unaffected: they were explicitly approved by the sender.
    const matchedUsers = await prisma.user.findMany({
      where: {
        phoneNumber: { in: [...phones] },
        isVerified: true,
        role: "USER",
      },
      select: { id: true },
    });
    matchedUsers.forEach((u) => recipients.add(u.id));
  }

  recipients.delete(userId);
  return [...recipients];
}

/**
 * Notify friends and matching emergency-contact users about an SOS.
 * Best-effort: never throws.
 *
 * @param {object} params
 * @param {string} params.userId - The sender.
 * @param {string} params.senderName
 * @param {object} params.alert - Serialized alert (numeric coordinates).
 * @param {Array<{ phoneNumber: string }>} params.contacts
 * @returns {Promise<void>}
 */
async function notifyRecipients({ userId, senderName, alert, contacts }) {
  try {
    const recipientIds = await findRecipientIds(userId, contacts);
    if (recipientIds.length === 0) return;

    const label = CATEGORY_LABELS[alert.category] || alert.category;
    let message =
      `${senderName} membutuhkan bantuan (${label}). ` +
      `Lokasi: ${buildMapUrl(alert.latitude, alert.longitude)}`;
    if (alert.message) {
      message += ` Pesan: ${alert.message}`;
    }

    await notificationService.createNotifications(
      recipientIds.map((recipientId) => ({
        userId: recipientId,
        senderId: userId,
        type: "SOS_ALERT",
        title: `Peringatan SOS: ${label}`,
        message,
      }))
    );
  } catch (error) {
    // Never let a notification problem break the SOS itself.
    console.error("Failed to notify SOS recipients:", error.message);
  }
}

/**
 * Trigger an SOS for the authenticated user (idempotent).
 *
 * - No ACTIVE alert: create one (created = true) and notify recipients.
 * - ACTIVE alert exists: update its position (and message / category when
 *   sent) and return it (created = false). Recipients are notified again only
 *   when the category changed, because that changes what kind of help is
 *   needed; plain location refreshes do not re-notify.
 *
 * Concurrency: the schema has no unique constraint on "one ACTIVE alert per
 * user", so the check-then-create runs inside a transaction that first takes a
 * per-user PostgreSQL advisory lock. Two near-simultaneous requests from the
 * same user are therefore serialized: the second sees the first one's alert
 * and updates it instead of creating a duplicate.
 *
 * @param {string} userId
 * @param {{ category: string, latitude: number, longitude: number, message?: string|null }} data
 * @returns {Promise<{ created: boolean, alert: object, emergencyContacts: object[], shareText: string }>}
 */
async function triggerSos(userId, data) {
  const user = await prisma.user.findUnique({
    where: { id: userId },
    select: { fullName: true },
  });
  if (!user) {
    throw new ServiceError("User not found.", 404);
  }

  const { created, categoryChanged, alert } = await prisma.$transaction(async (tx) => {
    // Serialize concurrent triggers of the same user (released at commit/rollback).
    await tx.$executeRaw`SELECT pg_advisory_xact_lock(hashtext(${`sos:${userId}`}::text))`;

    const existing = await tx.sosAlert.findFirst({
      where: { userId, status: "ACTIVE" },
      orderBy: { createdAt: "desc" },
      select: ALERT_FIELDS,
    });

    if (existing) {
      const updated = await tx.sosAlert.update({
        where: { id: existing.id },
        data: {
          category: data.category,
          latitude: data.latitude,
          longitude: data.longitude,
          // Keep the old message unless the client sent the field.
          ...(data.message !== undefined ? { message: data.message } : {}),
        },
        select: ALERT_FIELDS,
      });
      return {
        created: false,
        categoryChanged: existing.category !== data.category,
        alert: updated,
      };
    }

    const createdAlert = await tx.sosAlert.create({
      data: {
        userId,
        category: data.category,
        latitude: data.latitude,
        longitude: data.longitude,
        message: data.message ?? null,
      },
      select: ALERT_FIELDS,
    });
    return { created: true, categoryChanged: false, alert: createdAlert };
  });

  const serialized = serializeAlert(alert);
  const contacts = await getEmergencyContactsOf(userId);

  if (created || categoryChanged) {
    await notifyRecipients({
      userId,
      senderName: user.fullName,
      alert: serialized,
      contacts,
    });
  }

  return {
    created,
    alert: serialized,
    emergencyContacts: contacts,
    shareText: buildShareText({
      senderName: user.fullName,
      category: serialized.category,
      latitude: serialized.latitude,
      longitude: serialized.longitude,
      message: serialized.message,
    }),
  };
}

/**
 * Get the authenticated user's ACTIVE alert.
 * @param {string} userId
 * @returns {Promise<object|null>} The alert, or null when none is active.
 */
async function getActiveSos(userId) {
  const alert = await prisma.sosAlert.findFirst({
    where: { userId, status: "ACTIVE" },
    orderBy: { createdAt: "desc" },
    select: ALERT_FIELDS,
  });
  return serializeAlert(alert);
}

/**
 * List the authenticated user's SOS history, newest first.
 * @param {string} userId
 * @param {object} [pagination={}] - Normalized pagination options { skip, take }.
 * @returns {Promise<{ alerts: object[], total: number }>}
 */
async function getSosHistory(userId, pagination = {}) {
  const { skip, take } = pagination;
  const where = { userId };

  const [alerts, total] = await Promise.all([
    prisma.sosAlert.findMany({
      where,
      orderBy: { createdAt: "desc" },
      select: ALERT_FIELDS,
      ...(typeof skip === "number" ? { skip } : {}),
      ...(typeof take === "number" ? { take } : {}),
    }),
    prisma.sosAlert.count({ where }),
  ]);

  return { alerts: alerts.map(serializeAlert), total };
}

/**
 * Close one of the authenticated user's ACTIVE alerts.
 *
 * Returns 404 when the alert does not exist OR belongs to another user, and
 * 409 when it is no longer ACTIVE. The status change is a conditional update
 * (only while still ACTIVE), so two concurrent resolves cannot both succeed.
 *
 * @param {string} userId
 * @param {string} alertId
 * @param {"RESOLVED"|"CANCELLED"} status
 * @returns {Promise<object>} The updated alert.
 */
async function resolveSos(userId, alertId, status) {
  const existing = await prisma.sosAlert.findUnique({
    where: { id: alertId },
    select: { id: true, userId: true, status: true },
  });

  if (!existing || existing.userId !== userId) {
    throw new ServiceError("SOS alert not found.", 404);
  }
  if (existing.status !== "ACTIVE") {
    throw new ServiceError("SOS alert is no longer active.", 409);
  }

  const result = await prisma.sosAlert.updateMany({
    where: { id: alertId, userId, status: "ACTIVE" },
    data: { status, resolvedAt: new Date() },
  });
  if (result.count === 0) {
    throw new ServiceError("SOS alert is no longer active.", 409);
  }

  const alert = await prisma.sosAlert.findUnique({
    where: { id: alertId },
    select: ALERT_FIELDS,
  });
  return serializeAlert(alert);
}

module.exports = {
  ServiceError,
  triggerSos,
  getActiveSos,
  getSosHistory,
  resolveSos,
};
