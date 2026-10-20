// Emergency contact service.
//
// Business logic for the Emergency Contact module. Controllers call into this
// layer; this layer never touches req/res. Mirrors the auth service pattern,
// including a ServiceError carrying an HTTP status and optional field errors.

const prisma = require("../config/prisma");

/**
 * Error thrown by the service layer with an HTTP-ish status code and optional
 * field-level details. Controllers map this to a standardized JSON response.
 */
class ServiceError extends Error {
  constructor(message, statusCode = 400, errors = null) {
    super(message);
    this.name = "ServiceError";
    this.statusCode = statusCode;
    this.errors = errors;
  }
}

const MAX_CONTACTS_PER_USER = 5;

const CONTACT_FIELDS = {
  id: true,
  userId: true,
  contactName: true,
  phoneNumber: true,
  priority: true,
  createdAt: true,
};

/**
 * Create an emergency contact for the authenticated user.
 *
 * Enforces: at most 5 contacts per user, priority unique per user, and phone
 * number unique among the user's own contacts.
 *
 * @param {string} userId
 * @param {{ contactName: string, phoneNumber: string, priority: number }} data
 * @returns {Promise<object>} The created contact.
 */
async function createEmergencyContact(userId, data) {
  const count = await prisma.emergencyContact.count({ where: { userId } });
  if (count >= MAX_CONTACTS_PER_USER) {
    throw new ServiceError(
      `You can have at most ${MAX_CONTACTS_PER_USER} emergency contacts.`,
      400
    );
  }

  // Priority must be unique within the user's contacts.
  const priorityTaken = await prisma.emergencyContact.findFirst({
    where: { userId, priority: data.priority },
    select: { id: true },
  });
  if (priorityTaken) {
    throw new ServiceError("Priority is already in use.", 409, {
      priority: "Priority is already in use.",
    });
  }

  // Phone number must be unique among the user's own contacts.
  const phoneTaken = await prisma.emergencyContact.findFirst({
    where: { userId, phoneNumber: data.phoneNumber },
    select: { id: true },
  });
  if (phoneTaken) {
    throw new ServiceError("Phone number is already in use.", 409, {
      phoneNumber: "Phone number is already in use.",
    });
  }

  const contact = await prisma.emergencyContact.create({
    data: {
      userId,
      contactName: data.contactName,
      phoneNumber: data.phoneNumber,
      priority: data.priority,
    },
    select: CONTACT_FIELDS,
  });

  return contact;
}

/**
 * List the authenticated user's emergency contacts, ordered by priority ASC.
 * @param {string} userId
 * @returns {Promise<object[]>}
 */
async function getEmergencyContacts(userId) {
  return prisma.emergencyContact.findMany({
    where: { userId },
    orderBy: { priority: "asc" },
    select: CONTACT_FIELDS,
  });
}

/**
 * Update one of the authenticated user's emergency contacts.
 *
 * Returns 404 if the contact does not exist OR belongs to another user (so we
 * never reveal the existence of other users' contacts). Keeps priority and
 * phone number unique within the user's own contacts.
 *
 * @param {string} userId
 * @param {string} contactId
 * @param {object} updates - Sanitized subset of { contactName, phoneNumber, priority }.
 * @returns {Promise<object>} The updated contact.
 */
async function updateEmergencyContact(userId, contactId, updates) {
  const existing = await prisma.emergencyContact.findUnique({
    where: { id: contactId },
    select: { id: true, userId: true },
  });

  // Not found, or owned by someone else → 404 either way.
  if (!existing || existing.userId !== userId) {
    throw new ServiceError("Emergency contact not found.", 404);
  }

  // Priority uniqueness (excluding this contact).
  if (updates.priority !== undefined) {
    const clash = await prisma.emergencyContact.findFirst({
      where: {
        userId,
        priority: updates.priority,
        NOT: { id: contactId },
      },
      select: { id: true },
    });
    if (clash) {
      throw new ServiceError("Priority is already in use.", 409, {
        priority: "Priority is already in use.",
      });
    }
  }

  // Phone uniqueness within the user's contacts (excluding this contact).
  if (updates.phoneNumber !== undefined) {
    const clash = await prisma.emergencyContact.findFirst({
      where: {
        userId,
        phoneNumber: updates.phoneNumber,
        NOT: { id: contactId },
      },
      select: { id: true },
    });
    if (clash) {
      throw new ServiceError("Phone number is already in use.", 409, {
        phoneNumber: "Phone number is already in use.",
      });
    }
  }

  const contact = await prisma.emergencyContact.update({
    where: { id: contactId },
    data: updates,
    select: CONTACT_FIELDS,
  });

  return contact;
}

/**
 * Delete one of the authenticated user's emergency contacts.
 *
 * Returns 404 if the contact does not exist OR belongs to another user.
 * @param {string} userId
 * @param {string} contactId
 * @returns {Promise<void>}
 */
async function deleteEmergencyContact(userId, contactId) {
  const existing = await prisma.emergencyContact.findUnique({
    where: { id: contactId },
    select: { id: true, userId: true },
  });

  if (!existing || existing.userId !== userId) {
    throw new ServiceError("Emergency contact not found.", 404);
  }

  await prisma.emergencyContact.delete({ where: { id: contactId } });
}

module.exports = {
  ServiceError,
  createEmergencyContact,
  getEmergencyContacts,
  updateEmergencyContact,
  deleteEmergencyContact,
};
