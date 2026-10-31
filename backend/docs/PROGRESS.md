# ResQ Backend — Status Pengerjaan

> **Terakhir diperbarui:** 2026-10-29 (T6 — R-0.9; sebelumnya T5 — R-0.8 bagian kode; T4 — R-0.7, T3 — R-0.1, R-0.4, R-0.5, R-0.6)
> **Sumber kebenaran:** HANYA kode — seluruh `src/` (app.js, routes, controllers, services, validations, middleware, utils, config), `prisma/schema.prisma`, dan seluruh migrasi Prisma. Dokumentasi lama TIDAK dijadikan acuan.
> **Metode:** Setiap file di `src/` dibaca baris per baris. Jika dokumen lama bertentangan dengan kode, kode yang menang dan dokumen diperbarui mengikuti kode.
> **Aturan:** Setiap kali menyelesaikan atau mengubah fitur, perbarui tabel di bawah ini. Status yang jujur lebih berguna daripada status yang optimis.

## Ringkasan (kondisi backend saat ini)

Backend ResQ kini memiliki **sembilan modul fitur yang berfungsi penuh** end-to-end (route → controller → service → validasi → Prisma):
**Authentication (termasuk User Profile)**, **Emergency Contact**, **Incident Report**, **Report Media**, **Guide**, **Friendship**, **Notification**, **Sharing Session**, dan **User Utility**. Semua model lama sudah memiliki API; SosAlert, Facility, ReportLike, ReportComment sudah ada di skema dan akan mendapat API di tugas roadmap 1.3, 2.1, dan 3.1.

Infrastruktur lintas-sistem & keamanan:
- **Security Middlewares (Turn 2)**:
  - **Helmet**: proteksi HTTP security headers global di `app.js`.
  - **Morgan**: HTTP request logging (`dev` format).
  - **Rate Limiting**: `express-rate-limit` pada endpoint autentikasi publik (maks 20 req / 15 mnt) untuk mencegah brute-force attacks.
- **Standardized Pagination (Turn 2)**:
  - Utility `utils/pagination.js` (`parsePagination`, `buildPaginationMeta`) menormalisasi `page` & `limit` (default: 1 & 10, batas maksimal: 50).
  - Response helper `sendPaginated` di `utils/response.js` mengembalikan struktur `{ status: "success", data: [...], meta: { page, limit, total, totalPages } }`.
  - Terintegrasi di seluruh listing endpoint (reports, guides, notifications, friendships, users search/friends, sharing sessions, emergency contacts, report media).
- **Global error handler** (`middleware/error.middleware.js`) terpasang paling akhir di `app.js`.
- **Shared ServiceError** (`utils/service-error.js`) menggantikan duplikasi per-service.
- **asyncHandler** (`utils/async-handler.js`) membungkus seluruh controller CRUD (auth controller tetap `try/catch` lokal).
- **Role middleware** (`authorize`, `isAdmin`) sudah diimplementasikan dan dipakai di route admin Guide.
- **Notification business integration**: event pertemanan & sharing session membuat notifikasi secara best-effort.

Yang **masih tersisa**: **API Documentation interaktif (Swagger/OpenAPI)** dan **automated testing** belum ada.

Legenda status:
- ✅ **Selesai** — terimplementasi penuh (route, controller, service, validasi) dan diverifikasi dari kode.
- 🟡 **Sebagian** — ada sebagian, masih ada TODO / placeholder.
- ⬜ **Belum** — belum ada implementasi sama sekali.

## Peta Fase

