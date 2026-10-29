# ResQ Backend

REST API untuk **ResQ**, aplikasi mobile tanggap darurat dan keselamatan. Backend ini melayani autentikasi, kontak darurat, laporan insiden beserta foto/video, panduan keselamatan, pertemanan, notifikasi, sesi berbagi lokasi, dan SOS. **Modul SOS sudah tersedia** (`/api/sos`: `POST /` memicu SOS, `GET /active`, `GET /` riwayat, dan `PATCH /:id/resolve`; notifikasi ke teman dan ke kontak darurat yang akunnya terdaftar dan terverifikasi). Fitur lanjutan lain (peta fasilitas, lokasi live, feed publik laporan, endpoint admin) sedang dikerjakan bertahap; lihat [`docs/PROGRESS.md`](docs/PROGRESS.md) untuk status terkini.

**SDG yang ditargetkan:** SDG 3 (Kehidupan Sehat dan Sejahtera), SDG 5 (Kesetaraan Gender), dan SDG 11 (Kota dan Permukiman yang Berkelanjutan).

## Tech Stack

| Komponen | Teknologi |
| --- | --- |
| Runtime | Node.js 22 atau lebih baru (CommonJS) |
| Framework | Express 5 |
| ORM | Prisma 6 |
| Database | PostgreSQL di Neon (serverless) |
| Autentikasi | JWT (access + refresh), bcrypt |
| Media | Multer (memori) + Cloudinary |
| Email | Nodemailer (SMTP) |
| Keamanan dan log | helmet, express-rate-limit, morgan |

## Prasyarat

- **Node.js 22+** dan npm (`node -v`).
- Akun dan kredensial untuk layanan eksternal:
  - **Neon**: database PostgreSQL (connection string dengan `sslmode=require`).
  - **Cloudinary**: upload foto/video laporan.
  - **SMTP**: pengirim email verifikasi dan reset password (mis. Gmail dengan *App Password* untuk dev lokal, atau relay penyedia email).

## Menjalankan di Lokal

> **Peringatan: database cloud (Neon) dipakai bersama oleh seluruh tim dan oleh hosting.** Jangan menjalankan `prisma migrate reset` atau `prisma db push`, dan jangan menghapus data sembarangan. Perubahan skema hanya dilakukan lewat migrasi oleh pemilik skema.

```bash
# 1. Clone repo (berisi folder backend/ dan frontend/), lalu masuk ke backend
git clone <URL_REPO>
cd ResQ/backend

# 2. Pasang dependensi (otomatis menjalankan `prisma generate`)
npm install

# 3. Salin template environment lalu isi nilainya (lihat tabel di bawah)
cp .env.example .env

# 4. Generate Prisma Client (sudah otomatis oleh `npm install`; ulangi bila perlu)
npx prisma generate

# 5. Terapkan migrasi yang belum terpasang (aman diulang, tidak menghapus data)
npx prisma migrate deploy

# 6. Isi akun admin, panduan, dan (opsional) user demo. Idempoten, hanya menambah data
npm run seed

# 7. Jalankan server (nodemon)
npm run dev
```

Server berjalan di `http://localhost:3000` (atau `PORT` di `.env`). Cek dengan `GET /`: respons `{ "success": true, "message": "ResQ Backend API is running" }`.

### Isi `.env`

Nilai tidak pernah ditulis di repo. Template lengkap beserta komentar ada di [`.env.example`](.env.example). Kelompok variabel:

| Kelompok | Variabel | Keterangan |
| --- | --- | --- |
| Server | `PORT`, `NODE_ENV`, `APP_URL`, `TRUST_PROXY` | `APP_URL` dipakai untuk link di email. `TRUST_PROXY` hanya diisi di balik proxy hosting |
| Database | `DATABASE_URL` | **Wajib**, Neon dengan `sslmode=require` |
| JWT | `JWT_SECRET`, `JWT_REFRESH_SECRET`, `JWT_EMAIL_VERIFICATION_SECRET`, `JWT_PASSWORD_RESET_SECRET` | **Wajib semua**, nilai acak dan berbeda satu sama lain. Server menolak start bila kosong. Masa berlaku opsional lewat `JWT_*_EXPIRES_IN` |
| Hashing | `BCRYPT_SALT_ROUNDS` | Opsional (default 10) |
| Cloudinary | `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, `CLOUDINARY_API_SECRET`, `CLOUDINARY_REPORT_FOLDER` | Dibutuhkan untuk upload media laporan |
| Email | `SMTP_HOST`, `SMTP_PORT`, `SMTP_USER`, `SMTP_PASS`, `SMTP_FROM` | Dibutuhkan untuk email verifikasi dan reset password |
| Rate limit | `AUTH_RATE_LIMIT_MAX`, `EMAIL_RATE_LIMIT_MAX`, `REFRESH_RATE_LIMIT_MAX` | Opsional. Naikkan bila banyak perangkat berbagi satu Wi-Fi |
| Demo | `AUTO_VERIFY_EMAIL` | Default `false`. `true` = akun baru langsung terverifikasi. Hanya untuk dev/demo saat SMTP mati |
| Seed | `ADMIN_*`, `SEED_*` | Hanya dibaca oleh `npm run seed` (lihat bagian Akun Demo dan Admin) |

Bila SMTP belum siap saat mencoba di lokal, isi `AUTO_VERIFY_EMAIL=true` sementara agar bisa login tanpa email verifikasi.

## Script `package.json`

| Script | Fungsi |
| --- | --- |
| `npm run dev` | Server development dengan nodemon (tidak menjalankan migrasi) |
| `npm start` | Produksi: `prisma migrate deploy` lalu `node src/server.js` |
| `npm run seed` | Seed idempoten: akun admin, panduan, user demo (opsional) |
| `postinstall` | Otomatis dijalankan setelah `npm install`: `prisma generate` |

## Struktur Folder

```
backend/
├── docs/            # Dokumentasi proyek
├── postman/         # Postman collection dan environment
├── prisma/          # schema.prisma, migrations/, seed.js, seed/guides.json
├── src/
│   ├── app.js       # Setup Express + registrasi route
│   ├── server.js    # Entry point
│   ├── config/      # prisma, db, validasi env
│   ├── routes/      # Definisi endpoint
│   ├── controllers/ # Menangani req/res
│   ├── services/    # Logika bisnis + akses database
│   ├── validations/ # Validasi input
│   ├── middleware/  # auth, role, upload, rate limit, logger, error
│   └── utils/       # jwt, hash, mailer, cloudinary, response, dll.
├── .env.example     # Template variabel lingkungan (tanpa nilai)
└── package.json
```

Pola berlapis: `route -> middleware -> controller -> service -> Prisma`. Aturan lengkap ada di [`docs/CONVENTIONS.md`](docs/CONVENTIONS.md).

## Alur Autentikasi

1. **Register**: `POST /api/auth/register`. Akun dibuat dengan `isVerified=false` dan email verifikasi dikirim.
2. **Verifikasi email**: buka link di email (`GET /api/auth/verify-email?token=...`). Bila email tidak sampai, minta ulang lewat `POST /api/auth/resend-verification`.
3. **Login**: `POST /api/auth/login` dengan email atau username. Respons berisi `accessToken`, `refreshToken`, dan `user` (termasuk `role` `USER` atau `ADMIN`). Akun yang belum terverifikasi ditolak (403).
4. **Memakai API**: kirim header `Authorization: Bearer <accessToken>` ke endpoint yang butuh login.
5. **Token habis**: access token berumur pendek. Minta yang baru dengan `POST /api/auth/refresh` memakai `refreshToken`. `POST /api/auth/logout` mencabut refresh token.
6. **Lupa password**: `POST /api/auth/forgot-password` mengirim email berisi link ke halaman `GET /reset-password?token=...`, yang menyetel password baru lewat `POST /api/auth/reset-password`.

Satu pintu login dipakai user dan admin; aplikasi membaca `data.user.role` untuk memilih halaman awal.

## Akun Demo dan Admin

- **Akun admin tidak didaftarkan lewat API.** Akun `ADMIN` dibuat oleh `npm run seed` dari variabel `ADMIN_EMAIL` dan `ADMIN_PASSWORD` (opsional `ADMIN_USERNAME`, `ADMIN_FULL_NAME`, `ADMIN_PHONE`) yang Anda isi di `.env` lokal. Akun ini langsung terverifikasi.
- Bila `SEED_DEMO_PASSWORD` diisi, seed juga membuat dua user demo (`demo_user1` dan `demo_user2`) untuk menguji pertemanan dan sesi berbagi.
- **Tidak ada kata sandi nyata di repo ini.** Kredensial akun demo dan admin untuk penilai disampaikan di laporan atau video demo, bukan di repo. Gunakan password khusus demo.
- Seed tidak menimpa password admin yang sudah ada kecuali `SEED_RESET_ADMIN_PASSWORD=true`.

## Postman Collection

File ada di folder [`postman/`](postman/):
- `ResQ.postman_collection.json`: seluruh endpoint, dikelompokkan per modul, beserta test otomatis.
- `ResQ.postman_environment.json`: variabel environment (nilai awal kosong, kecuali `baseUrl`).

Cara memakai:
1. Di Postman, **Import** kedua file, lalu pilih environment **ResQ Local** di pojok kanan atas.
2. `baseUrl` default `http://localhost:3000`. Setelah backend di-deploy, ganti dengan URL deploy. Emulator Android memakai `http://10.0.2.2:3000`.
3. Isi `email` dan `password` dengan akun uji Anda (simpan hanya di environment lokal, **jangan commit**).
4. Jalankan **Auth -> Register**, verifikasi email, lalu **Auth -> Login**. Token disimpan otomatis ke `accessToken` dan `refreshToken`; semua request lain memakainya sebagai Bearer.
5. Untuk request admin (membuat/mengubah panduan), isi `adminEmail` dan `adminPassword` lalu jalankan **Auth -> Login Admin**.
6. Request create (kontak, laporan, permintaan teman, sesi berbagi) menyimpan `id` hasilnya, sehingga request detail/update/delete di bawahnya langsung bisa dijalankan.

