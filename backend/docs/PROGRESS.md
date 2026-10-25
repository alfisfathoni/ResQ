# ResQ Backend — Status Pengerjaan

> **Terakhir diperbarui:** 2026-10-25 (audit ulang menyeluruh setelah Phase 1 + Phase 2 item 1-2)
> **Sumber kebenaran:** HANYA kode — seluruh `src/` (app.js, routes, controllers, services, validations, middleware, utils, config), `prisma/schema.prisma`, dan seluruh migrasi Prisma. Dokumentasi lama TIDAK dijadikan acuan.
> **Metode:** Setiap file di `src/` dibaca baris per baris. Jika dokumen lama bertentangan dengan kode, kode yang menang dan dokumen diperbarui mengikuti kode.
> **Aturan:** Setiap kali menyelesaikan atau mengubah fitur, perbarui tabel di bawah ini. Status yang jujur lebih berguna daripada status yang optimis.

## Ringkasan (kondisi backend saat ini)

Backend ResQ kini memiliki **sembilan modul fitur yang berfungsi penuh** end-to-end (route → controller → service → validasi → Prisma):
**Authentication (termasuk User Profile)**, **Emergency Contact**, **Incident Report**, **Report Media**, **Guide**, **Friendship**, **Notification**, **Sharing Session**, dan **User Utility**. Semua model di `prisma/schema.prisma` sudah memiliki API.

Infrastruktur lintas-sistem juga sudah matang:
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
| Phase 2 — Item 3 | API Documentation (Swagger/OpenAPI) | ⬜ |

## Status Per Fitur

| Fitur | Status | Catatan (berdasarkan kode) |
| --- | --- | --- |
| **Authentication** | ✅ | register, login (email/username), refresh, logout, verify-email, forgot/reset-password. Controller auth masih `try/catch` lokal. |
| **User Profile** | ✅ | Bagian modul auth: `GET/PUT/DELETE /api/auth/profile`. Update terbatas `fullName`, `phoneNumber`, `profilePicture`. DELETE menolak ADMIN (403). |
| **Emergency Contact** | ✅ | CRUD penuh. Maks 5/user, `priority` unik 1-5, `phoneNumber` unik per user. |
| **Incident Report** | ✅ | CRUD penuh. `category` enum, koordinat divalidasi & di-serialize ke `Number`. |
| **Report Media** | ✅ | Upload (Multer→Cloudinary), list, delete. Hapus asset best-effort lalu hapus baris DB. |
| **Guide** | ✅ | `GET /` & `GET /:id` publik; `POST`/`PUT`/`DELETE` admin (`authenticate` + `isAdmin`). Kategori enum `GuideCategory`. |
| **Friendship** | ✅ | request, list friends, list incoming requests, accept, reject, remove. Cek dua arah; revive `REJECTED`→`PENDING`. |
| **Notification** | ✅ | list, unread-count, read, read-all, delete. Dibuat via event bisnis (bukan endpoint create langsung). |
| **Sharing Session** | ✅ | start (ke semua teman ACCEPTED), list active, detail, stop. Satu sesi aktif per owner. |
| **User Utility** | ✅ | search (by username/fullName), detail (+`friendshipStatus`), list friends. Hanya field publik. |
| **Notification Integration** | ✅ | friendship (FRIEND_REQUEST, FRIEND_ACCEPTED) & sharing session (SHARE_SESSION) membuat notifikasi best-effort via `notification.service`. |
| **Global Error Handler** | ✅ | `middleware/error.middleware.js` terdaftar terakhir di `app.js`; map `ServiceError` + 500 generik tanpa bocor stack. |
| **Shared ServiceError** | ✅ | `utils/service-error.js`; tiap service import & re-export. Tidak ada lagi duplikasi kelas. |
| **asyncHandler** | ✅ | `utils/async-handler.js`; dipakai semua controller CRUD. Auth controller pengecualian (try/catch lokal). |
| **Admin / Role Middleware** | ✅ | `role.middleware.js` — `authorize(...roles)` + `isAdmin`. Dipakai di route admin Guide. (Dulu placeholder 501.) |
| **Upload Middleware** | ✅ | multer memory storage, limit 20MB, filter `image/*` & `video/*`. |
| **Auth Middleware** | ✅ | verifikasi Bearer, isi `req.user = { id, role }`, gagal → 401. |
| **Cloudinary** | ✅ | `uploadBuffer`, `deleteAsset`, `publicIdFromUrl`. |
| **JWT** | ✅ | 4 tipe token: access, refresh, email-verification, password-reset. |
| **Mail** | ✅ | Nodemailer (lazy), email verifikasi & reset. |
| **Prisma** | ✅ | Singleton client + verifikasi `SELECT 1` saat start. Skema + migrasi `init`. |
| **API Reference (markdown)** | ✅ | `docs/API_REFERENCE.md` mencakup seluruh 9 modul (diverifikasi ulang terhadap route/controller/validasi). |
| **Swagger / OpenAPI** | ⬜ | Belum ada dependency maupun setup di `app.js`. = Phase 2 Item 3. |
| **Testing** | ⬜ | Belum ada framework test, file test, atau script `test` di `package.json`. |
| **Documentation** | ✅ | Folder `docs/` diaudit ulang & disinkronkan dengan kode pada tanggal di atas. |

## Endpoint yang tersedia (diverifikasi dari route + controller)

