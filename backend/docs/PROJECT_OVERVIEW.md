# ResQ Backend — Project Overview

## Apa Ini?

**ResQ Backend** adalah REST API untuk aplikasi mobile **ResQ**, sebuah aplikasi keselamatan/darurat (emergency & safety). API ini menangani autentikasi pengguna, kontak darurat, pelaporan insiden beserta media (foto/video), dan direncanakan mendukung panduan keselamatan, notifikasi, pertemanan, serta berbagi lokasi secara real-time.

## Tujuan Fungsional

- Pengguna dapat mendaftar, verifikasi email, login, dan mengelola profil.
- Pengguna dapat menyimpan daftar **kontak darurat** berprioritas.
- Pengguna dapat membuat **laporan insiden** (kriminal, kecelakaan, kebakaran, medis, bencana, dll.) lengkap dengan lokasi (lat/long) dan media.
- Pengguna dapat membaca **panduan keselamatan** (dikelola admin), menerima **notifikasi**, mengelola **pertemanan**, serta memulai **sesi berbagi lokasi** ke teman yang sudah diterima.
- Terdapat **utilitas user** (cari user, lihat profil publik + status pertemanan, lihat daftar teman user).

## Tech Stack

| Komponen | Teknologi |
| --- | --- |
| Runtime | Node.js (CommonJS) |
| Framework | Express 5 |
| ORM | Prisma 6 |
| Database | PostgreSQL (Neon, serverless) |
| Auth | JWT (access + refresh), bcrypt untuk hashing |
| Upload media | Multer + Cloudinary |
| Email | Nodemailer (SMTP) |
| Dev tooling | nodemon |

## Arsitektur (Layered)

Alur request mengikuti pola berlapis yang jelas:

```
Route  ->  Middleware  ->  Controller  ->  Service  ->  Prisma  ->  Database
                               |
                          Validation
```

- **Routes** (`src/routes/`): mendefinisikan endpoint + memasang middleware.
- **Middleware** (`src/middleware/`): autentikasi, otorisasi role, upload file.
- **Controllers** (`src/controllers/`): menangani `req`/`res`, memanggil service, memformat respons. Tidak ada logika bisnis di sini.
- **Services** (`src/services/`): berisi logika bisnis murni dan akses database via Prisma. **Tidak pernah menyentuh `req`/`res`.**
- **Validations** (`src/validations/`): sanitasi dan validasi input.
- **Utils** (`src/utils/`): helper pendukung:
  - `response.js` — `sendSuccess`/`sendError`, envelope respons konsisten.
  - `hash.js` — bcrypt (salt rounds dari env, default 10).
  - `jwt.js` — sign/verify 4 jenis token: access, refresh, email-verification, password-reset.
  - `mailer.js` — Nodemailer (transport lazy) + template email verifikasi & reset.
  - `cloudinary.js` — upload buffer, delete asset, derive `public_id` dari URL.
  - `service-error.js` — kelas **`ServiceError` bersama** dipakai seluruh service.
  - `async-handler.js` — **`asyncHandler`** pembungkus handler async agar error diteruskan ke global error handler.
- **Config** (`src/config/`):
  - `prisma.js` — satu instance PrismaClient dipakai bersama (hemat koneksi untuk serverless Neon).
  - `db.js` — `verifyDatabaseConnection()` jalankan `SELECT 1` saat start; gagal tidak memblokir server.

### Middleware
- `auth.middleware.js` — verifikasi `Authorization: Bearer <token>`, isi `req.user = { id, role }`; gagal → 401.
- `upload.middleware.js` — multer memory storage, limit 20MB, hanya `image/*` & `video/*`.
- `role.middleware.js` — `authorize(...roles)` **sudah diimplementasikan** (cek `req.user.role`; tanpa identitas → 401, role tak diizinkan → 403) + guard `isAdmin` untuk route admin.
- `error.middleware.js` — **global error handler** (`errorHandler`), middleware error terakhir di `app.js`.