| Fase | Item | Status |
| --- | --- | --- |
| Phase 1 — Item 1 | Guide Module | ✅ |
| Phase 1 — Item 2 | Emergency Contact Module | ✅ |
| Phase 1 — Item 3 | Friendship Module | ✅ |
| Phase 1 — Item 4 | Notification Module | ✅ |
| Phase 1 — Item 5 | Sharing Session Module | ✅ |
| Phase 1 — Item 6 | User Utility Module | ✅ |
| Phase 2 — Item 1 | Notification Business Integration | ✅ |
| Phase 2 — Item 2 | Global Error Handling | ✅ |
| Turn 2 (T2) — 0.2 | Core Security & Logging (Helmet, Morgan, Rate Limit) | ✅ |
| Turn 2 (T2) — 0.3 | Standardized Pagination & Metadata Response | ✅ |
| Turn 4 (T4) — 0.7 | `resend-verification`, halaman reset password, verify-email HTML, flag `AUTO_VERIFY_EMAIL` | ✅ |
| Turn 5 (T5) — 0.8 | Persiapan deploy (bagian kode + panduan): `trust proxy`, redaksi token di log, rate limit dapat diatur, `DEPLOYMENT.md`. **Deploy belum dilakukan.** | ✅ (kode) / ⬜ (deploy) |
| Turn 6 (T6) — 0.9 | Postman collection + environment (`postman/`) dan README akar (`README.md`) | ✅ |
| Phase 2 — Item 3 | API Documentation (Swagger/OpenAPI) | ⬜ |

## Status Per Fitur

| Fitur | Status | Catatan (berdasarkan kode) |
| --- | --- | --- |
| **Authentication** | ✅ | register, login (email/username), refresh, logout, verify-email (HTML untuk browser, JSON untuk API), resend-verification, forgot/reset-password (+ halaman browser `GET /reset-password`). Diproteksi rate limiter (default maks 20 req/15 min; resend-verification 5 req/15 min; refresh 100 req/15 min; semuanya dapat diatur lewat env). Controller auth masih `try/catch` lokal. |
| **User Profile** | ✅ | Bagian modul auth: `GET/PUT/DELETE /api/auth/profile`. Update terbatas `fullName`, `phoneNumber`, `profilePicture`. DELETE menolak ADMIN (403). |
| **Emergency Contact** | ✅ | CRUD penuh. Maks 5/user, `priority` unik 1-5, `phoneNumber` unik per user. Listing mendukung paginasi & meta. |
| **Incident Report** | ✅ | CRUD penuh. `category` enum, koordinat divalidasi & di-serialize ke `Number`. Listing mendukung paginasi & meta. |
| **Report Media** | ✅ | Upload (Multer→Cloudinary), list, delete. Hapus asset best-effort lalu hapus baris DB. Listing mendukung paginasi & meta. |
| **Guide** | ✅ | `GET /` & `GET /:id` publik; `POST`/`PUT`/`DELETE` admin (`authenticate` + `isAdmin`). Kategori enum `GuideCategory`. Listing mendukung paginasi & meta. |
| **Friendship** | ✅ | request, list friends, list incoming requests, accept, reject, remove. Cek dua arah; revive `REJECTED`→`PENDING`. Listing mendukung paginasi & meta. |
| **Notification** | ✅ | list, unread-count, read, read-all, delete. Dibuat via event bisnis (bukan endpoint create langsung). Listing mendukung paginasi & meta. |
| **Sharing Session** | ✅ | start (ke semua teman ACCEPTED), list active, detail, stop. Satu sesi aktif per owner. Listing aktif mendukung paginasi & meta. |
| **User Utility** | ✅ | search (by username/fullName), detail (+`friendshipStatus`), list friends. Hanya field publik. Search & friends list mendukung paginasi & meta. |
| **Notification Integration** | ✅ | friendship (FRIEND_REQUEST, FRIEND_ACCEPTED) & sharing session (SHARE_SESSION) membuat notifikasi best-effort via `notification.service`. |
| **Security & Logging** | ✅ | Helmet secure headers, Morgan HTTP request logging (nilai `?token=` diganti `[REDACTED]`), express-rate-limit pada auth endpoints (batas dapat diatur via env), `trust proxy` via env `TRUST_PROXY`. |
| **Standardized Pagination** | ✅ | Helper `utils/pagination.js` (`parsePagination`, `buildPaginationMeta`) + `sendPaginated` di `utils/response.js`. Capped limit 50. |
| **Global Error Handler** | ✅ | `middleware/error.middleware.js` terdaftar terakhir di `app.js`; map `ServiceError` + 500 generik tanpa bocor stack. |
| **Shared ServiceError** | ✅ | `utils/service-error.js`; tiap service import & re-export. Tidak ada lagi duplikasi kelas. |
| **asyncHandler** | ✅ | `utils/async-handler.js`; dipakai semua controller CRUD. Auth controller pengecualian (try/catch lokal). |
| **Admin / Role Middleware** | ✅ | `role.middleware.js` — `authorize(...roles)` + `isAdmin`. Dipakai di route admin Guide. (Dulu placeholder 501.) |
| **Upload Middleware** | ✅ | multer memory storage, limit 20MB, filter `image/*` & `video/*`. |
| **Auth Middleware** | ✅ | verifikasi Bearer, isi `req.user = { id, role }`, gagal → 401. |
| **Cloudinary** | ✅ | `uploadBuffer`, `deleteAsset`, `publicIdFromUrl`. |
| **JWT** | ✅ | 4 tipe token: access, refresh, email-verification, password-reset. |
| **Mail** | ✅ | Nodemailer (lazy), email verifikasi & reset. Link reset mengarah ke `APP_URL/reset-password?token=`. |
| **Prisma** | ✅ | Singleton client + verifikasi `SELECT 1` saat start. Skema + migrasi `init`. |
| **API Reference (markdown)** | ✅ | `docs/API_REFERENCE.md` mencakup seluruh 9 modul (diverifikasi ulang terhadap route/controller/validasi). |
| **Swagger / OpenAPI** | ⬜ | Belum ada dependency maupun setup di `app.js`. = Phase 2 Item 3. |
| **Testing** | ⬜ | Belum ada framework test, file test, atau script `test` di `package.json`. |
| **Documentation** | ✅ | Folder `docs/` diaudit ulang & disinkronkan dengan kode pada tanggal di atas. |
| **R-1.1 Migrasi skema** | ✅ | Migrasi tunggal `resq_extend_schema`: model baru `SosAlert`, `Facility`, `ReportLike`, `ReportComment`; field baru di `User`, `SharingSession`, `IncidentReport`; enum baru dan nilai enum tambahan. **Model baru belum memiliki API** (tanpa route/controller/service). |

