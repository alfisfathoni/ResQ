# ResQ Backend — API Reference

Base URL: `http://localhost:3000` (lihat `PORT` di `.env`).
Semua endpoint diawali `/api`. Endpoint terproteksi butuh header:

```
Authorization: Bearer <accessToken>
```

## Format Respons (selalu konsisten)

Semua respons melewati helper `utils/response.js`.

### Sukses standar (`sendSuccess`):
```json
{
  "status": "success",
  "success": true,
  "message": "...",
  "data": <payload-atau-null>
}
```

### Listing dengan paginasi standar (`sendPaginated`):
```json
{
  "status": "success",
  "success": true,
  "message": "...",
  "data": [...],
  "meta": {
    "page": 1,
    "limit": 10,
    "total": 42,
    "totalPages": 5
  }
}
```

### Gagal (`sendError`):
```json
{
  "success": false,
  "message": "...",
  "errors": <detail-atau-null>
}
```

- Field `data` **selalu ada** pada respons sukses (bernilai `null` jika tidak ada payload).
- Seluruh listing endpoint mendukung query parameters `page` (default: 1) dan `limit` (default: 10, batas maksimal: 50).
- Endpoint autentikasi publik dilindungi oleh rate limiting per IP (`express-rate-limit`, jendela 15 menit); melebihi kuota menghasilkan status **429**. Default: register, login, forgot/reset password maks **20** (`AUTH_RATE_LIMIT_MAX`); `resend-verification` maks **5** (`EMAIL_RATE_LIMIT_MAX`); `refresh` maks **100** (`REFRESH_RATE_LIMIT_MAX`). Semua dapat diatur lewat env.
- Header HTTP diamankan secara terpusat menggunakan middleware `helmet`.
- Log request HTTP dicatat menggunakan `morgan` (`dev` format pada development).
- Field `errors` berisi map `{ namaField: pesan }` saat validasi gagal; `null` untuk error umum.
- Error validasi menggunakan status **422** dengan `message: "Validation failed."`.
- Error tak terduga menggunakan status **500** dengan `message: "Something went wrong."`.

> Dokumen ini hanya mencakup endpoint yang **sudah terimplementasi** (diverifikasi baris per baris dari kode). Perbarui saat menambah endpoint baru.

### Penanganan error terpusat

Sejak Phase 2, penanganan error dipusatkan:
- Semua controller CRUD membungkus handler dengan **`asyncHandler`** (`utils/async-handler.js`). `ServiceError` yang dilempar service diteruskan ke **global error handler** (`middleware/error.middleware.js`, terdaftar paling akhir di `app.js`) yang memetakannya ke respons standar; error tak terduga → **500** generik tanpa membocorkan stack trace.
- `ServiceError` kini **satu definisi bersama** di `utils/service-error.js` (dipakai semua service).
- Pengecualian: modul **auth** masih memakai `try/catch` lokal di controllernya (perilaku respons tetap sama).

## Health Check

| Method | Path | Auth | Status | Keterangan |
| --- | --- | --- | --- | --- |
| GET | `/` | — | 200 | Cek apakah API hidup |

---

## Autentikasi — `/api/auth`

Rate limit per IP dalam jendela 15 menit (melebihi batas: status 429). Batas dapat diatur lewat env; angka di bawah adalah default:
- `POST /register`, `/login`, `/forgot-password`, `/reset-password`: maks **20** request (`AUTH_RATE_LIMIT_MAX`).
- `POST /resend-verification`: limiter ketat tersendiri, maks **5** request (`EMAIL_RATE_LIMIT_MAX`).
- `POST /refresh`: limiter terpisah yang lebih longgar, maks **100** request (`REFRESH_RATE_LIMIT_MAX`), dan tidak memakai jatah login.
- Di balik proxy hosting, set `TRUST_PROXY=1` agar IP klien terbaca benar (lihat `DEPLOYMENT.md`).

