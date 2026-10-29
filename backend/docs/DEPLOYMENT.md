# ResQ Backend — Panduan Deploy

> **Status: DRAF.** Deploy ditunda sampai mendekati demo. Platform final belum dikunci (rencana: Railway). Beberapa platform gratis (Render, Railway) memblokir SMTP, sehingga email di hosting perlu memakai API HTTPS penyedia email. Dokumen ini akan diperbarui saat deploy dilakukan.

Panduan ini untuk menjalankan backend ResQ di hosting cloud supaya bisa diakses lewat HTTPS dari HP dan aplikasi Android. Ditulis untuk yang baru pertama kali deploy: ikuti urutannya dari atas ke bawah.

> **Status:** tugas roadmap 0.8 (T5). Perubahan kode untuk produksi sudah ada di repo. **Deploy itu sendiri belum dilakukan**; isi URL layanan di `PROGRESS.md` setelah layanan hidup dan checklist di bagian 5 lolos.

> **Catatan penting tentang harga, kuota, dan tampilan dashboard:** semuanya bisa berubah. Selalu cek dokumentasi resmi platform sebelum memutuskan. Nama menu di dashboard bisa sedikit berbeda dari yang ditulis di sini; cari padanan istilahnya.

---

## 1. Gambaran Arsitektur

```
Aplikasi Android ──HTTPS──▶ Backend ResQ (hosting) ──▶ PostgreSQL (Neon)
                                   │
                                   ├──▶ Cloudinary (foto/video laporan)
                                   └──▶ Server SMTP (email verifikasi & reset password)
```

- **Satu database Neon yang sama** dipakai oleh backend di laptop (lokal) dan backend di hosting. Artinya data uji di lokal akan terlihat di hosting, dan sebaliknya. Hati-hati saat mencoba hal yang menghapus data.
- Backend **tidak menyimpan file di disk**: upload foto/video diproses di memori lalu dikirim ke Cloudinary, jadi tidak butuh penyimpanan persisten di hosting.
- Halaman reset password (`/reset-password`) disajikan oleh backend sendiri, jadi `APP_URL` harus menunjuk ke URL publik backend.

