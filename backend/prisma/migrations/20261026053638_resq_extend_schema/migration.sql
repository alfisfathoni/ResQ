-- CreateEnum
CREATE TYPE "report_status" AS ENUM ('SUBMITTED', 'IN_REVIEW', 'FORWARDED', 'RESOLVED', 'REJECTED');

-- CreateEnum
CREATE TYPE "sos_category" AS ENUM ('POLICE', 'MEDICAL', 'FIRE');

-- CreateEnum
CREATE TYPE "sos_status" AS ENUM ('ACTIVE', 'RESOLVED', 'CANCELLED');

-- CreateEnum
CREATE TYPE "facility_type" AS ENUM ('POLICE', 'HOSPITAL', 'CLINIC', 'FIRE_STATION', 'BUS_STOP', 'TRAIN_STATION');

-- AlterEnum
ALTER TYPE "guide_category" ADD VALUE 'LEGAL';

-- AlterEnum
-- This migration adds more than one value to an enum.
-- With PostgreSQL versions 11 and earlier, this is not possible
-- in a single migration. This can be worked around by creating
-- multiple migrations, each migration adding only one value to
-- the enum.


ALTER TYPE "incident_category" ADD VALUE 'HARASSMENT';
ALTER TYPE "incident_category" ADD VALUE 'PUBLIC_FACILITY';

-- AlterEnum
-- This migration adds more than one value to an enum.
-- With PostgreSQL versions 11 and earlier, this is not possible
-- in a single migration. This can be worked around by creating
-- multiple migrations, each migration adding only one value to
-- the enum.


ALTER TYPE "notification_type" ADD VALUE 'SOS_ALERT';
ALTER TYPE "notification_type" ADD VALUE 'SHARE_ARRIVED';

-- AlterTable
ALTER TABLE "incident_report" ADD COLUMN     "is_anonymous" BOOLEAN NOT NULL DEFAULT false,
ADD COLUMN     "status" "report_status" NOT NULL DEFAULT 'SUBMITTED',
ADD COLUMN     "status_note" TEXT;

-- AlterTable
ALTER TABLE "sharing_session" ADD COLUMN     "destination_latitude" DECIMAL(10,7),
ADD COLUMN     "destination_longitude" DECIMAL(10,7),
ADD COLUMN     "destination_name" VARCHAR(255),
ADD COLUMN     "expires_at" TIMESTAMP(3);

-- AlterTable
ALTER TABLE "user" ADD COLUMN     "location_updated_at" TIMESTAMP(3);

-- CreateTable
CREATE TABLE "sos_alert" (
    "id" TEXT NOT NULL,
    "user_id" TEXT NOT NULL,
    "category" "sos_category" NOT NULL,
    "status" "sos_status" NOT NULL DEFAULT 'ACTIVE',
    "latitude" DECIMAL(10,7) NOT NULL,
    "longitude" DECIMAL(10,7) NOT NULL,
    "message" TEXT,
    "created_at" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "resolved_at" TIMESTAMP(3),

    CONSTRAINT "sos_alert_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "facility" (
    "id" TEXT NOT NULL,
    "name" VARCHAR(255) NOT NULL,
    "type" "facility_type" NOT NULL,
    "address" TEXT,
    "phone" VARCHAR(30),
    "latitude" DECIMAL(10,7) NOT NULL,
    "longitude" DECIMAL(10,7) NOT NULL,
    "created_at" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updated_at" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "facility_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "report_like" (
    "id" TEXT NOT NULL,
    "report_id" TEXT NOT NULL,
    "user_id" TEXT NOT NULL,
    "created_at" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "report_like_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "report_comment" (
    "id" TEXT NOT NULL,
    "report_id" TEXT NOT NULL,
    "user_id" TEXT NOT NULL,
    "content" VARCHAR(500) NOT NULL,
    "created_at" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "report_comment_pkey" PRIMARY KEY ("id")
);

-- CreateIndex
CREATE INDEX "sos_alert_user_id_idx" ON "sos_alert"("user_id");

-- CreateIndex
CREATE INDEX "sos_alert_status_created_at_idx" ON "sos_alert"("status", "created_at");

-- CreateIndex
CREATE INDEX "facility_type_idx" ON "facility"("type");

-- CreateIndex
CREATE INDEX "facility_latitude_longitude_idx" ON "facility"("latitude", "longitude");

-- CreateIndex
CREATE INDEX "report_like_user_id_idx" ON "report_like"("user_id");

-- CreateIndex
CREATE UNIQUE INDEX "report_like_report_id_user_id_key" ON "report_like"("report_id", "user_id");

-- CreateIndex
CREATE INDEX "report_comment_report_id_created_at_idx" ON "report_comment"("report_id", "created_at");

-- CreateIndex
CREATE INDEX "incident_report_created_at_idx" ON "incident_report"("created_at");

-- CreateIndex
CREATE INDEX "incident_report_category_idx" ON "incident_report"("category");

-- CreateIndex
CREATE INDEX "incident_report_status_idx" ON "incident_report"("status");

-- CreateIndex
CREATE INDEX "incident_report_latitude_longitude_idx" ON "incident_report"("latitude", "longitude");

-- AddForeignKey
ALTER TABLE "sos_alert" ADD CONSTRAINT "sos_alert_user_id_fkey" FOREIGN KEY ("user_id") REFERENCES "user"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "report_like" ADD CONSTRAINT "report_like_report_id_fkey" FOREIGN KEY ("report_id") REFERENCES "incident_report"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "report_like" ADD CONSTRAINT "report_like_user_id_fkey" FOREIGN KEY ("user_id") REFERENCES "user"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "report_comment" ADD CONSTRAINT "report_comment_report_id_fkey" FOREIGN KEY ("report_id") REFERENCES "incident_report"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "report_comment" ADD CONSTRAINT "report_comment_user_id_fkey" FOREIGN KEY ("user_id") REFERENCES "user"("id") ON DELETE CASCADE ON UPDATE CASCADE;