### POST `/register` — publik
Membuat akun baru (role default `USER`, `isVerified=false`) lalu mengirim email verifikasi.
- **Flag demo `AUTO_VERIFY_EMAIL`** (env, default `false`): bila `true`, akun langsung dibuat dengan `isVerified=true`, email verifikasi **tidak** dikirim, dan pesan respons menjadi `"Registration successful. You can now log in."`. Hanya untuk dev lokal / demo saat SMTP mati — **jangan aktifkan di produksi**.
Payload & aturan validasi:
- `fullName` — wajib, maks 255 karakter.
- `username` — wajib, 3-100 karakter, hanya huruf/angka/underscore/titik.
- `email` — wajib, format email valid, maks 255, disimpan lowercase.
- `phoneNumber` — wajib, pola `^\+?\d{7,15}$` (opsional `+`, 7-15 digit).
- `password` — wajib, 8-72 karakter.
- Sukses: **201**, `data` = user tanpa password (termasuk `role`, selalu `USER` untuk pendaftaran lewat API; akun `ADMIN` hanya dibuat lewat seed). Duplikat username/email/phone: **409** dengan `errors` per-field.

### POST `/login` — publik
- `identifier` — email **atau** username (juga menerima `email`/`username` sebagai alias). Wajib.
- `password` — wajib.
- Sukses: **200**, `data` = `{ accessToken, refreshToken, user }`.
- `user` memuat field **`role`** (`USER` atau `ADMIN`). Satu pintu login untuk user dan admin: FE membaca `data.user.role` untuk memilih halaman awal. Field `password` tidak pernah ikut. Contoh (dipersingkat):
  ```json
  {
    "success": true,
    "message": "Login successful.",
    "data": {
      "accessToken": "...",
      "refreshToken": "...",
      "user": { "id": "...", "fullName": "...", "username": "...", "email": "...", "role": "USER", "isVerified": true }
    }
  }
  ```
- Kredensial salah: **401** (`"Invalid credentials."`). Akun belum verifikasi: **403**.

### POST `/refresh` — publik
- `refreshToken` — wajib. Harus masih tersimpan di tabel `RefreshToken` (belum di-revoke).
- Sukses: **200**, `data` = `{ accessToken }` (refresh token tidak diganti).
- Token kadaluarsa/invalid/sudah dicabut: **401**.