- **Health**: `GET /`.
- **Auth** `/api/auth`: `POST /register`, `POST /login`, `POST /refresh`, `POST /forgot-password`, `POST /reset-password`, `GET /verify-email`, `GET /profile`*, `PUT /profile`*, `DELETE /profile`*, `POST /logout`*.
- **Emergency Contact** `/api/emergency-contacts`* : `POST /`, `GET /`, `PUT /:id`, `DELETE /:id`.
- **Incident Report** `/api/reports`* : `POST /`, `GET /`, `GET /:id`, `PUT /:id`, `DELETE /:id`.
- **Report Media** `/api/reports/:reportId/media`* : `POST /`, `GET /`, `DELETE /:mediaId`.
- **Guide** `/api/guides`: `GET /` (publik), `GET /:id` (publik), `POST /` (admin), `PUT /:id` (admin), `DELETE /:id` (admin).
- **Friendship** `/api/friendships`* : `POST /request`, `GET /`, `GET /requests`, `PATCH /:id/accept`, `PATCH /:id/reject`, `DELETE /:id`.
- **Notification** `/api/notifications`* : `GET /`, `GET /unread-count`, `PATCH /read-all`, `PATCH /:id/read`, `DELETE /:id`.
- **Sharing Session** `/api/sharing-sessions`* : `POST /`, `GET /active`, `GET /:id`, `PATCH /:id/stop`.
- **User Utility** `/api/users`* : `GET /search`, `GET /:id`, `GET /:id/friends`.

(* = butuh `Authorization: Bearer <accessToken>`)

Seluruh router di atas terdaftar di `app.js` (9 feature router + health check), dan `errorHandler` global terpasang paling akhir.

## Detail Terverifikasi

### Pola arsitektur (konsisten di semua modul)
- Setiap modul: `routes → controller → service → prisma`, plus `validations/`.
- Service melempar `ServiceError` **bersama** (`utils/service-error.js`); diteruskan ke global error handler.
- Controller CRUD dibungkus `asyncHandler`; hanya validasi gagal yang dikembalikan langsung (**422**). Auth controller tetap `try/catch` lokal.
- Validator buatan sendiri (tanpa library) mengembalikan `{ valid, errors, data }`.

### Notification business integration (Phase 2 — Item 1)
- `notification.service` menyediakan `createNotification` (satu) & `createNotifications` (bulk), keduanya **best-effort** (gagal → log + swallow, tidak membatalkan operasi utama).
- Friendship: kirim request → `FRIEND_REQUEST`; accept → `FRIEND_ACCEPTED`.
- Sharing session: start → `SHARE_SESSION` ke setiap teman ACCEPTED.

### Global error handling (Phase 2 — Item 2)
- `errorHandler` empat-argumen, terdaftar terakhir di `app.js`. `headersSent` → delegasi ke Express default.
- `ServiceError` → status/message/errors asli; error lain → **500** `"Something went wrong."` + `console.error`.

### Catatan modul baru
- **Friendship**: relasi diperiksa di kedua arah; baris `REJECTED` di arah sama "dihidupkan" ke `PENDING`; accept/reject hanya oleh addressee; unfriend hanya untuk `ACCEPTED`.
- **Sharing Session**: satu sesi = banyak baris dengan `startedAt` sama (satu per viewer). Satu sesi aktif per owner; stop menonaktifkan seluruh grup.
- **User Utility**: hanya field publik (`id, fullName, username, profilePicture, createdAt`); `friendshipStatus` ∈ `FRIENDS|PENDING_SENT|PENDING_RECEIVED|NONE` (REJECTED disembunyikan).
- **Guide**: konten global admin-managed; baca publik, tulis via `isAdmin`.
- **Route ordering**: path literal (`/search`, `/active`, `/unread-count`, `/read-all`) dideklarasikan sebelum `/:id`.

## Yang Belum (benar-benar belum ada di kode)

1. **Swagger / OpenAPI** (Phase 2 — Item 3) — belum ada dependency maupun setup. Dokumentasi endpoint saat ini di `docs/API_REFERENCE.md`.
2. **Automated testing** — belum ada framework/test/skrip test.

### Catatan keamanan (temuan dari kode, masih berlaku)
- `utils/jwt.js` punya **fallback default secret** (`"change_me_access_secret"`) bila env kosong. Untuk produksi, set semua `JWT_*_SECRET`.
- `utils/mailer.js` mengirim link reset ke `${APP_URL}/api/auth/reset-password?token=...`, sedangkan endpoint reset adalah **POST** (butuh body `resetToken` + `newPassword`). Link email diharapkan membuka halaman frontend, bukan endpoint API langsung — pastikan `APP_URL`/alur frontend konsisten.

## Prioritas Berikutnya (kondisi backend saat ini)

Seluruh modul fitur inti dan infrastruktur error sudah selesai. Prioritas mengikuti kondisi nyata:

1. **API Documentation (Swagger/OpenAPI)** — Phase 2 Item 3 yang tersisa; melengkapi `API_REFERENCE.md` dengan dokumentasi interaktif.
2. **API Testing** — pasang framework test (mis. Jest/Vitest) + tulis test endpoint.
3. **Postman Collection** — untuk eksplorasi & QA manual.
4. **Frontend Integration**.
5. **Bug Fixing** & **Regression Testing**.
6. **Deployment Preparation**.

> Perbarui daftar ini sesuai arahan terbaru pemilik proyek.