## Catatan Roadmap

- **R-0.1 ✅ Environment dan hapus fallback secret JWT** — `utils/jwt.js` tidak lagi punya nilai default untuk keempat secret (`JWT_SECRET`, `JWT_REFRESH_SECRET`, `JWT_EMAIL_VERIFICATION_SECRET`, `JWT_PASSWORD_RESET_SECRET`) dan melempar error bila kosong. `config/env.js` (`validateEnv()`) dipanggil di awal `server.js`, sebelum `app.js` dimuat: server berhenti (exit 1) dengan pesan yang hanya menyebut **nama** variabel bila `DATABASE_URL` atau salah satu `JWT_*_SECRET` kosong. Berlaku di **semua environment**. Ditambah `.env.example` (hanya nama variabel). `.gitignore` sudah meng-ignore `.env`, `.env.*` (kecuali `.env.example`), dan isi `uploads/` (kecuali `.gitkeep`).
  - **Temuan keamanan "fallback default secret JWT" (risiko pada audit, `AUDIT_REPORT.md`): ✅ sudah diperbaiki.** Tidak ada lagi secret yang jatuh ke nilai default atau saling meminjam secret lain.
- **R-0.4 ✅ Script `package.json`** — `postinstall: prisma generate`; `start` = `prisma migrate deploy && node src/server.js`; `seed` = `node prisma/seed.js` (tanpa blok `prisma.seed`). Paket `prisma` (CLI) dipindah dari `devDependencies` ke `dependencies` agar `generate` dan `migrate deploy` tetap jalan di hosting yang tidak memasang devDependencies.
- **R-0.5 ✅ Seed** — `prisma/seed.js` + `prisma/seed/guides.json`. Idempoten dan hanya menambah (tidak ada operasi hapus). (1) Akun `ADMIN` dari `ADMIN_EMAIL`/`ADMIN_PASSWORD` (`isVerified=true`); bila email sudah ada hanya dipromosikan, password tidak ditimpa kecuali `SEED_RESET_ADMIN_PASSWORD=true`. (2) 13 panduan Bahasa Indonesia mencakup `SAFETY`, `FIRST_AID`, `DISASTER`, `EMERGENCY`, `LEGAL` (2 item `LEGAL`); dibuat hanya bila kombinasi `title` + `category` belum ada. (3) 2 user demo hanya bila `SEED_DEMO_PASSWORD` terisi. Validator panduan kini menerima `LEGAL`. Konten panduan masih perlu diverifikasi manusia sebelum dipakai di produksi.
- **R-0.6 ✅ `role` di respons login dan profil** — `role` (`USER`/`ADMIN`) sudah ada di `POST /api/auth/login` (`data.user`) dan `GET /api/auth/profile`. `toPublicUser()` diubah dari "buang password" menjadi whitelist (`PUBLIC_USER_SELECT`, juga dipakai `getProfile` dan `updateProfile`), sehingga field baru di model `User` tidak bocor tanpa disengaja. Payload JWT dan `auth.middleware.js` tidak diubah.
- **R-0.7 ✅ Resend verifikasi, halaman reset password, flag `AUTO_VERIFY_EMAIL`** —
  - `POST /api/auth/resend-verification` (body `{ email }`): kirim token verifikasi baru hanya bila user ada dan belum terverifikasi; **selalu 200** dengan pesan generik (anti-enumerasi). Limiter ketat baru `emailLimiter` (5 req / 15 menit per IP) di `rate-limit.middleware.js`.
  - `GET /reset-password?token=` (router baru `page.routes.js` → `page.controller.js`, dipasang di root `app.js`): halaman HTML minimal ramah HP yang memanggil `POST /api/auth/reset-password`. Link di `utils/mailer.js` diubah ke path ini.
  - `GET /api/auth/verify-email`: content negotiation (`req.accepts`) — browser dapat halaman HTML, klien API tetap JSON; status code sama.
  - Util baru `utils/html-page.js` (render halaman + escape HTML + header CSP nonce dan `no-store`, karena CSP default helmet memblokir script inline).
  - Flag `AUTO_VERIFY_EMAIL` (default `false`, ditambahkan ke `.env.example`): bila `true` akun baru langsung `isVerified=true` dan email verifikasi tidak dikirim. Hanya untuk dev/demo.
