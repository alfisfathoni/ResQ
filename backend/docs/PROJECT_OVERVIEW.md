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
  - `jwt.js` — sign/verify 4 jenis token: access, refresh, email-verification, password-reset. Keempat secret wajib ada di env (tanpa fallback).
  - `mailer.js` — Nodemailer (transport lazy) + template email verifikasi & reset.
  - `cloudinary.js` — upload buffer, delete asset, derive `public_id` dari URL.
  - `phone.js` — `normalizePhone()` (`08…`/`628…`/`+628…` → `+628…`) dan `phoneVariants()`; dipakai SOS untuk mencocokkan kontak darurat dengan pengguna terdaftar.
  - `service-error.js` — kelas **`ServiceError` bersama** dipakai seluruh service.
  - `async-handler.js` — **`asyncHandler`** pembungkus handler async agar error diteruskan ke global error handler.
- **Config** (`src/config/`):
  - `prisma.js` — satu instance PrismaClient dipakai bersama (hemat koneksi untuk serverless Neon).
  - `db.js` — `verifyDatabaseConnection()` jalankan `SELECT 1` saat start; gagal tidak memblokir server.

### Middleware
- `request-logger.middleware.js` — logging `morgan` dengan token kustom `safe-url` dan `safe-referrer` yang mengganti nilai parameter `token` menjadi `[REDACTED]` (format `dev` dan produksi).
- `rate-limit.middleware.js` — `authLimiter`, `emailLimiter`, `refreshLimiter` (batasnya dapat diatur lewat env, lihat "Variabel Lingkungan") dan `sosLimiter` (5 request / menit per user untuk `POST /api/sos`, tidak dapat diatur lewat env).
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
│   ├── seed.js               # Seed idempoten: admin, panduan, user demo (`npm run seed`)
│   ├── seed/guides.json      # Konten panduan untuk seed
│   └── migrations/           # Migrasi database
├── src/
│   ├── app.js                # Setup Express app + registrasi route
│   ├── server.js             # Entry point: start server + koneksi DB
│   ├── config/               # prisma.js, db.js, env.js (validasi env saat start)
│   ├── controllers/          # auth, emergency-contact, report, report-media, guide, friendship, notification, sharing-session, user, sos
│   ├── middleware/           # auth, role, upload, error
│   ├── routes/               # definisi endpoint (10 router)
│   ├── services/             # logika bisnis
│   ├── utils/                # jwt, hash, mailer, cloudinary, response, service-error, async-handler
│   └── validations/          # validasi input per fitur
├── uploads/                  # tidak dipakai kode (hanya .gitkeep); upload memakai Multer memoryStorage, tanpa disk
├── .env.example              # daftar nama variabel lingkungan (tanpa nilai rahasia)
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
| `SharingSession` | Sesi berbagi lokasi owner -> viewer (kolom baru `expiresAt` dan tujuan perjalanan sudah ada di skema, belum dipakai API) | Dipakai penuh |
| `SosAlert` | Peringatan SOS (kategori, status, lokasi) milik user | Dipakai penuh (`/api/sos`; satu SOS `ACTIVE` per user ditegakkan di service) |
| `Facility` | Fasilitas publik (polisi, rumah sakit, halte, dll.) dengan koordinat | Disiapkan, API belum ada |
| `ReportLike` | Like pada laporan insiden (unik per laporan + user) | Disiapkan, API belum ada |
| `ReportComment` | Komentar pada laporan insiden | Disiapkan, API belum ada |

## Variabel Lingkungan (`.env`)

Kunci yang dibutuhkan (nilai rahasia tidak ditulis di sini). Daftar lengkap dengan komentar ada di `.env.example`; salin menjadi `.env` lalu isi.

