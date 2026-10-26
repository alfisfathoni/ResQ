# ResQ Backend — API Reference

Base URL: `http://localhost:3000` (lihat `PORT` di `.env`).
Semua endpoint diawali `/api`. Endpoint terproteksi butuh header:

```
Authorization: Bearer <accessToken>
```

## Format Respons (selalu konsisten)

Semua respons melewati helper `utils/response.js`.

Sukses (`sendSuccess`):
```json
{ "success": true, "message": "...", "data": <payload-atau-null> }
```

Gagal (`sendError`):
```json
{ "success": false, "message": "...", "errors": <detail-atau-null> }
```

- Field `data` **selalu ada** pada respons sukses (bernilai `null` jika tidak ada payload).
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

### POST `/register` — publik
Membuat akun baru (role default `USER`, `isVerified=false`) lalu mengirim email verifikasi.
Payload & aturan validasi:
- `fullName` — wajib, maks 255 karакter.
- `username` — wajib, 3-100 karakter, hanya huruf/angka/underscore/titik.
- `email` — wajib, format email valid, maks 255, disimpan lowercase.
- `phoneNumber` — wajib, pola `^\+?\d{7,15}$` (opsional `+`, 7-15 digit).
- `password` — wajib, 8-72 karakter.
- Sukses: **201**, `data` = user tanpa password. Duplikat username/email/phone: **409** dengan `errors` per-field.

### POST `/login` — publik
- `identifier` — email **atau** username (juga menerima `email`/`username` sebagai alias). Wajib.
- `password` — wajib.
- Sukses: **200**, `data` = `{ accessToken, refreshToken, user }`.
- Kredensial salah: **401** (`"Invalid credentials."`). Akun belum verifikasi: **403**.

### POST `/refresh` — publik
- `refreshToken` — wajib. Harus masih tersimpan di tabel `RefreshToken` (belum di-revoke).
- Sukses: **200**, `data` = `{ accessToken }` (refresh token tidak diganti).
- Token kadaluarsa/invalid/sudah dicabut: **401**.

### POST `/forgot-password` — publik
- `email` — wajib & valid.
- **Selalu** balas **200** dengan pesan generik (cegah enumerasi akun), apa pun hasilnya.

### POST `/reset-password` — publik
- `resetToken` — wajib (JWT bertipe `password_reset`).
- `newPassword` — wajib, 8-72 karakter.
- Sukses: **200**; semua refresh token user dihapus (semua sesi logout). Token invalid/kadaluarsa: **400**.

### GET `/verify-email?token=...` — publik
- `token` — dari query string (JWT bertipe `email_verification`).
- Sukses: **200**, `data` = user terverifikasi. Sudah terverifikasi: **409**. Token invalid/kadaluarsa: **400**.

### GET `/profile` — butuh auth
- Sukses: **200**, `data` = profil terkini (dibaca dari DB, bukan dari token).

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

## Kontak Darurat — `/api/emergency-contacts` (semua butuh auth)

| Method | Path | Status sukses | Keterangan |
| --- | --- | --- | --- |
| POST | `/` | 201 | Tambah kontak |
| GET | `/` | 200 | Daftar kontak user, diurut `priority` naik |
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
| GET | `/` | 200 | Daftar laporan user, diurut `createdAt` terbaru dulu |
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
| GET | `/` | 200 | Daftar media laporan, diurut `createdAt` terlama dulu |
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
| GET | `/` | — (publik) | 200 | Daftar semua panduan, diurut `createdAt` terbaru dulu |
| GET | `/:id` | — (publik) | 200 | Detail satu panduan |
| POST | `/` | admin | 201 | Buat panduan |
| PUT | `/:id` | admin | 200 | Update parsial |
| DELETE | `/:id` | admin | 200 | Hapus panduan |

Payload & validasi (create):
- `title` — wajib, maks 255.
- `category` — wajib, salah satu enum `GuideCategory`: `SAFETY, FIRST_AID, DISASTER, EMERGENCY`.
- `content` — wajib, string.
- `thumbnail` — opsional, string atau `null`.
- PUT bersifat parsial: minimal satu field; panduan tidak ada → **404**.
- Non-admin yang mengakses route tulis → **403**; tanpa token → **401**.

---

## Pertemanan — `/api/friendships` (semua butuh auth)

| Method | Path | Status sukses | Keterangan |
| --- | --- | --- | --- |
| POST | `/request` | 201 | Kirim permintaan pertemanan (`body.addresseeId` wajib) |
| GET | `/` | 200 | Daftar teman (status `ACCEPTED`), diurut `updatedAt` terbaru dulu |
| GET | `/requests` | 200 | Daftar permintaan masuk (`PENDING`, ditujukan ke user), terbaru dulu |
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
| GET | `/` | 200 | Daftar notifikasi user, terbaru dulu (termasuk preview `sender`) |
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
| GET | `/active` | 200 | Daftar sesi aktif di mana user adalah owner atau viewer |
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
| GET | `/search?q=<kata>` | 200 | Cari user by `username`/`fullName` (case-insensitive), kecuali diri sendiri; diurut `username` |
| GET | `/:id` | 200 | Profil publik + `friendshipStatus` relatif terhadap user |
| GET | `/:id/friends` | 200 | Daftar teman `ACCEPTED` user tsb, diurut `username` |

Aturan:
- `q` — wajib & non-kosong → jika tidak **422**.
- `friendshipStatus` bernilai salah satu: `FRIENDS`, `PENDING_SENT`, `PENDING_RECEIVED`, `NONE` (status `REJECTED` disembunyikan sebagai `NONE`).
- User tidak ada (detail / friends) → **404**.
- Route literal `/search` dideklarasikan sebelum route `/:id`.

---

## Belum Tersedia (rencana)

- **Swagger / OpenAPI interaktif** belum ada (tidak ada dependency maupun setup di `app.js`). Dokumentasi endpoint saat ini mengandalkan file ini. Lihat `PROGRESS.md` (Phase 2 — Item 3).
- Semua model lama sudah memiliki API; SosAlert, Facility, ReportLike, ReportComment sudah ada di skema dan akan mendapat API di tugas roadmap 1.3, 2.1, dan 3.1.