Catatan:
- Alur pertemanan butuh dua akun (accept/reject dijalankan dengan token penerima), dan sesi berbagi butuh minimal satu teman `ACCEPTED`.
- Request bertanda ⚠️ bersifat **destruktif dan permanen**: **⚠️ Delete Account** (menghapus akun yang sedang login), **⚠️ Delete Report**, **⚠️ Delete Media**, dan **⚠️ Delete Guide (admin)**. Database dipakai bersama, jadi pakai hanya akun dan data uji.
- Variabel panduan dipisah: `guideId` untuk membaca (List/Get Guide) dan `createdGuideId` untuk menulis. Create Guide menyimpan id ke `createdGuideId`, dan Update/Delete Guide hanya memakainya, sehingga panduan seed tidak ikut berubah atau terhapus.
- **Report Media -> Upload Media** mengunggah file ke Cloudinary; pilih file di tab Body.
- **SOS -> Trigger SOS** mengirim notifikasi ke teman dan kontak darurat yang terdaftar. Pakai akun uji dan akhiri dengan **Resolve SOS**.
- Sebelum menjalankan folder Guides, pastikan `npm run seed` sudah dijalankan di database yang dipakai.

## Dokumentasi

| File | Isi |
| --- | --- |
| [`docs/API_REFERENCE.md`](docs/API_REFERENCE.md) | Daftar endpoint, payload, aturan validasi, status code |
| [`docs/PROJECT_OVERVIEW.md`](docs/PROJECT_OVERVIEW.md) | Tujuan, arsitektur, entitas database, variabel lingkungan |
| [`docs/PROGRESS.md`](docs/PROGRESS.md) | Status pengerjaan per fitur dan per tugas roadmap |
| [`docs/CONVENTIONS.md`](docs/CONVENTIONS.md) | Aturan penulisan kode dan pola yang dipakai |
| [`docs/RESQ_BACKEND_ROADMAP.md`](docs/RESQ_BACKEND_ROADMAP.md) | Rencana pengerjaan lanjutan dan spesifikasi endpoint baru |
| [`docs/DEPLOYMENT.md`](docs/DEPLOYMENT.md) | Panduan deploy ke hosting cloud (draf) |