- **R-0.8 ✅ (bagian kode) Persiapan deploy: trust proxy, redaksi log, rate limit dapat diatur, panduan deploy** — **Bagian kode sudah selesai.** **Deploy itu sendiri BELUM dilakukan dan sengaja ditunda sampai mendekati demo** (belum ada URL hidup). Platform final belum dikunci (rencana: Railway), dan `DEPLOYMENT.md` berstatus DRAF yang akan diperbarui saat deploy dilakukan. Catatan: Render dan Railway gratis memblokir SMTP, jadi email di hosting kemungkinan perlu API HTTPS penyedia email (perubahan `utils/mailer.js`, belum dikerjakan). Status deploy diperbarui manual setelah layanan hidup dan checklist di `DEPLOYMENT.md` lolos.
  - `TRUST_PROXY` (angka jumlah hop, mis. `1`; `true`/`false` juga diterima; kosong = perilaku lama) → `app.set("trust proxy", ...)` di `app.js`, parser di `config/env.js`. Dengan angka, `express-rate-limit` 8.x tidak melempar galat validasi `X-Forwarded-For`; `true` hanya memunculkan peringatan `ERR_ERL_PERMISSIVE_TRUST_PROXY` di log.
  - `middleware/request-logger.middleware.js`: token morgan `safe-url` dan `safe-referrer` mengganti nilai parameter `token` menjadi `[REDACTED]` pada format `dev` dan `combined` (produksi). `console.*` lain di `src/` sudah diperiksa: tidak ada yang mencetak URL atau token.
  - Rate limit: keadaan awal hardcoded (`authLimiter` 20, `emailLimiter` 5 per 15 menit, dan `/refresh` ikut `authLimiter`). Kini `AUTH_RATE_LIMIT_MAX` (20), `EMAIL_RATE_LIMIT_MAX` (5), dan limiter terpisah `refreshLimiter` untuk `POST /api/auth/refresh` via `REFRESH_RATE_LIMIT_MAX` (100). Respons 429 tetap lewat `sendError`.
  - `package.json`: `engines.node` = `>=22`.
  - Panduan lengkap: `docs/DEPLOYMENT.md`.