### POST `/forgot-password` — publik
- `email` — wajib & valid.
- **Selalu** balas **200** dengan pesan generik (cegah enumerasi akun), apa pun hasilnya.
- Link di email mengarah ke halaman browser `APP_URL/reset-password?token=...` (lihat [Halaman Browser](#halaman-browser-dari-link-email)).

### POST `/resend-verification` — publik
Mengirim ulang email verifikasi untuk akun yang belum terverifikasi.
- `email` — wajib & valid (disimpan lowercase).
- Bila user ada **dan** `isVerified=false`: token verifikasi baru dibuat dan email dikirim. User tidak ada atau sudah terverifikasi: tidak ada aksi.
- **Selalu** balas **200** dengan pesan generik `"If an unverified account exists with that email, a new verification link has been sent."` (cegah enumerasi), termasuk bila SMTP gagal.
- Validasi gagal: **422**. Rate limit ketat: maks **5 request / 15 menit per IP**, lebih dari itu **429**.

### POST `/reset-password` — publik
- `resetToken` — wajib (JWT bertipe `password_reset`).
- `newPassword` — wajib, 8-72 karakter.
- Sukses: **200**; semua refresh token user dihapus (semua sesi logout). Token invalid/kadaluarsa: **400**.
- Endpoint ini juga dipanggil oleh form di halaman browser `GET /reset-password`.

### GET `/verify-email?token=...` — publik
- `token` — dari query string (JWT bertipe `email_verification`).
- Sukses: **200**, `data` = user terverifikasi. Sudah terverifikasi: **409**. Token invalid/kadaluarsa/kosong: **400**. User tidak ada: **404**.
- **Respons ganda (content negotiation via header `Accept`)**, status code sama di kedua format:
  - Browser (`Accept: text/html`, mis. user membuka link di email) → halaman **HTML** ramah: "Email verified" (200), "Already verified" (409), atau "Verification failed" (400/404/500).
  - Klien API (`Accept: application/json`, atau wildcard `*/*` seperti default Postman) → **JSON** standar (`sendSuccess`/`sendError`) seperti sebelumnya.

### GET `/profile` — butuh auth
- Sukses: **200**, `data` = profil terkini (dibaca dari DB, bukan dari token). Memuat field **`role`** (`USER` atau `ADMIN`) dan tidak pernah memuat `password`. `PUT /profile` mengembalikan bentuk yang sama.

### PUT `/profile` — butuh auth
Update parsial, minimal satu field:
- `fullName` — opsional, maks 255.
- `phoneNumber` — opsional, pola phone; ditolak **409** jika sudah dipakai user lain.
- `profilePicture` — opsional, string atau `null` (untuk menghapus).
- Tanpa field apa pun: **422**.

### DELETE `/profile` — butuh auth
- Hapus akun sendiri. Akun `ADMIN` ditolak **403**. Data terkait terhapus via cascade DB.

### POST `/logout` — butuh auth
- `refreshToken` — wajib. Mencabut token milik user tsb (idempoten, tetap **200** jika sudah tidak ada).

---

## Halaman Browser (dari link email)

Halaman HTML minimal (ramah HP) di root aplikasi, **bukan** di bawah `/api`. Header: `Cache-Control: no-store` dan CSP ketat (hanya script inline milik halaman via nonce, request hanya ke origin sendiri).

| Method | Path | Auth | Status | Keterangan |
| --- | --- | --- | --- | --- |
| GET | `/reset-password?token=...` | — | 200 | Form "New password" + "Confirm password". Saat submit, JavaScript halaman memanggil `POST /api/auth/reset-password` dengan `{ resetToken, newPassword }` lalu menampilkan pesan inline (sukses: "Password reset successfully. You can now log in from the app."; gagal: pesan dari API). Validasi di sisi halaman: 8-72 karakter dan kedua field harus sama. Token tidak dicek saat halaman dibuka, hanya saat submit. Tanpa `token` → pesan "link invalid" dan form disembunyikan. |

---

## Kontak Darurat — `/api/emergency-contacts` (semua butuh auth)

| Method | Path | Status sukses | Keterangan |
| --- | --- | --- | --- |
| POST | `/` | 201 | Tambah kontak |
| GET | `/` | 200 | Daftar kontak user (paginasi: `page`, `limit`), diurut `priority` naik |
| PUT | `/:id` | 200 | Update parsial |
| DELETE | `/:id` | 200 | Hapus kontak |

Aturan bisnis & validasi:
- `contactName` — wajib (create), maks 255.
- `phoneNumber` — wajib (create), pola `^\+?\d{7,15}$`, **unik di antara kontak milik user**.
- `priority` — wajib (create), integer **1-5**, **unik per user**.
- Maksimal **5 kontak per user** (melebihi → **400**).
- Konflik priority/phone → **409**. Kontak milik user lain atau tidak ada → **404**.
- PUT bersifat parsial: minimal satu dari `contactName`/`phoneNumber`/`priority`.

---

## Laporan Insiden — `/api/reports` (semua butuh auth)

| Method | Path | Status sukses | Keterangan |
| --- | --- | --- | --- |
| POST | `/` | 201 | Buat laporan |
| GET | `/` | 200 | Daftar laporan user (paginasi: `page`, `limit`), diurut `createdAt` terbaru dulu |
| GET | `/:id` | 200 | Detail satu laporan |
| PUT | `/:id` | 200 | Update parsial |
| DELETE | `/:id` | 200 | Hapus laporan (media ikut terhapus via cascade) |

Payload & validasi:
- `category` — wajib, salah satu enum: `CRIME, ACCIDENT, FIRE, MEDICAL, DISASTER, SUSPICIOUS, OTHER`.
- `title` — wajib, maks 255.
- `description` — opsional, string atau `null`.
- `latitude` — wajib, angka/numeric string dalam rentang **-90..90**.
- `longitude` — wajib, angka/numeric string dalam rentang **-180..180**.
- Koordinat dikembalikan sebagai **number** (bukan string Decimal).
- Laporan milik user lain atau tidak ada → **404** (tidak membocorkan keberadaan data).
- PUT bersifat parsial: minimal satu field.

---

## Media Laporan — `/api/reports/:reportId/media` (semua butuh auth)

Upload via `multipart/form-data`, field bernama `file`.

| Method | Path | Status sukses | Keterangan |
| --- | --- | --- | --- |
| POST | `/` | 201 | Upload 1 media ke Cloudinary |
| GET | `/` | 200 | Daftar media laporan (paginasi: `page`, `limit`), diurut `createdAt` terlama dulu |
| DELETE | `/:mediaId` | 200 | Hapus 1 media |

Aturan:
- Hanya tipe `image/*` dan `video/*`; maksimal **20 MB**. Pelanggaran → **422**.
- `mediaType` di DB (`IMAGE`/`VIDEO`) ditentukan otomatis dari MIME file.
- Upload gagal ke Cloudinary → **502**.
- Saat hapus: asset Cloudinary dihapus best-effort (kegagalan tidak membatalkan hapus baris DB), lalu baris DB dihapus.
- Laporan bukan milik user → **404**. Media tidak ada / bukan milik laporan tsb → **404**.

---

## Panduan — `/api/guides`

Konten panduan keselamatan yang dikelola admin. **Baca bersifat publik; tulis hanya admin** (`authenticate` + `isAdmin`).

| Method | Path | Auth | Status sukses | Keterangan |
| --- | --- | --- | --- | --- |
| GET | `/` | — (publik) | 200 | Daftar semua panduan (paginasi: `page`, `limit`), diurut `createdAt` terbaru dulu |
| GET | `/:id` | — (publik) | 200 | Detail satu panduan |
| POST | `/` | admin | 201 | Buat panduan |
| PUT | `/:id` | admin | 200 | Update parsial |
| DELETE | `/:id` | admin | 200 | Hapus panduan |

Payload & validasi (create):
- `title` — wajib, maks 255.
- `category` — wajib, salah satu enum `GuideCategory`: `SAFETY, FIRST_AID, DISASTER, EMERGENCY, LEGAL`.
- `content` — wajib, string.
- `thumbnail` — opsional, string atau `null`.
- PUT bersifat parsial: minimal satu field; panduan tidak ada → **404**.
- Non-admin yang mengakses route tulis → **403**; tanpa token → **401**.

---

## Pertemanan — `/api/friendships` (semua butuh auth)

| Method | Path | Status sukses | Keterangan |
| --- | --- | --- | --- |
| POST | `/request` | 201 | Kirim permintaan pertemanan (`body.addresseeId` wajib) |
| GET | `/` | 200 | Daftar teman (status `ACCEPTED`, paginasi: `page`, `limit`), diurut `updatedAt` terbaru dulu |
| GET | `/requests` | 200 | Daftar permintaan masuk (`PENDING`, paginasi: `page`, `limit`), terbaru dulu |
| PATCH | `/:id/accept` | 200 | Terima permintaan (hanya addressee) |
| PATCH | `/:id/reject` | 200 | Tolak permintaan (hanya addressee) |
| DELETE | `/:id` | 200 | Hapus (unfriend) pertemanan `ACCEPTED` yang melibatkan user |

Aturan bisnis & validasi:
- `addresseeId` — wajib (POST `/request`).
- Tidak bisa berteman dengan diri sendiri → **400**.
- User target tidak ada → **404**.
- Sudah berteman (`ACCEPTED`) atau sudah ada permintaan (`PENDING`) di arah mana pun → **409**.
- Baris `REJECTED` di arah yang sama akan "dihidupkan" kembali menjadi `PENDING`.
- Accept/reject: hanya addressee; bukan addressee atau tidak ada → **404**; bukan `PENDING` lagi → **409**.
- DELETE: baris tidak ada / bukan `ACCEPTED` / tidak melibatkan user → **404**.
- Respons menyertakan preview user (`requester`/`addressee`: id, fullName, username, profilePicture).
- Efek samping: kirim permintaan → notifikasi `FRIEND_REQUEST`; accept → notifikasi `FRIEND_ACCEPTED` (best-effort, tidak membatalkan operasi utama).

---

## Notifikasi — `/api/notifications` (semua butuh auth)

| Method | Path | Status sukses | Keterangan |
| --- | --- | --- | --- |
| GET | `/` | 200 | Daftar notifikasi user (paginasi: `page`, `limit`), terbaru dulu (termasuk preview `sender`) |
| GET | `/unread-count` | 200 | `data = { count }` jumlah belum dibaca |
| PATCH | `/read-all` | 200 | Tandai semua sebagai dibaca; `data = { updated }` |
| PATCH | `/:id/read` | 200 | Tandai satu sebagai dibaca (idempoten jika sudah dibaca) |
| DELETE | `/:id` | 200 | Hapus satu notifikasi |

Aturan:
- `:id` wajib (route param). Notifikasi tidak ada / milik user lain → **404**.
- Notifikasi dibuat oleh event bisnis (pertemanan, sharing session), bukan oleh endpoint create langsung.
- Route literal (`/unread-count`, `/read-all`) dideklarasikan sebelum route `/:id`.

---

## Sesi Berbagi Lokasi — `/api/sharing-sessions` (semua butuh auth)

Model data: satu baris `SharingSession` per pasangan (owner, viewer). Satu "sesi" owner = sekumpulan baris dengan `startedAt` sama — satu baris per teman (`ACCEPTED`).

| Method | Path | Status sukses | Keterangan |
| --- | --- | --- | --- |
| POST | `/` | 201 | Mulai sesi: berbagi lokasi ke semua teman `ACCEPTED`; `data` = array baris sesi aktif |
| GET | `/active` | 200 | Daftar sesi aktif (paginasi: `page`, `limit`) di mana user adalah owner atau viewer |
| GET | `/:id` | 200 | Detail satu baris sesi (hanya owner atau viewer-nya) |
| PATCH | `/:id/stop` | 200 | Hentikan sesi (hanya owner); menonaktifkan seluruh grup sesi |

Aturan:
- Body boleh kosong; jika dikirim harus berupa objek JSON (bukan array/nilai lain) → jika tidak **422**.
- Sudah punya sesi aktif → **409**. Tidak punya teman `ACCEPTED` → **400**.
- Detail: bukan owner/viewer atau tidak ada → **404**.
- Stop: bukan owner atau tidak ada → **404**; sesi sudah berhenti → **409**.
- Respons menyertakan preview `owner` & `viewer`.
- Efek samping: memulai sesi mengirim notifikasi `SHARE_SESSION` ke tiap teman (best-effort).

---

## Utilitas User — `/api/users` (semua butuh auth)

Hanya mengembalikan field publik (`id, fullName, username, profilePicture, createdAt`). Field sensitif (password, email, phone, role, isVerified, lokasi) tidak pernah dikembalikan.

| Method | Path | Status sukses | Keterangan |
| --- | --- | --- | --- |
| GET | `/search?q=<kata>` | 200 | Cari user by `username`/`fullName` (case-insensitive, paginasi: `page`, `limit`), kecuali diri sendiri; diurut `username` |
| GET | `/:id` | 200 | Profil publik + `friendshipStatus` relatif terhadap user |
| GET | `/:id/friends` | 200 | Daftar teman `ACCEPTED` user tsb (paginasi: `page`, `limit`), diurut `username` |

Aturan:
- `q` — wajib & non-kosong → jika tidak **422**.
- `friendshipStatus` bernilai salah satu: `FRIENDS`, `PENDING_SENT`, `PENDING_RECEIVED`, `NONE` (status `REJECTED` disembunyikan sebagai `NONE`).
- User tidak ada (detail / friends) → **404**.
- Route literal `/search` dideklarasikan sebelum route `/:id`.

---

## Belum Tersedia (rencana)

- **Swagger / OpenAPI interaktif** belum ada (tidak ada dependency maupun setup di `app.js`). Dokumentasi endpoint saat ini mengandalkan file ini. Lihat `PROGRESS.md` (Phase 2 — Item 3).
- Semua model lama sudah memiliki API; SosAlert, Facility, ReportLike, ReportComment sudah ada di skema dan akan mendapat API di tugas roadmap 1.3, 2.1, dan 3.1.