### Penanganan error (kondisi saat ini)
- **Ada global error handler** di `app.js` (`error.middleware.js`), terdaftar paling akhir. Ia memetakan `ServiceError` ke respons standar dan mengubah error tak terduga menjadi **500** generik tanpa membocorkan stack trace.
- **`ServiceError` sudah di-share** di `utils/service-error.js`; setiap service mengimpor & meng-ekspor ulang dari sana.
- **Ada `asyncHandler`** (`utils/async-handler.js`); seluruh controller CRUD dibungkus dengannya sehingga error diteruskan ke error handler global. Pengecualian: controller **auth** masih memakai `try/catch` lokal (respons tetap sama).
- **Belum ada Swagger/OpenAPI** — dokumentasi endpoint saat ini hanya di `API_REFERENCE.md`.

## Struktur Folder

```
backend/
├── docs/                     # Dokumentasi proyek (folder ini)
├── prisma/
│   ├── schema.prisma         # Skema database (source of truth untuk model)
│   └── migrations/           # Migrasi database
├── src/
│   ├── app.js                # Setup Express app + registrasi route
│   ├── server.js             # Entry point: start server + koneksi DB
│   ├── config/               # prisma.js, db.js
│   ├── controllers/          # auth, emergency-contact, report, report-media, guide, friendship, notification, sharing-session, user
│   ├── middleware/           # auth, role, upload, error
│   ├── routes/               # definisi endpoint (9 router)
│   ├── services/             # logika bisnis
│   ├── utils/                # jwt, hash, mailer, cloudinary, response, service-error, async-handler
│   └── validations/          # validasi input per fitur
├── uploads/                  # penyimpanan sementara upload (ada .gitkeep)
├── .env                      # variabel lingkungan (JANGAN di-commit)
└── package.json
```

## Entitas Database (dari `schema.prisma`)

| Model | Keterangan | Status implementasi |
| --- | --- | --- |
| `User` | Akun pengguna (role USER/ADMIN, verifikasi email, lokasi terakhir) | Dipakai penuh |
| `RefreshToken` | Penyimpanan refresh token untuk revoke/logout | Dipakai penuh |
| `EmergencyContact` | Kontak darurat berprioritas milik user | Dipakai penuh |
| `IncidentReport` | Laporan insiden dengan kategori & lokasi | Dipakai penuh |
| `ReportMedia` | Media (IMAGE/VIDEO) milik laporan | Dipakai penuh |
| `Guide` | Panduan keselamatan berkategori | Dipakai penuh (baca publik, tulis admin) |
| `Notification` | Notifikasi ke pengguna | Dipakai penuh (dibuat via event bisnis) |
| `Friendship` | Relasi pertemanan (PENDING/ACCEPTED/REJECTED) | Dipakai penuh |
| `SharingSession` | Sesi berbagi lokasi owner -> viewer | Dipakai penuh |

## Variabel Lingkungan (`.env`)

Kunci yang dibutuhkan (nilai rahasia tidak ditulis di sini):

- **Server**: `PORT`, `APP_URL`
- **Database**: `DATABASE_URL` (Neon PostgreSQL, wajib `sslmode=require`)
- **JWT**: `JWT_SECRET`, `JWT_EXPIRES_IN`, `JWT_REFRESH_SECRET`, `JWT_REFRESH_EXPIRES_IN`, `JWT_EMAIL_VERIFICATION_SECRET`, `JWT_EMAIL_VERIFICATION_EXPIRES_IN`, `JWT_PASSWORD_RESET_SECRET`, `JWT_PASSWORD_RESET_EXPIRES_IN`
- **Hashing**: `BCRYPT_SALT_ROUNDS`
- **Cloudinary**: `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, `CLOUDINARY_API_SECRET`
- **SMTP/Mail**: `SMTP_HOST`, `SMTP_PORT`, `SMTP_USER`, `SMTP_PASS`, `SMTP_FROM`

> Catatan keamanan: `.env` berisi kredensial asli (DB, JWT secret, Cloudinary, SMTP). Jangan commit ke repository dan jangan tampilkan nilainya di dokumentasi.

## Menjalankan Proyek

```bash
npm install
npx prisma generate          # generate Prisma client
npx prisma migrate deploy    # terapkan migrasi ke database
npm run dev                  # jalankan dengan nodemon (development)
npm start                    # jalankan production
```

Health check: `GET /` mengembalikan `{ success: true, message: "ResQ Backend API is running" }`.

> Catatan: `utils/jwt.js` memakai fallback default secret bila env kosong. Untuk produksi, set semua `JWT_*_SECRET` agar tidak memakai default yang lemah.