- **R-0.9 ✅ Postman collection + README akar** —
  - `postman/ResQ.postman_collection.json` (Collection v2.1) dan `postman/ResQ.postman_environment.json` (environment **ResQ Local**). Bearer `{{accessToken}}` diatur di level collection; request publik memakai No Auth; request admin (tulis Guide) memakai `{{adminAccessToken}}`. Semua 48 route di `src/` tercakup (51 request: 3 request tambahan untuk Login Admin, prasyarat media, dan tampilan HTML verify-email). Script test menyimpan token dan `id` hasil create ke environment, dan tiap request punya test status code + `success`.
  - Environment tidak berisi kredensial: semua nilai kosong kecuali `baseUrl` (`http://localhost:3000`; ganti dengan URL deploy setelah deploy, emulator Android memakai `http://10.0.2.2:3000`).
  - `README.md` di root backend: deskripsi, SDG 3/5/11, tech stack, prasyarat, langkah lokal, script, struktur folder, cara memakai Postman, alur autentikasi, dan catatan akun admin/demo tanpa kata sandi nyata.

- **R-1.1 ✅ Migrasi skema tunggal (SosAlert, Facility, ReportLike, ReportComment, field baru, enum baru)** — folder migrasi `prisma/migrations/20261026053638_resq_extend_schema/`, hanya berisi penambahan (tanpa `DROP`). Perubahan:
  - Enum baru: `ReportStatus`, `SosCategory`, `SosStatus`, `FacilityType`. Nilai baru: `GuideCategory.LEGAL`, `IncidentCategory.HARASSMENT`/`PUBLIC_FACILITY`, `NotificationType.SOS_ALERT`/`SHARE_ARRIVED`.
  - `User.locationUpdatedAt`; `SharingSession.expiresAt`, `destinationName`, `destinationLatitude`, `destinationLongitude`; `IncidentReport.status` (default `SUBMITTED`), `statusNote`, `isAnonymous` (default `false`) beserta index `createdAt`, `category`, `status`, `[latitude, longitude]`.
  - **Model baru dan field baru belum dipakai oleh API mana pun.** `SosAlert`, `Facility`, `ReportLike`, `ReportComment` baru disiapkan di database (API menyusul di tugas roadmap 1.3, 2.1, dan 3.1). Endpoint lama tidak berubah perilakunya.

## Endpoint yang tersedia (diverifikasi dari route + controller)