- **Server**: `PORT`, `NODE_ENV`, `APP_URL`, `TRUST_PROXY` (opsional; isi angka jumlah proxy, mis. `1`, saat di balik proxy hosting agar rate limit membaca IP klien yang benar; kosong = tidak dipercaya)
- **Rate limit (opsional, maks request per IP per 15 menit)**: `AUTH_RATE_LIMIT_MAX` (default 20), `EMAIL_RATE_LIMIT_MAX` (default 5), `REFRESH_RATE_LIMIT_MAX` (default 100, khusus `POST /api/auth/refresh`)
- **Demo**: `AUTO_VERIFY_EMAIL` (default `false`; jangan aktif di luar demo)
- **Database**: `DATABASE_URL` (**wajib**; Neon PostgreSQL, wajib `sslmode=require`)
- **JWT**: `JWT_SECRET`, `JWT_REFRESH_SECRET`, `JWT_EMAIL_VERIFICATION_SECRET`, `JWT_PASSWORD_RESET_SECRET` (**keempatnya wajib**, tanpa fallback), serta masa berlaku opsional `JWT_EXPIRES_IN`, `JWT_REFRESH_EXPIRES_IN`, `JWT_EMAIL_VERIFICATION_EXPIRES_IN`, `JWT_PASSWORD_RESET_EXPIRES_IN`
- **Hashing**: `BCRYPT_SALT_ROUNDS`
- **Cloudinary**: `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, `CLOUDINARY_API_SECRET`, `CLOUDINARY_REPORT_FOLDER` (opsional)
- **SMTP/Mail**: `SMTP_HOST`, `SMTP_PORT`, `SMTP_USER`, `SMTP_PASS`, `SMTP_FROM`
- **Seed (`ADMIN_*` dan `SEED_*`, hanya dibaca oleh `npm run seed`)**:
  - `ADMIN_EMAIL`, `ADMIN_PASSWORD` — **wajib** untuk seed.
  - `ADMIN_USERNAME` (default `admin`), `ADMIN_FULL_NAME` (default `ResQ Admin`), `ADMIN_PHONE` (opsional, harus unik dan sesuai `^\+?\d{7,15}$`) — opsional.
  - `SEED_RESET_ADMIN_PASSWORD` — isi `true` hanya bila ingin menimpa password admin yang sudah ada (default: tidak menimpa).
  - `SEED_DEMO_PASSWORD` — bila diisi, seed membuat 2 user demo (`demo.user1@example.com`, `demo.user2@example.com`); kosong = dilewati.

**Validasi saat start:** server (`src/config/env.js`) menolak start di **semua environment** bila `DATABASE_URL` atau salah satu `JWT_*_SECRET` kosong; pesan error hanya menyebut nama variabel.

> Catatan keamanan: `.env` berisi kredensial asli (DB, JWT secret, Cloudinary, SMTP, password admin). Jangan commit ke repository dan jangan tampilkan nilainya di dokumentasi. `.env.example` hanya berisi nama variabel.

## Menjalankan Proyek

```bash
cp .env.example .env         # lalu isi nilainya (jangan commit .env)
npm install                  # otomatis menjalankan `prisma generate` (postinstall)
npm run dev                  # development dengan nodemon (tidak menjalankan migrasi)
npm start                    # production: `prisma migrate deploy` lalu `node src/server.js`
npm run seed                 # isi akun admin, panduan, dan (opsional) user demo
```

- `npm start` menerapkan migrasi yang tertunda sebelum server berjalan. Karena itu paket `prisma` (CLI) berada di `dependencies`, bukan `devDependencies`.
- `npm run seed` idempoten dan hanya menambah data (aman diulang, tidak menghapus). Butuh `ADMIN_EMAIL` dan `ADMIN_PASSWORD` di `.env`. Panduan yang sudah ada (judul + kategori sama) tidak ditimpa. Untuk database baru, jalankan migrasi dulu (`npx prisma migrate deploy` atau `npm start`).
- Akun admin hanya dibuat lewat seed (tidak ada pendaftaran admin dari API). Login memakai `POST /api/auth/login` dan FE membaca `data.user.role`.

Untuk menjalankan backend di hosting cloud (HTTPS, URL publik), ikuti panduan di [`DEPLOYMENT.md`](DEPLOYMENT.md).

Health check: `GET /` mengembalikan `{ success: true, message: "ResQ Backend API is running" }`.

> Catatan: semua `JWT_*_SECRET` dan `DATABASE_URL` wajib diisi; server menolak start bila ada yang kosong (tidak ada lagi fallback secret).