> **Peringatan SMTP (baca sebelum memilih paket hosting):** banyak platform **memblokir koneksi SMTP keluar pada paket gratis**. Pada saat panduan ini ditulis: Render memblokir port 25, 465, dan 587 untuk web service gratis ([changelog Render](https://render.com/changelog/free-web-services-will-no-longer-allow-outbound-traffic-to-smtp-ports)), dan Railway menonaktifkan SMTP pada paket Free, Trial, dan Hobby ([dokumentasi Railway](https://docs.railway.com/reference/outbound-networking)). Backend ResQ mengirim email lewat SMTP, jadi **Gmail SMTP (port 465/587) tidak akan jalan di paket gratis tersebut**. **Rekomendasi utama: pakai relay SMTP dari penyedia email transaksional (mis. Brevo atau Mailgun) di port 2525**, yang tidak ikut diblokir. Langkah lengkapnya ada di bagian 4 ("Email (SMTP relay, port 2525)"); opsi cadangan ada di bagian 6. Periksa kebijakan terbaru platform dan penyedia karena bisa berubah.

---

## 2. Rekomendasi Platform

### 2.1 Render (Web Service): rekomendasi utama

Alasan: bisa deploy langsung dari GitHub, memberi URL HTTPS otomatis, dan punya paket gratis untuk percobaan.

Hal yang perlu diketahui tentang paket gratis (**periksa harga dan kuota terbaru di situs resmi karena dapat berubah**):

- **Layanan tidur** setelah beberapa waktu tanpa trafik. Request pertama setelah tidur lambat (cold start; bisa sampai satu menit atau lebih) karena layanan dinyalakan lagi.
- **SMTP keluar di port 25, 465, dan 587 diblokir** (lihat peringatan di bagian 1). Gunakan relay SMTP di port 2525 (bagian 4).
- Ada batas jam pemakaian dan sumber daya per bulan.

**Cara menangani layanan tidur saat demo:** buka `<BASE_URL>/` di browser atau jalankan `curl <BASE_URL>/` sekitar 5-10 menit sebelum demo, tunggu sampai respons JSON muncul, lalu coba satu login dari aplikasi. Setelah itu layanan akan terasa normal selama masih ada trafik. Jangan mencoba memanggil layanan terus-menerus secara otomatis sebelum membaca aturan penggunaan platform.

### 2.2 Alternatif: Railway (ringkas)

1. Buat project baru, pilih deploy dari repo GitHub.
2. Atur **Root Directory** ke `backend`, build dengan `npm ci`, start dengan `npm start` (sama seperti Render, lihat bagian 3).
3. Isi variabel lingkungan di menu *Variables* (bagian 4).
4. Buat domain publik untuk layanan (menu *Networking* / *Domains*), lalu isi `APP_URL` dengan URL itu dan redeploy.
5. Cek paket dan kuota di situs resmi. SMTP keluar dinonaktifkan di paket Free, Trial, dan Hobby, jadi relay SMTP di port 2525 mungkin juga tidak lolos di sana. Periksa dokumentasi Railway; bila tetap diblokir, pakai opsi cadangan di bagian 6.

Selain dua ini, hosting lain (VPS, Fly.io, dll.) juga bisa, asalkan mendukung Node.js, HTTPS, dan variabel lingkungan.

---

## 3. Pengaturan Layanan (Render)

Di dashboard Render: **New → Web Service**, hubungkan akun GitHub, pilih repo ResQ. Isi:

| Pengaturan | Nilai | Keterangan |
|---|---|---|
| Source / Repository | repo GitHub ResQ | Repo berisi folder `backend/` dan `frontend/` |
| **Root Directory** | `backend` | **Wajib.** Tanpa ini Render membangun dari akar repo dan gagal menemukan `package.json` |
| Runtime / Language | Node | |
| **Branch** | `feat/T5-deploy-hardening` | Untuk uji pertama. **Setelah PR ter-merge, ganti ke branch dasar** (mis. `backend/main`; sesuaikan dengan branch yang dipakai tim) di *Settings → Build & Deploy* |
| **Build Command** | `npm ci` | Otomatis menjalankan `prisma generate` lewat `postinstall` |
| **Start Command** | `npm start` | Menjalankan `prisma migrate deploy` lalu server (lihat catatan di bawah) |
| **Health Check Path** | `/` | `GET /` ringan dan tidak menyentuh database |
| Instance type | Free untuk percobaan | SMTP port 25/465/587 diblokir di paket gratis; pakai relay port 2525 (bagian 4) |
| Environment Variables | lihat bagian 4 | |

Catatan:
- **Port:** jangan atur `PORT` sendiri. Render mengisinya otomatis dan backend membaca `process.env.PORT`.
- **Versi Node:** `package.json` meminta `node >=22` (`engines.node`). Platform memakai field itu untuk memilih versi Node. Bila build gagal karena versi, atur versi Node di platform (Render: variabel `NODE_VERSION`; periksa dokumentasi) sesuai kebutuhan.
- **Migrasi database:** setiap kali layanan start, `npm start` menjalankan `prisma migrate deploy` ke database Neon yang **dipakai bersama**. Perintah ini hanya menerapkan migrasi yang belum terpasang dan tidak menghapus data. Jangan pernah menjalankan `prisma migrate reset` atau `prisma db push` ke database ini.
- `devDependencies` (nodemon) tidak dibutuhkan di hosting; paket `prisma` sudah ada di `dependencies`.

---

## 4. Variabel Lingkungan di Hosting

Isi di menu *Environment* layanan (nama saja di sini; **jangan** menaruh nilai rahasia di repo atau dokumen). Template lengkap ada di `.env.example`.

### Server

| Variabel | Status | Nilai / catatan |
|---|---|---|
| `NODE_ENV` | wajib | `production` |
| `APP_URL` | wajib | URL publik HTTPS layanan, tanpa garis miring di akhir, mis. `https://nama-layanan.onrender.com`. Diisi **setelah URL pertama diketahui**, lalu redeploy. Dipakai untuk link di email |
| `TRUST_PROXY` | wajib di hosting | `1` (satu proxy di depan server). Tanpa ini semua pengguna tampak punya satu IP dan rate limit salah |
| `PORT` | jangan diisi | Diisi otomatis oleh platform |

### Database

| Variabel | Status | Nilai / catatan |
|---|---|---|
| `DATABASE_URL` | wajib | Connection string Neon yang sama dengan lokal. **Harus memuat `sslmode=require`** |

### JWT dan hashing

| Variabel | Status | Nilai / catatan |
|---|---|---|
| `JWT_SECRET` | wajib | Acak, berbeda dari tiga lainnya |
| `JWT_REFRESH_SECRET` | wajib | |
| `JWT_EMAIL_VERIFICATION_SECRET` | wajib | |
| `JWT_PASSWORD_RESET_SECRET` | wajib | |
| `JWT_EXPIRES_IN`, `JWT_REFRESH_EXPIRES_IN`, `JWT_EMAIL_VERIFICATION_EXPIRES_IN`, `JWT_PASSWORD_RESET_EXPIRES_IN` | opsional | Default `15m`, `7d`, `1d`, `1h` |
| `BCRYPT_SALT_ROUNDS` | opsional | Default `10` |

> Memakai secret JWT yang **sama** dengan lokal berarti token dari lokal juga sah di hosting (karena database-nya sama). Itu praktis untuk demo. Bila ingin memisahkan, pakai secret baru di hosting; konsekuensinya semua pengguna harus login ulang.

### Cloudinary

| Variabel | Status | Nilai / catatan |
|---|---|---|
| `CLOUDINARY_CLOUD_NAME` | wajib bila memakai upload media | |
| `CLOUDINARY_API_KEY` | wajib bila memakai upload media | |
| `CLOUDINARY_API_SECRET` | wajib bila memakai upload media | |
| `CLOUDINARY_REPORT_FOLDER` | opsional | Default `resq/reports` |

### Email (SMTP relay, port 2525)

**Rekomendasi utama:** pakai relay SMTP dari penyedia email transaksional di **port 2525**, misalnya Brevo atau Mailgun. Port 2525 bukan port standar resmi, tetapi lazim dipakai sebagai alternatif port 587 dan tidak termasuk port yang diblokir Render pada paket gratis (25, 465, 587). Backend mendukungnya tanpa perubahan kode: port selain 465 memakai STARTTLS.

| Variabel | Status | Nilai / catatan |
|---|---|---|
| `SMTP_HOST` | wajib bila email dipakai | Host relay penyedia. Brevo: `smtp-relay.brevo.com`. Mailgun: `smtp.mailgun.org` (region EU: `smtp.eu.mailgun.org`) |
| `SMTP_PORT` | **`2525`** | Default kode adalah `587` (port `465` memakai TLS implisit), jadi **wajib diisi `2525`** di hosting yang memblokir 587 |
| `SMTP_USER` | wajib bila email dipakai | Username SMTP dari penyedia. Brevo: *SMTP login* (bukan email akun dan bukan nama host). Mailgun: kredensial SMTP per domain (mis. `postmaster@domain-anda`) |
| `SMTP_PASS` | wajib bila email dipakai | Password/kunci SMTP dari penyedia (Brevo: *SMTP key*, bukan API key dan bukan password akun). Rahasia: hanya diisi di dashboard hosting |
| `SMTP_FROM` | **sangat disarankan** | Alamat pengirim yang **sudah diverifikasi** di penyedia (single sender atau domain terverifikasi). Bila kosong, kode memakai `SMTP_USER`, yang pada relay biasanya bukan alamat email valid sehingga email ditolak atau masuk spam |

Cara menyiapkan (garis besar; menu penyedia bisa berubah):
1. Buat akun di penyedia (Brevo atau Mailgun) dan **verifikasi alamat pengirim atau domain** yang akan dipakai di `SMTP_FROM`.
2. Buat kredensial SMTP di dashboard penyedia, lalu salin host, username, dan password ke variabel di atas.
3. Isi `SMTP_PORT=2525` dan `SMTP_FROM` di hosting, lalu redeploy.
4. Uji dengan checklist 5.2 butir 6 dan cek folder spam.

> **Periksa dokumentasi penyedia untuk port dan kuota terbaru.** Port yang didukung, batas kiriman harian atau bulanan, dan syarat paket gratis (mis. pembatasan penerima atau domain) dapat berubah. Referensi: [Brevo SMTP](https://help.brevo.com/hc/en-us/articles/7924908994450-Send-transactional-emails-using-Brevo-SMTP), [Mailgun SMTP](https://documentation.mailgun.com/docs/mailgun/user-manual/sending-messages/send-smtp).

**Gmail SMTP tidak cocok di paket gratis Render.** Gmail hanya menyediakan port 465 dan 587, yang diblokir pada web service gratis Render (dan SMTP dinonaktifkan di paket Free/Trial/Hobby Railway). Gmail tetap bisa dipakai saat menjalankan backend **di laptop** (`SMTP_HOST=smtp.gmail.com`, `SMTP_PORT=587`, `SMTP_PASS` berupa *App Password* dengan verifikasi 2 langkah aktif).

Opsi cadangan bila relay 2525 tidak bisa dipakai: instance berbayar yang mengizinkan SMTP, pengiriman email lewat API HTTPS penyedia (tugas lanjutan, butuh perubahan kode), atau `AUTO_VERIFY_EMAIL=true` khusus demo. Rinciannya di bagian 6.

### Demo dan rate limit

| Variabel | Status | Nilai / catatan |
|---|---|---|
| `AUTO_VERIFY_EMAIL` | opsional | Default `false`. `true` = akun baru langsung terverifikasi dan email verifikasi tidak dikirim. **Hanya untuk demo** bila SMTP tidak bisa dipakai; matikan lagi sesudahnya |
| `AUTH_RATE_LIMIT_MAX` | opsional | Default `20` per IP per 15 menit (login, register, forgot/reset password). Naikkan (mis. `100`) bila banyak HP berbagi satu Wi-Fi saat demo |
| `EMAIL_RATE_LIMIT_MAX` | opsional | Default `5` per IP per 15 menit (`resend-verification`) |
| `REFRESH_RATE_LIMIT_MAX` | opsional | Default `100` per IP per 15 menit (`POST /api/auth/refresh`, dipanggil otomatis aplikasi) |

### Yang tidak diisi di hosting

- **`ADMIN_*` dan `SEED_*` tidak diisi di hosting.** Seed hanya dijalankan **dari laptop** (`npm run seed`) ke database Neon yang sama, jadi password admin tidak perlu tersimpan di hosting.
- Backend tidak membaca variabel tersebut saat berjalan.

---

## 5. Urutan Deploy Pertama dan Verifikasi

### 5.1 Urutan

1. **Pastikan database siap.** Database Neon sudah berisi migrasi dan seed (admin + panduan) dari pekerjaan lokal. Bila belum, dari laptop: `npx prisma migrate deploy` lalu `npm run seed` (seed idempoten dan hanya menambah data). Jangan jalankan seed dari hosting.
2. **Push branch** `feat/T5-deploy-hardening` ke GitHub.
3. **Buat Web Service** di Render dengan pengaturan di bagian 3.
4. **Isi variabel lingkungan** di bagian 4, termasuk relay SMTP port 2525 (`SMTP_HOST`, `SMTP_PORT=2525`, `SMTP_USER`, `SMTP_PASS`, `SMTP_FROM`) yang alamat pengirimnya sudah diverifikasi di penyedia. Untuk sementara `APP_URL` boleh diisi tebakan atau dikosongkan; akan diperbaiki di langkah 6.
5. **Deploy.** Tunggu build selesai dan di log muncul `ResQ Backend API is running on port ...` dan `Database connection OK`. Log `No pending migrations to apply` berarti database sudah mutakhir.
6. **Catat URL publik** layanan (HTTPS). Isi `APP_URL` dengan URL itu, simpan, lalu **redeploy**.
7. Jalankan **checklist verifikasi** di 5.2.
8. Setelah PR ter-merge, **ganti branch** layanan ke branch dasar. Bagikan `<BASE_URL>` ke tim FE.

### 5.2 Checklist verifikasi pasca-deploy

Ganti `<BASE_URL>` dengan URL layanan (mis. `https://nama-layanan.onrender.com`). Jika layanan baru tidur, request pertama bisa lambat; tunggu saja.

1. **Health check**
   ```bash
   curl -i <BASE_URL>/
   ```
   Harapan: status `200` dan `{"success":true,"message":"ResQ Backend API is running"}`.

2. **Login admin hasil seed** (ganti dengan kredensial admin dari seed; ketik langsung di terminal tanpa menyimpannya di file atau repo)
   ```bash
   curl -s -X POST <BASE_URL>/api/auth/login \
     -H "Content-Type: application/json" \
     -d '{"identifier":"<ADMIN_EMAIL>","password":"<ADMIN_PASSWORD>"}'
   ```
   Harapan: status `200`, `data.user.role` bernilai `ADMIN`, dan ada access token.

3. **Panduan keselamatan (publik)**
   ```bash
   curl -s "<BASE_URL>/api/guides?limit=3"
   ```
   Harapan: `success: true` dan daftar panduan hasil seed, dengan `meta` paginasi.

4. **Header keamanan Helmet**
   ```bash
   curl -sI <BASE_URL>/
   ```
   Harapan: ada antara lain `Strict-Transport-Security`, `X-Content-Type-Options: nosniff`, `Content-Security-Policy`, dan `Referrer-Policy`.

5. **Halaman reset password terbuka**
   ```bash
   curl -i "<BASE_URL>/reset-password?token=abc"
   ```
   Harapan: status `200` dan isi HTML ("Reset your password"). Buka juga di browser HP.

6. **`resend-verification` dua kali dengan email yang sama** (gunakan email akun uji yang belum terverifikasi, atau email sembarang)
   ```bash
   curl -s -X POST <BASE_URL>/api/auth/resend-verification \
     -H "Content-Type: application/json" \
     -d '{"email":"<EMAIL_UJI>"}'
   ```
   Jalankan dua kali. Harapan: keduanya `200` dengan pesan generik yang sama (respons tidak membocorkan apakah email terdaftar). Pada akun yang belum terverifikasi, email masuk bila SMTP berfungsi. Batas default 5 kali per 15 menit per IP; setelah itu `429`.

7. **Tes dari HP**
   - Browser HP: buka `<BASE_URL>/` dan `<BASE_URL>/reset-password?token=abc` lewat HTTPS (tidak ada peringatan sertifikat).
   - Aplikasi: arahkan base URL aplikasi ke `<BASE_URL>`, lalu coba login (dan register bila email sudah berfungsi).

8. **Pastikan token tidak muncul di log:** buka `<BASE_URL>/api/auth/verify-email?token=abc123`, lalu cari barisnya di log hosting. Harapan: `token=[REDACTED]`, bukan `abc123`.

---

## 6. Pemecahan Masalah

**Aplikasi gagal start (deploy berstatus failed).**
Lihat log awal. Pesan `Server cannot start: required environment variables are missing or empty` diikuti daftar nama variabel berarti variabel wajib belum diisi: `DATABASE_URL`, `JWT_SECRET`, `JWT_REFRESH_SECRET`, `JWT_EMAIL_VERIFICATION_SECRET`, `JWT_PASSWORD_RESET_SECRET`. Isi di menu *Environment*, lalu redeploy. Penyebab lain: *Root Directory* belum `backend` (error `package.json` tidak ditemukan), atau versi Node tidak sesuai `engines.node`.

**Koneksi database gagal.**
Cek `DATABASE_URL` memuat `sslmode=require` dan tidak ada spasi atau tanda kutip yang ikut tersalin. Pastikan database Neon aktif (Neon bisa "tidur" dan butuh beberapa detik untuk bangun). Pesan alasan kegagalan ada di log baris `Reason:` dan `prisma migrate deploy`.

**Email tidak terkirim.**
Backend sengaja tetap membalas sukses walau email gagal (supaya tidak membocorkan status akun), jadi **periksa log server** untuk baris `Failed to send verification email:` atau `Failed to send password reset email:`. Penyebab umum:
- *Port SMTP diblokir oleh hosting.* Gejalanya di log biasanya timeout atau connection error. Pada Render gratis port 25, 465, dan 587 diblokir, sehingga **Gmail SMTP (465/587) tidak bisa dipakai di sana**; pada Railway paket Free/Trial/Hobby SMTP dinonaktifkan. Urutan penanganan:
  1. **Utama: relay SMTP di port 2525** (Brevo atau Mailgun). Isi `SMTP_HOST`, `SMTP_PORT=2525`, `SMTP_USER`, `SMTP_PASS`, dan `SMTP_FROM` (alamat pengirim yang sudah diverifikasi) seperti di bagian 4, lalu redeploy. Periksa dokumentasi penyedia untuk port dan kuota terbaru.
  2. **Cadangan, instance berbayar:** upgrade ke instance yang mengizinkan SMTP keluar (cek dokumentasi dan harga terbaru platform), lalu SMTP di port 587 dapat dipakai.
  3. **Cadangan, API HTTPS (tugas lanjutan):** mengirim email lewat API HTTPS penyedia email, bukan SMTP. Ini membutuhkan perubahan kode di `utils/mailer.js` dan belum termasuk dalam tugas ini.
  4. **Cadangan, khusus demo:** set `AUTO_VERIFY_EMAIL=true` supaya registrasi tidak membutuhkan email verifikasi. Ini **tidak** memperbaiki email reset password, dan harus dimatikan lagi setelah demo.
- *Relay ditolak (error autentikasi atau pengirim).* Pastikan `SMTP_USER` adalah username SMTP dari penyedia (bukan nama host atau email akun, terutama di Brevo), `SMTP_PASS` adalah kunci/password SMTP (bukan API key), dan `SMTP_FROM` adalah alamat atau domain pengirim yang sudah diverifikasi di penyedia.
- *Gmail (hanya saat dijalankan di laptop):* butuh verifikasi 2 langkah aktif dan **App Password** (16 karakter) sebagai `SMTP_PASS`, bukan password akun; `SMTP_HOST=smtp.gmail.com`, `SMTP_PORT=587`.
- *Email masuk ke folder spam.* Periksa folder spam atau promosi.
- *Link di email salah* (menunjuk `localhost`): lihat butir `APP_URL` di bawah.

**Error `429 Too many ...`.**
Rate limit terlampaui. Tunggu 15 menit, atau naikkan batas lewat `AUTH_RATE_LIMIT_MAX`, `EMAIL_RATE_LIMIT_MAX`, `REFRESH_RATE_LIMIT_MAX` lalu redeploy. Bila `429` muncul terlalu cepat padahal baru satu pengguna yang mencoba, pastikan `TRUST_PROXY=1` terisi: tanpa itu semua pengguna dianggap satu IP (IP proxy).

**Request pertama sangat lambat.**
Itu *cold start*: layanan di paket gratis tidur saat sepi. Bangunkan sebelum demo (bagian 2.1).

**Halaman reset menampilkan atau email berisi URL `localhost`.**
`APP_URL` belum diatur di hosting, atau masih `http://localhost:3000`. Isi dengan URL publik HTTPS layanan, lalu redeploy. Email yang sudah terlanjur terkirim tetap memuat link lama; minta email baru.

**Log menampilkan `ERR_ERL_UNEXPECTED_X_FORWARDED_FOR`.**
Server berada di balik proxy tetapi `TRUST_PROXY` belum diisi. Isi `TRUST_PROXY=1`.

**Log menampilkan `ERR_ERL_PERMISSIVE_TRUST_PROXY`.**
`TRUST_PROXY=true` mempercayai semua hop sehingga IP bisa dipalsukan. Ganti dengan angka (`1`).

---

## 7. Operasional dan Keamanan

**Melihat log.** Di dashboard layanan buka menu *Logs*. Di sana terlihat log start, request (nilai `token` pada URL sudah disamarkan menjadi `[REDACTED]`), dan error.

**Redeploy.** Setiap push ke branch yang dipantau memicu deploy otomatis (bila opsi auto-deploy aktif). Mengubah variabel lingkungan juga perlu redeploy agar nilainya terbaca. Ada juga tombol deploy manual (mis. *Manual Deploy* di Render).

**Rollback.** Buka daftar deploy sebelumnya (*Events* / *Deploys*) dan pilih deploy lama yang berhasil untuk dijalankan ulang (di Render: *Rollback*). Rollback hanya mengembalikan **kode**, bukan database. Migrasi yang sudah terpasang di Neon tidak ikut dibatalkan, jadi hindari rollback melewati perubahan skema tanpa memahami akibatnya.

**Keamanan.**
- Jangan menaruh secret (`.env`, password, API key, JWT secret) di repo, issue, atau chat. Repo akhir bersifat publik. `.env` sudah di-ignore oleh Git.
- Bila secret pernah tidak sengaja ter-commit, anggap bocor: ganti nilainya di penyedia (Neon, Cloudinary, SMTP) dan di hosting.
- **Matikan `AUTO_VERIFY_EMAIL`** (`false` atau hapus) kecuali memang dibutuhkan untuk demo, karena ia melewati verifikasi email.
- Database dipakai bersama lokal dan hosting: jangan jalankan `prisma migrate reset`, `prisma db push`, atau perintah yang menghapus data.
- Gunakan password admin khusus demo, dan jangan menuliskannya di repo (lihat catatan roadmap bagian 7).
- Gunakan hanya URL HTTPS di aplikasi dan dokumen untuk pengguna.