- **Health**: `GET /`.
- **Halaman browser**: `GET /reset-password?token=` (HTML).
- **Auth** `/api/auth`: `POST /register`, `POST /login`, `POST /refresh`, `POST /forgot-password`, `POST /resend-verification`, `POST /reset-password`, `GET /verify-email` (HTML/JSON), `GET /profile`*, `PUT /profile`*, `DELETE /profile`*, `POST /logout`*. (Endpoint publik dilindungi rate limiter).
- **Emergency Contact** `/api/emergency-contacts`* : `POST /`, `GET /` (paginasi), `PUT /:id`, `DELETE /:id`.
- **Incident Report** `/api/reports`* : `POST /`, `GET /` (paginasi), `GET /:id`, `PUT /:id`, `DELETE /:id`.
- **Report Media** `/api/reports/:reportId/media`* : `POST /`, `GET /` (paginasi), `DELETE /:mediaId`.
- **Guide** `/api/guides`: `GET /` (publik, paginasi), `GET /:id` (publik), `POST /` (admin), `PUT /:id` (admin), `DELETE /:id` (admin).
- **Friendship** `/api/friendships`* : `POST /request`, `GET /` (paginasi), `GET /requests` (paginasi), `PATCH /:id/accept`, `PATCH /:id/reject`, `DELETE /:id`.
- **Notification** `/api/notifications`* : `GET /` (paginasi), `GET /unread-count`, `PATCH /read-all`, `PATCH /:id/read`, `DELETE /:id`.
- **Sharing Session** `/api/sharing-sessions`* : `POST /`, `GET /active` (paginasi), `GET /:id`, `PATCH /:id/stop`.
- **User Utility** `/api/users`* : `GET /search` (paginasi), `GET /:id`, `GET /:id/friends` (paginasi).

(* = butuh `Authorization: Bearer <accessToken>`)

Seluruh router di atas terdaftar di `app.js` (9 feature router + router halaman browser + health check), dan `errorHandler` global terpasang paling akhir.

## Detail Terverifikasi

### Pola arsitektur (konsisten di semua modul)
- Setiap modul: `routes → controller → service → prisma`, plus `validations/`.
- Service melempar `ServiceError` **bersama** (`utils/service-error.js`); diteruskan ke global error handler.
- Controller CRUD dibungkus `asyncHandler`; hanya validasi gagal yang dikembalikan langsung (**422**). Auth controller tetap `try/catch` lokal.
- Validator buatan sendiri (tanpa library) mengembalikan `{ valid, errors, data }`.
- Listing controller menggunakan `parsePagination(req.query)` dan `buildPaginationMeta(...)` dari `utils/pagination.js`, lalu merespons dengan `sendPaginated` dari `utils/response.js`.

### Keamanan & Logging (Turn 2)
- **Helmet**: mengamankan header HTTP standar di root `app.js`.
- **Morgan**: mencatat request HTTP masuk (`dev` format).
- **Rate limiting**: `express-rate-limit` dipasang di `auth.routes.js`: register, login, forgot/reset password memakai `authLimiter` (default 20 request / 15 menit), `resend-verification` memakai `emailLimiter` (default 5), `refresh` memakai `refreshLimiter` (default 100). Batas diatur lewat `AUTH_RATE_LIMIT_MAX`, `EMAIL_RATE_LIMIT_MAX`, `REFRESH_RATE_LIMIT_MAX`. Melebihi kuota menghasilkan status **429** (`sendError`).
- **Morgan**: token `?token=` pada URL dan referrer diganti `[REDACTED]` agar tidak masuk log.

### Paginasi & Metadata Response (Turn 2)
- Format respons paginasi:
  ```json
  {
    "status": "success",
    "data": [...],
    "meta": {
      "page": 1,
      "limit": 10,
      "total": 42,
      "totalPages": 5
    }
  }
  ```
- Nilai default: `page = 1`, `limit = 10`. Batas maksimal `limit` = **50**.

## Rencana Lanjutan

Pengerjaan selanjutnya mengikuti `docs/RESQ_BACKEND_ROADMAP.md` (Fase 0-3 dan ID tugas di sana).

- Istilah **"Phase"** di dokumen ini merujuk pekerjaan lama yang sudah selesai.
- Istilah **"Fase"** di roadmap merujuk pekerjaan baru yang akan dikerjakan.
- Setiap tugas roadmap yang selesai dicatat dengan menyebut ID tugasnya (contoh: "SOS (R-1.3) ✅").
