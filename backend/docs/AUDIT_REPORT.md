# Audit Backend ResQ

- **Tanggal audit:** 2026-10-25
- **Branch:** `main`
- **Commit HEAD:** `46e94f1` — "chore: clean repo to only contain 2 folders"
- **Working tree:** bersih (`git status --short` kosong)
- **Metode:** READ-ONLY. Hanya membaca file + perintah non-mutasi (`ls`, `grep`, `git log/status`). Satu-satunya file yang dibuat adalah dokumen ini.
- **Sumber kebenaran:** kode di `src/`, `prisma/schema.prisma`, `prisma/migrations/`, `package.json`, `.gitignore`. Isi `docs/` lain diperlakukan sebagai klaim.

---

## 0. Ringkasan Eksekutif

**Verdict:** Backend ResQ adalah **REST API CRUD yang rapi dan konsisten**, tetapi **belum memenuhi inti ide aplikasi** yang bersifat *real-time, berbasis lokasi, dan sosial*. Yang sudah matang adalah fondasi akun + konten (auth, kontak darurat, laporan, media, panduan, pertemanan, notifikasi, sesi berbagi sebagai relasi). Yang **hilang** justru bagian yang membuat ResQ "ResQ": **tidak ada SOS, tidak ada data lokasi real-time, tidak ada peta/fasilitas/jarak, tidak ada feed publik, tidak ada pagination**.

**Persentase kelengkapan backend terhadap ide awal: ± 45%** (dasar perhitungan di bagian 10).

**5 temuan terpenting:**
1. **[Tinggi] Fitur 1 (SOS) tidak ada sama sekali** — tidak ada model/endpoint/service SOS; `EmergencyContact` hanya nomor telepon mentah, tidak ada mekanisme kirim alert (bukti: tidak ditemukan di seluruh `src/`).
2. **[Tinggi] Fitur 3 (Safe Companion) tidak menyimpan lokasi** — `SharingSession` hanya relasi owner↔viewer + flag aktif; tidak ada endpoint kirim/baca lokasi (bukti: `prisma/schema.prisma` model `SharingSession`; `src/services/sharing-session.service.js`).
3. **[Tinggi] Fitur 2 (Peta) tidak ada backend** — tidak ada query jarak/Haversine/nearby, tidak ada data fasilitas/POI, tidak ada seed (bukti: grep `haversine|radius|nearby|distance` = 0 hasil di `src/`).
4. **[Tinggi] Fitur 4 (Laporan) tidak punya feed publik & pagination** — `GET /api/reports` hanya mengembalikan laporan milik user sendiri; tidak ada endpoint publik, tidak ada `take/skip` di seluruh service (bukti: `src/services/report.service.js:70-78`; grep pagination = 0 hasil).
5. **[Tinggi] Tidak ada ADMIN yang bisa dibuat** — register memaksa `role: "USER"`; satu-satunya cara jadi ADMIN adalah edit DB manual; tidak ada seed (bukti: `src/services/auth.service.js:83`).

---

## 1. Inventaris Proyek (A)

### Pohon folder (maks 3 level, tanpa `node_modules`)

```
backend/
├── .env                      (TIDAK di-commit; lihat .gitignore)
├── .gitignore
├── package.json
├── package-lock.json
├── docs/                     AUDIT_REPORT.md, API_REFERENCE.md, CONVENTIONS.md,
│                             PROGRESS.md, PROJECT_OVERVIEW.md, README.md
├── prisma/
│   ├── schema.prisma
│   └── migrations/
│       ├── 20261020081336_init/migration.sql
│       └── migration_lock.toml
├── src/
│   ├── app.js
│   ├── server.js
│   ├── config/               db.js, prisma.js
│   ├── controllers/          auth, emergency-contact, report, report-media,
│   │                         guide, friendship, notification, sharing-session, user
│   ├── middleware/           auth, role, upload, error
│   ├── routes/               (9 router — sama dengan controllers)
│   ├── services/             (9 service)
│   ├── utils/                response, hash, jwt, mailer, cloudinary,
│   │                         service-error, async-handler
│   └── validations/          (9 validator)
└── uploads/                  .gitkeep (isi di-ignore)
```

> Catatan: repo Git induk berada di `ResQ/` (parent), berisi `backend/` + `frontend/`. Audit ini fokus pada `backend/`.

### package.json — dependencies (versi)

| Paket | Versi | | Paket | Versi |
|---|---|---|---|---|
| @prisma/client | ^6.19.3 | | dotenv | ^18.0.8 |
| bcrypt | ^6.0.0 | | express | ^5.3.0 |
| cloudinary | ^2.11.0 | | jsonwebtoken | ^9.0.3 |
| cors | ^2.8.6 | | multer | ^2.4.0 |
| | | | nodemailer | ^10.1.7 |

**devDependencies:** `nodemon` ^3.1.14, `prisma` ^6.19.3.

**scripts:** `start` = `node src/server.js`; `dev` = `nodemon src/server.js`.
**TIDAK ADA** script `test`, `build`, `seed`, `migrate`, atau `postinstall` (bukti: `package.json`).

### Variabel lingkungan yang dipakai di kode (nama saja)

Dari `grep process.env src/` (nilai tidak ditampilkan):

- `PORT` — `src/server.js:9`
- `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, `CLOUDINARY_API_SECRET`, `CLOUDINARY_REPORT_FOLDER` — `src/utils/cloudinary.js:15-21`
- `BCRYPT_SALT_ROUNDS` — `src/utils/hash.js:9`
- `JWT_SECRET`, `JWT_EXPIRES_IN`, `JWT_REFRESH_SECRET`, `JWT_REFRESH_EXPIRES_IN`, `JWT_EMAIL_VERIFICATION_SECRET`, `JWT_EMAIL_VERIFICATION_EXPIRES_IN`, `JWT_PASSWORD_RESET_SECRET`, `JWT_PASSWORD_RESET_EXPIRES_IN` — `src/utils/jwt.js:15-26`
- `SMTP_PORT`, `SMTP_HOST`, `SMTP_USER`, `SMTP_PASS`, `SMTP_FROM`, `APP_URL` — `src/utils/mailer.js:19-85`
- `DATABASE_URL` — dipakai Prisma via `schema.prisma` (`env("DATABASE_URL")`), tidak lewat `process.env` langsung.

**`.env.example`: TIDAK DITEMUKAN** (dicari di root; `ls .env.example` = MISSING). Konsekuensi: kontributor baru tidak punya daftar env resmi selain membaca kode.

### Info Git

- Branch aktif: `main`.
- 20 commit terakhir (`git log --oneline -20`) — hanya 11 commit ada di history:
  ```
  46e94f1 chore: clean repo to only contain 2 folders
  aa60b13 Merge pull request #7 ... backend-core-update
  4e43622 feat: update backend core modules and project documentation
  9288c27 Merge pull request #6 ... backend-core
  9eccbe3 feat: implement backend core modules
  65d7058 Merge pull request #4 ... frontend
  f5f5898 refactor(frontend): move android project into frontend directory
  6c50177 Merge pull request #2 ... feature/login-register
  7153213 feat: add login and register screens with basic validation
  c602279 Merge pull request #1 ... feature/homepage-ui
  7170e0e implement ResQ emergency homepage UI
  19123cf chore: initial android project setup
  ```
- Perubahan belum di-commit: **TIDAK ADA** (`git status --short` kosong).
- `.env` pernah ter-commit? **TIDAK** (`git log --all --oneline -- .env` kosong). Aman.

## 2. Skema Database (B)

Isi lengkap `schema.prisma` ada di **Lampiran 1**. Ringkasan di bawah ini diverifikasi dari skema + migrasi.

### Model & enum

| Model | Field inti | Relasi | Index/unik |
|---|---|---|---|
| `User` | id, fullName, username, email, phoneNumber, password, profilePicture, role, isVerified, **lastLatitude, lastLongitude**, createdAt, updatedAt | 1-N ke hampir semua model | unik: username, email, phoneNumber |
| `RefreshToken` | id, userId, token, expiresAt, createdAt | N-1 User (cascade) | index userId |
| `EmergencyContact` | id, userId, contactName, **phoneNumber (string)**, priority (SmallInt), createdAt | N-1 User (cascade) | index userId |
| `IncidentReport` | id, userId, category, title, description?, latitude, longitude, createdAt, updatedAt | N-1 User; 1-N ReportMedia | index userId |
| `ReportMedia` | id, reportId, mediaUrl, mediaType, createdAt | N-1 IncidentReport (cascade) | index reportId |
| `Guide` | id, title, category, content, thumbnail?, createdAt, updatedAt | — | — |
| `Notification` | id, userId, senderId?, type, title, message, isRead, createdAt | N-1 User (receiver cascade, sender setNull) | index userId, senderId |
| `Friendship` | id, requesterId, addresseeId, status, createdAt, updatedAt | 2× N-1 User (cascade) | unik (requesterId, addresseeId); index keduanya |
| `SharingSession` | id, ownerId, viewerId, isActive, startedAt, endedAt? | 2× N-1 User (cascade) | index ownerId, viewerId |

**Enum (seluruh nilai):**
- `UserRole`: USER, ADMIN
- `GuideCategory`: SAFETY, FIRST_AID, DISASTER, EMERGENCY
- `IncidentCategory`: CRIME, ACCIDENT, FIRE, MEDICAL, DISASTER, SUSPICIOUS, OTHER
- `NotificationType`: REPORT, FRIEND_REQUEST, FRIEND_ACCEPTED, SHARE_SESSION, SYSTEM
- `FriendshipStatus`: PENDING, ACCEPTED, REJECTED
- `MediaType`: IMAGE, VIDEO

### Migrasi

- Satu migrasi: `20261020081336_init/migration.sql`. Membuat 6 enum + 9 tabel + index + foreign key (cascade; `notification.sender_id` → SET NULL).
- **Konsistensi skema ↔ migrasi:** konsisten. Semua model/enum di `schema.prisma` tercermin di SQL init; tidak ada migrasi lanjutan yang tertinggal (hanya satu migrasi, tidak ada drift yang terlihat dari file).

### Jawaban spesifik

1. **Field lokasi di `User`:** `lastLatitude`, `lastLongitude` (Decimal 10,7, nullable). **Apakah ditulis/dibaca?** Hanya **dibaca** di respons profil (`src/services/auth.service.js:351-352` getProfile, `:416-417` updateProfile). **TIDAK ADA kode yang menulisnya** — tidak ada endpoint update lokasi (grep `lastLatitude` hanya 4 baca, 0 tulis). Jadi kolom ini praktis selalu `null`.
2. **`SharingSession`:** field = id, ownerId, viewerId, isActive, startedAt, endedAt. **Tidak menyimpan lokasi.** Hanya relasi owner↔viewer + status aktif + waktu. (bukti: `schema.prisma` model `SharingSession`).
3. **`IncidentReport`:** enum `category` = CRIME, ACCIDENT, FIRE, MEDICAL, DISASTER, SUSPICIOUS, OTHER. **TIDAK ADA** field `status`, `isPublic`, `verified`, atau sejenis (bukti: `schema.prisma` model `IncidentReport` hanya punya field di tabel di atas).
4. **Model untuk SOS / fasilitas-POI / area rawan / like-komentar / device token (FCM): TIDAK DITEMUKAN** di `schema.prisma`. Tidak ada tabel apa pun untuk itu.

---

## 3. Inventaris Endpoint & Selisih dengan Dokumen (C)

Diturunkan dari `src/app.js` + file routes. Semua endpoint di-mount di `app.js:40-48`; `errorHandler` global di `app.js:51`.

| Method | Path lengkap | Middleware | Controller.fungsi | Service.fungsi | Validasi |
|---|---|---|---|---|---|
| GET | `/` | — | (inline di app.js:32) | — | — |
| POST | `/api/auth/register` | — | auth.register | authService.register | validateRegister |
| POST | `/api/auth/login` | — | auth.login | authService.login | validateLogin |
| POST | `/api/auth/refresh` | — | auth.refreshToken | authService.refreshToken | validateRefreshToken |
| POST | `/api/auth/forgot-password` | — | auth.forgotPassword | authService.forgotPassword | validateForgotPassword |
| POST | `/api/auth/reset-password` | — | auth.resetPassword | authService.resetPassword | validateResetPassword |
| GET | `/api/auth/verify-email` | — | auth.verifyEmail | authService.verifyEmail | (query token, tanpa validator) |
| GET | `/api/auth/profile` | authenticate | auth.getProfile | authService.getProfile | — |
| PUT | `/api/auth/profile` | authenticate | auth.updateProfile | authService.updateProfile | validateUpdateProfile |
| DELETE | `/api/auth/profile` | authenticate | auth.deleteAccount | authService.deleteAccount | — |
| POST | `/api/auth/logout` | authenticate | auth.logout | authService.logout | validateLogout |
| POST | `/api/emergency-contacts` | authenticate | ec.create | createEmergencyContact | validateCreateEmergencyContact |
| GET | `/api/emergency-contacts` | authenticate | ec.list | getEmergencyContacts | — |
| PUT | `/api/emergency-contacts/:id` | authenticate | ec.update | updateEmergencyContact | validateUpdateEmergencyContact |
| DELETE | `/api/emergency-contacts/:id` | authenticate | ec.remove | deleteEmergencyContact | — |
| POST | `/api/reports` | authenticate | report.create | createReport | validateCreateReport |
| GET | `/api/reports` | authenticate | report.list | getReports (hanya milik user) | — |
| GET | `/api/reports/:id` | authenticate | report.detail | getReportById | — |
| PUT | `/api/reports/:id` | authenticate | report.update | updateReport | validateUpdateReport |
| DELETE | `/api/reports/:id` | authenticate | report.remove | deleteReport | — |
| POST | `/api/reports/:reportId/media` | authenticate + upload.single("file") | media.create | createMedia | validateUploadMedia |
| GET | `/api/reports/:reportId/media` | authenticate | media.list | getMedia | — |
| DELETE | `/api/reports/:reportId/media/:mediaId` | authenticate | media.remove | deleteMedia | — |
| GET | `/api/guides` | — (publik) | guide.list | getGuides | — |
| GET | `/api/guides/:id` | — (publik) | guide.detail | getGuideById | — |
| POST | `/api/guides` | authenticate + **isAdmin** | guide.create | createGuide | validateCreateGuide |
| PUT | `/api/guides/:id` | authenticate + **isAdmin** | guide.update | updateGuide | validateUpdateGuide |
| DELETE | `/api/guides/:id` | authenticate + **isAdmin** | guide.remove | deleteGuide | — |
| POST | `/api/friendships/request` | authenticate | friendship.sendRequest | sendRequest | validateCreateFriendRequest |
| GET | `/api/friendships` | authenticate | friendship.listFriends | getFriends | — |
| GET | `/api/friendships/requests` | authenticate | friendship.listIncomingRequests | getIncomingRequests | — |
| PATCH | `/api/friendships/:id/accept` | authenticate | friendship.accept | acceptRequest | — |
| PATCH | `/api/friendships/:id/reject` | authenticate | friendship.reject | rejectRequest | — |
| DELETE | `/api/friendships/:id` | authenticate | friendship.remove | deleteFriendship | — |
| GET | `/api/notifications` | authenticate | notification.list | getNotifications | — |
| GET | `/api/notifications/unread-count` | authenticate | notification.unreadCount | getUnreadCount | — |
| PATCH | `/api/notifications/read-all` | authenticate | notification.markAllRead | markAllAsRead | — |
| PATCH | `/api/notifications/:id/read` | authenticate | notification.markRead | markAsRead | validateNotificationId |
| DELETE | `/api/notifications/:id` | authenticate | notification.remove | deleteNotification | validateNotificationId |
| POST | `/api/sharing-sessions` | authenticate | ss.create | startSession | validateCreateSharingSession |
| GET | `/api/sharing-sessions/active` | authenticate | ss.listActive | getActiveSessions | — |
| GET | `/api/sharing-sessions/:id` | authenticate | ss.detail | getSessionById | — |
| PATCH | `/api/sharing-sessions/:id/stop` | authenticate | ss.stop | stopSession | — |
| GET | `/api/users/search` | authenticate | user.search | searchUsers | validateUserSearch |
| GET | `/api/users/:id` | authenticate | user.detail | getUserById | — |
| GET | `/api/users/:id/friends` | authenticate | user.friends | getUserFriends | — |

**Total: 45 endpoint** (44 API + 1 health check).

### Selisih dengan `docs/API_REFERENCE.md`

- **Ada di kode, ada di dokumen:** seluruh 44 endpoint API sudah terdokumentasi (API_REFERENCE sudah disinkronkan sebelumnya). **Tidak ada endpoint di kode yang hilang dari dokumen.**
- **Ada di dokumen, tidak ada di kode:** tidak ada endpoint fiktif.
- **Perbedaan perilaku dokumen vs kode:** tidak ditemukan perbedaan status code/aturan yang berarti pada pemeriksaan ini. (Catatan: `NotificationType.REPORT` dan `SYSTEM` ada di enum tetapi tidak pernah dipakai kode mana pun — lihat Lampiran 2.)

Detail ketidaksesuaian lintas-dokumen ada di **Lampiran 2**.

---

## 4. Analisis Kesenjangan per Fitur (D)

### Fitur 1 — One-Tap Multi-Category SOS → ❌ Belum ada

- **Yang sudah ada di kode:** hanya `EmergencyContact` (CRUD) di `src/services/emergency-contact.service.js`. Itu daftar kontak, bukan mekanisme SOS.
- **Yang kurang:** tidak ada model SOS, tidak ada endpoint "kirim alert", tidak ada pengiriman lokasi ke kontak, tidak ada kategori SOS (Kejahatan/Medis/Kebakaran) yang dipicu one-tap. Grep `sos` di `src/` = 0 hasil fungsional.
- **Relasi dengan `EmergencyContact`:** `EmergencyContact.phoneNumber` adalah **string nomor telepon mentah** (`schema.prisma` model `EmergencyContact`), **tidak** di-link ke `User` terdaftar. Jadi kontak darurat tidak bisa menerima notifikasi in-app.
- **Mekanisme kirim alert (SMS/push/email/in-app): TIDAK DITEMUKAN.** Mailer hanya untuk verifikasi & reset password (`src/utils/mailer.js`). Tidak ada SMS gateway, tidak ada FCM/push.
- **Dampak ke FE:** fitur utama #1 aplikasi tidak bisa dibangun dari backend ini. FE hanya bisa menampilkan/CRUD kontak, tidak bisa benar-benar mengirim SOS.

### Fitur 2 — Interactive Emergency Map → ❌ Belum ada

- **Yang sudah ada di kode:** tidak ada.
- **Yang kurang:**
  - **Pencarian berbasis jarak (nearby/radius/bounding box/Haversine): TIDAK DITEMUKAN** (grep `haversine|radius|nearby|distance|bounding` di `src/` = 0 hasil).
  - **Data fasilitas (polisi/RS/damkar/halte): TIDAK DITEMUKAN** — tidak ada model, tidak ada seed.
  - **Agregasi laporan jadi "area rawan": TIDAK DITEMUKAN** — `getReports` hanya daftar laporan milik user (`src/services/report.service.js:70-78`), tanpa agregasi/geo.
- **Dampak ke FE:** peta interaktif tidak punya sumber data backend apa pun. FE harus memakai data eksternal (mis. Google Places) sendiri tanpa dukungan server.

### Fitur 3 — Safe Companion (lokasi real-time) → 🟡 Sebagian (hanya kerangka relasi)

- **Yang sudah ada di kode:** `SharingSession` sebagai relasi. `startSession` membuat 1 baris aktif per teman ACCEPTED (`src/services/sharing-session.service.js:70-130`), `getActiveSessions`, `getSessionById`, `stopSession`. Memulai sesi mengirim notifikasi `SHARE_SESSION`.
- **Yang kurang:**
  - **Endpoint kirim pembaruan lokasi & baca lokasi terakhir owner: TIDAK DITEMUKAN.** → **Sesi hanya relasi, tanpa data lokasi.** Tidak ada field lokasi di `SharingSession`, dan `User.lastLatitude/lastLongitude` tak pernah ditulis.
  - **Tujuan/ETA/"sampai tujuan": TIDAK DITEMUKAN.**
  - **Auto-expire: TIDAK DITEMUKAN** — `endedAt` hanya di-set manual lewat `stopSession`; tidak ada TTL/cron.
  - **Teman otomatis terlihat gaya Find My:** TIDAK. Harus ada sesi aktif, dan sesi pun tak membawa koordinat.
- **Dampak ke FE:** FE tidak bisa menampilkan posisi teman di peta. Hanya bisa menampilkan "siapa sedang berbagi", tanpa titik lokasi.

### Fitur 4 — Laporan (feed sosial) → 🟡 Sebagian (CRUD privat, bukan feed)

- **Siapa yang bisa melihat laporan?** Hanya pemiliknya. `getReports(userId)` memfilter `where: { userId }` (`src/services/report.service.js:72`); `getReportById` menolak non-pemilik dengan 404 (`:92-98`). **TIDAK ADA endpoint feed publik** yang menampilkan laporan semua user. Tidak ada filter kategori/lokasi.
- **Pagination?** **TIDAK ADA di endpoint mana pun.** Grep `take:|skip:|cursor|page|paginat` di `src/services` & `src/controllers` = 0 hasil. Semua list mengembalikan seluruh baris:
  - `GET /api/reports` — `report.service.js:70` tanpa `take`.
  - `GET /api/notifications` — `notification.service.js` getNotifications tanpa `take`.
  - `GET /api/guides` — `guide.service.js` getGuides tanpa `take`.
  - `GET /api/friendships`, `/requests`, `/api/users/search`, `/api/users/:id/friends`, `/api/sharing-sessions/active` — semua tanpa batas.
- **Like / komentar / status laporan / "diteruskan ke aparat": TIDAK DITEMUKAN** (tidak ada model maupun endpoint).
- **Kategori mencakup pelecehan & kerusakan fasilitas publik?** **TIDAK secara eksplisit.** Enum `IncidentCategory` = CRIME, ACCIDENT, FIRE, MEDICAL, DISASTER, SUSPICIOUS, OTHER. "Pelecehan" bisa dipaksakan ke CRIME/SUSPICIOUS; "lampu jalan mati / CCTV rusak" hanya muat di OTHER. Tidak ada kategori khusus fasilitas publik.
- **Dampak ke FE:** feed gaya Instagram tidak bisa dibangun (tidak ada data publik, tidak ada paging, tidak ada like/komentar). FE hanya bisa menampilkan laporan milik user sendiri.

### Fitur 6 — Panduan Keselamatan → 🟡 Sebagian (CRUD lengkap, konten & kategori kurang)

- **Yang sudah ada di kode:** CRUD penuh. Baca publik, tulis admin (`src/routes/guide.routes.js:14-21`). Enum `GuideCategory` = SAFETY, FIRST_AID, DISASTER, EMERGENCY.
- **Apakah kategori mencakup langkah hukum?** **TIDAK** — tidak ada kategori LEGAL/hukum di `GuideCategory`.
- **Seed konten panduan: TIDAK DITEMUKAN** (tidak ada seed script). Tanpa ADMIN (lihat di bawah) & tanpa seed, tabel `guide` kemungkinan kosong saat demo.
- **Dampak ke FE:** struktur siap, tapi konten harus diisi manual; kategori "langkah hukum dasar" dari ide awal tidak terwakili.

### Admin

- **Cara ADMIN dibuat:** **hanya manual di DB.** Register mem-hardcode `role: "USER"` (`src/services/auth.service.js:83`); tidak ada endpoint promote-role, tidak ada seed admin. `isAdmin` membaca `req.user.role` dari token (`src/middleware/role.middleware.js:55`), jadi ADMIN baru berfungsi setelah ada baris user ber-role ADMIN di DB lalu login.
- **Endpoint khusus admin:** hanya 3 — `POST/PUT/DELETE /api/guides` (`guide.routes.js:19-21`).
- **Moderasi laporan / kelola user / kelola fasilitas: TIDAK DITEMUKAN.**
- **Dampak ke FE/demo:** untuk mendemokan pembuatan guide, perlu menyiapkan user ADMIN lewat edit DB manual lebih dulu.

---

## 5. Alur Autentikasi (E)

- **Login mewajibkan email terverifikasi?** **Ya.** `login` menolak `!user.isVerified` dengan 403 (`src/services/auth.service.js:148-153` area pengecekan `isVerified`).
- **Jika SMTP gagal saat register, register tetap sukses?** **Ya.** `register` membungkus pengiriman email dalam try/catch yang hanya `console.error` dan tidak melempar (`src/services/auth.service.js` blok `sendVerificationEmail` ~`:120-130`). Akun tetap dibuat (isVerified=false). Risiko: user tak bisa login karena tak pernah terima email, tanpa endpoint resend.
- **Link email mengarah ke mana?**
  - Verifikasi: `${APP_URL}/api/auth/verify-email?token=...` (`src/utils/mailer.js:58-59`). Ini **endpoint API GET** yang nyata → bisa diklik dari browser.
  - Reset: `${APP_URL}/api/auth/reset-password?token=...` (`src/utils/mailer.js:85`). **Namun endpoint reset adalah POST** butuh body `resetToken`+`newPassword` (`auth.routes.js`). Jadi link ini **mengarah ke path yang bukan GET** → diharapkan dibuka halaman frontend, bukan API. **Mismatch alur** bila `APP_URL` menunjuk ke backend.
  - **Dipakai dari Android?** Keduanya berbasis URL web; tanpa deep link/halaman web khusus, alur reset tidak langsung jalan di app. Perlu halaman web atau deep-link handler.
- **Masa berlaku token (nama env):** access `JWT_EXPIRES_IN` (default `15m`), refresh `JWT_REFRESH_EXPIRES_IN` (default `7d`) (`src/utils/jwt.js:16,18`).
- **Rotasi refresh token?** **Tidak.** `refreshToken` hanya menerbitkan access baru; refresh lama tetap dipakai (`src/services/auth.service.js:230-233` area). Refresh disimpan di DB dan dicek saat refresh; dihapus saat logout/reset.
- **Fallback secret di jwt.js?** **Ya [risiko].** `JWT_SECRET || "change_me_access_secret"` dan semua secret lain fallback ke ACCESS_SECRET (`src/utils/jwt.js:15-26`). Jika env kosong, token ditandatangani dengan secret lemah yang diketahui publik.
- **Rate limiting pada login/register/forgot-password?** **TIDAK ADA** (grep `rate-limit` = 0 hasil). Rentan brute-force & abuse.

---

## 6. Kualitas Kode & Keamanan (F)

### Arsitektur & konsistensi
- **[Rendah] Error handling tidak seragam:** controller auth masih `try/catch` lokal (`src/controllers/auth.controller.js:24` dst), sedangkan 8 controller lain pakai `asyncHandler` + global error handler (`src/middleware/error.middleware.js`). Perilaku respons sama, tapi pola bercabang.
- **[Rendah] `verify-email` tanpa validator** — token query dibaca langsung tanpa validator khusus (controller `verifyEmail`). Divalidasi di service, jadi aman, tapi menyimpang dari pola "validasi di controller".
- Pemisahan layer secara umum **baik**: tidak ditemukan query Prisma di controller, tidak ada logika bisnis di routes/validation (grep `prisma` di `src/controllers` = 0).

### Konfigurasi keamanan HTTP
- **[Sedang] Tidak ada `helmet`** (header keamanan) — grep = 0.
- **[Sedang] Tidak ada `express-rate-limit`** — endpoint auth rentan brute-force.
- **[Rendah] Tidak ada logging request** (`morgan` dll) — menyulitkan audit/forensik.
- **[Rendah] Tidak ada body size limit eksplisit** — `express.json()` tanpa opsi `limit` (`src/app.js:24`); pakai default 100kb. Upload file dibatasi 20MB oleh multer (`upload.middleware.js`), ini sudah baik.
- **CORS:** `cors()` tanpa opsi (`src/app.js:21`) → mengizinkan semua origin. Untuk API publik mobile relatif wajar, tapi tidak membatasi siapa pun.

### Kerentanan umum
- **[Sedang] Query tanpa batas (tidak ada `take`/limit) di semua list** — potensi beban besar saat data banyak; juga menghalangi feed yang skalabel. Bukti: grep pagination = 0.
- **Kebocoran field sensitif:** **tidak ditemukan.** User selalu di-`select` eksplisit tanpa `password` (`auth.service.js`), modul user hanya field publik (`user.service.js:21-27`).
- **Mass assignment:** **terkendali.** Semua service memakai `data: { field eksplisit }` atau objek `updates` yang sudah disaring validator; tidak ada `data: req.body` mentah.
- **IDOR / kepemilikan:** **terkendali** di report, emergency-contact, notification, sharing-session (cek `userId`/owner → 404). Guide memang publik untuk baca (by design).
- **N+1:** tidak signifikan; relasi diambil via `select`/`include` sekali jalan (mis. friendship `FRIENDSHIP_FIELDS`).

### Secret di repo
- **Hardcoded secret selain fallback JWT:** **tidak ditemukan** nilai asli (grep pola di `src/`). Satu-satunya "secret" literal adalah fallback default `"change_me_access_secret"` (`src/utils/jwt.js:15`) — bukan secret asli, tapi tetap berisiko bila dipakai di produksi.
- **`.gitignore`:** `.env`, `.env.*` (kecuali `.env.example`), dan `uploads/*` (kecuali `.gitkeep`) **sudah di-ignore** (`.gitignore`). Baik.
- **Apakah `.env` pernah ter-commit?** **TIDAK** — `git log --all --oneline -- .env` kosong. Aman.

### Fitur berisiko disalahgunakan
- **[Sedang] Spam friend request / notifikasi:** tak ada rate limit; satu user bisa membuat banyak request. Mitigasi ada sebatas cek duplikat (409) di `friendship.service.js`.
- **[Sedang] Spam laporan / media:** tak ada rate limit maupun kuota (selain 20MB/file).
- **Lokasi bocor ke non-teman:** risiko rendah saat ini karena **tidak ada data lokasi** yang disimpan/disajikan; `sharing-session` membatasi view ke owner/viewer (`sharing-session.service.js:getSessionById`). Begitu lokasi real-time ditambah, ini harus ditinjau ulang.

---

## 7. Testing & Dokumentasi (G)

- **Test (file/framework/script): TIDAK DITEMUKAN.** Tidak ada folder test, tidak ada jest/vitest/mocha di deps, tidak ada script `test` (`package.json`).
- **Postman collection: TIDAK DITEMUKAN** (tidak ada `*.postman_collection.json` di repo).
- **Swagger/OpenAPI: TIDAK DITEMUKAN** (tidak ada dependency swagger, tidak ada setup di `app.js`).
- **README di root repo backend: TIDAK DITEMUKAN** (`ls README*` di `backend/` = tidak ada). Dokumentasi hanya di `docs/` (PROJECT_OVERVIEW, PROGRESS, API_REFERENCE, CONVENTIONS, README-docs). Artinya pengunjung repo tidak melihat README di akar `backend/`; harus masuk `docs/`.
  - `docs/PROJECT_OVERVIEW.md` **cukup** untuk menjalankan (ada langkah `npm install` → `prisma generate` → `migrate deploy` → `npm run dev`), tetapi berada di `docs/`, bukan README akar.

---

## 8. Deployment & Integrasi FE (H)

- **Konfigurasi deploy:** **TIDAK ADA.** Tidak ada `Dockerfile`, `vercel.json`, `render.yaml`, `Procfile`, `docker-compose.yml`, `.dockerignore` (dicek, semua MISSING).
- **`prisma generate` / `migrate deploy` otomatis saat deploy?** **TIDAK** — tidak ada `postinstall`/`build`/`start` yang menjalankannya; `start` hanya `node src/server.js` (`package.json`). Harus dijalankan manual.
- **CORS & PORT untuk Android:** `cors()` mengizinkan semua origin (`app.js:21`) → akses lintas-origin tidak terblokir. `PORT` dari env dengan fallback 3000 (`server.js:9`). Secara teknis bisa diakses perangkat Android **asalkan** server di-host dengan URL/host yang dapat dijangkau (bukan localhost).
- **Seed & database export (SQL dump):**
  - Seed: **TIDAK ADA** (grep `seed` = 0).
  - Cara menghasilkan DB export: **TIDAK ADA script**; harus manual (`pg_dump`) di luar repo. Deliverable mata kuliah "database export" belum difasilitasi.
- **Ketergantungan layanan eksternal saat demo:**
  - **Neon (PostgreSQL)** — wajib via `DATABASE_URL`; tanpa ini semua endpoint DB gagal.
  - **Cloudinary** — wajib untuk upload media (`src/utils/cloudinary.js`); gagal upload → 502.
  - **SMTP** — untuk verifikasi email & reset; jika mati, register tetap jalan tapi user tak bisa verifikasi → tak bisa login.

---

## 9. Kepatuhan Ketentuan Mata Kuliah (I)

| Ketentuan | Status | Alasan singkat (bukti) |
|---|---|---|
| API buatan sendiri (bukan Firebase/Supabase) | **Terpenuhi** | Express REST API custom (`src/app.js`, 44 endpoint). |
| Database yang diizinkan | **Terpenuhi** | PostgreSQL via Prisma (`schema.prisma` provider `postgresql`). |
| Struktur folder bersih + separation of concern | **Terpenuhi** | Layer routes/controllers/services/validations/middleware/utils konsisten. |
| Repo siap dipublikasikan tanpa secret | **Sebagian** | `.env` tidak pernah ter-commit & di-ignore (aman), TAPI ada fallback secret lemah di `jwt.js:15` dan tidak ada `.env.example` untuk kontributor. |
| Database export (SQL dump) | **Belum** | Tidak ada script/dump di repo; harus manual. |
| Dukungan data SDG 3 (kesehatan) | **Sebagian** | Guide (FIRST_AID), kategori MEDICAL di report. Tidak ada data layanan kesehatan/fasilitas. |
| Dukungan data SDG 5 (kesetaraan gender) | **Lemah** | Tidak ada fitur/kategori khusus keselamatan perempuan (mis. kategori pelecehan, SOS, berbagi lokasi real-time semua belum berfungsi penuh). Klaim "perhatian pada perempuan" tak tercermin di data/endpoint. |
| Dukungan data SDG 11 (kota berkelanjutan) | **Sebagian-Lemah** | `IncidentReport` + lokasi mendukung pelaporan kota, TAPI tanpa feed publik/agregasi/peta, nilainya sebagai "kota aman" lemah. |

> Catatan rubrik: **API Integration (25%)** terbantu karena API jalan & terstruktur. **Data Handling (20%)** berisiko karena tak ada pagination/validasi lanjutan untuk skala. **SDG Alignment (10%)** adalah titik terlemah dari sisi bukti data.

---

## 10. Rekomendasi & Pertanyaan Terbuka (J)

### Persentase kelengkapan backend terhadap ide awal: ± 45%

**Dasar perhitungan** (6 fitur ide, bobot sama; Fitur 5 Emergency Call = sisi FE, dikeluarkan → 5 fitur backend + fondasi):

| Area | Bobot | Skor | Catatan |
|---|---|---|---|
| Fondasi (auth, akun, media, konvensi, error handling) | 25% | 23% | Matang. |
| Fitur 1 SOS | 15% | 0% | Tidak ada. |
| Fitur 2 Peta | 15% | 0% | Tidak ada. |
| Fitur 3 Safe Companion | 15% | 5% | Hanya relasi, tanpa lokasi. |
| Fitur 4 Laporan (sosial) | 15% | 6% | CRUD privat, tanpa feed/paging. |
| Fitur 6 Panduan | 15% | 11% | CRUD lengkap, konten/kategori kurang. |
| **Total** | **100%** | **± 45%** | |

### Top 10 risiko/temuan (terurut dampak)

1. **[Tinggi] SOS (fitur inti #1) tidak ada** — tanpa ini aplikasi kehilangan nilai utamanya.
2. **[Tinggi] Safe Companion tanpa data lokasi** — `SharingSession` hanya relasi; tidak ada kirim/baca GPS.
3. **[Tinggi] Tidak ada feed laporan publik + tidak ada pagination di seluruh list** — menghalangi fitur sosial & skalabilitas (Data Handling 20%).
4. **[Tinggi] Peta/fasilitas/jarak tak ada backend** — fitur #2 tak bisa dibangun.
5. **[Tinggi] Tidak ada cara membuat ADMIN selain edit DB manual** — demo guide-admin tersendat; tidak ada seed.
6. **[Tinggi] SDG 5 lemah** — tidak ada fitur keselamatan perempuan yang konkret; rubrik SDG berisiko.
7. **[Sedang] Tidak ada rate limiting & helmet** — auth rentan brute-force, header keamanan absen.
8. **[Sedang] Fallback JWT secret lemah + tidak ada `.env.example`** — risiko konfigurasi produksi.
9. **[Sedang] Tidak ada deployment config, seed, maupun DB export** — deliverable & demo belum siap otomatis.
10. **[Sedang] Alur reset-password mismatch (link GET ke endpoint POST)** + tidak ada resend verifikasi — alur Android bisa buntu.

### Daftar tugas yang diusulkan (prioritas; HANYA USULAN)

| # | Judul | Ukuran | Dependensi |
|---|---|---|---|
| 1 | Endpoint update lokasi user + lokasi pada SharingSession (kirim/baca) | L | schema change (migrasi) |
| 2 | Modul SOS (model alert + endpoint trigger + link ke EmergencyContact/teman + notifikasi) | L | #1, notifikasi |
| 3 | Feed laporan publik + pagination (cursor/offset) di semua list | M | — |
| 4 | Pagination menyeluruh (reports, notifications, guides, friends, search) | M | — |
| 5 | Seed script (admin + guide konten + fasilitas contoh) + `package.json` script | S | — |
| 6 | Endpoint/flow pembuatan ADMIN yang aman (atau seed admin terdokumentasi) | S | #5 |
| 7 | Data fasilitas + endpoint nearby (Haversine / PostGIS) untuk peta | L | #1 |
| 8 | Rate limiting + helmet + request logging | S | — |
| 9 | `.env.example`, Dockerfile/Procfile, auto `prisma migrate deploy` saat start | M | — |
| 10 | Kategori tambahan (pelecehan, fasilitas publik, langkah hukum di Guide) | S | migrasi enum |
| 11 | README akar + Postman collection + Swagger | M | — |
| 12 | Test otomatis (minimal happy-path auth + reports) | M | — |

> Semua di atas **usulan**. Tidak ada yang diimplementasikan dalam audit ini.

### Pertanyaan terbuka (perlu keputusan manusia)

1. Apakah SOS, peta, dan lokasi real-time memang **in-scope** untuk rilis mata kuliah, atau sebagian sengaja dipindah ke FE/eksternal?
2. Bagaimana FE mengirim lokasi — endpoint REST periodik, atau butuh WebSocket/polling? (menentukan arsitektur Safe Companion).
3. Siapa penerima "pengaduan diteruskan ke aparat" — apakah hanya tampilan FE atau butuh integrasi backend nyata?
4. Apakah `APP_URL` menunjuk ke backend atau ke halaman web frontend (menentukan perbaikan alur reset-password)?
5. Apakah kontak darurat harus user terdaftar (agar bisa notifikasi in-app) atau tetap nomor mentah?
6. Target "database export" mata kuliah: dump apa (struktur saja / dengan data seed)?

---

## Lampiran 1: schema.prisma (verbatim)

```prisma
generator client {
  provider = "prisma-client-js"
}

datasource db {
  provider = "postgresql"
  url      = env("DATABASE_URL")
}

enum UserRole {
  USER
  ADMIN

  @@map("user_role")
}

enum GuideCategory {
  SAFETY
  FIRST_AID
  DISASTER
  EMERGENCY

  @@map("guide_category")
}

enum IncidentCategory {
  CRIME
  ACCIDENT
  FIRE
  MEDICAL
  DISASTER
  SUSPICIOUS
  OTHER

  @@map("incident_category")
}

enum NotificationType {
  REPORT
  FRIEND_REQUEST
  FRIEND_ACCEPTED
  SHARE_SESSION
  SYSTEM

  @@map("notification_type")
}

enum FriendshipStatus {
  PENDING
  ACCEPTED
  REJECTED

  @@map("friendship_status")
}

enum MediaType {
  IMAGE
  VIDEO

  @@map("media_type")
}

model User {
  id             String   @id @default(uuid())
  fullName       String   @map("full_name") @db.VarChar(255)
  username       String   @unique @db.VarChar(100)
  email          String   @unique @db.VarChar(255)
  phoneNumber    String   @unique @map("phone_number") @db.VarChar(20)
  password       String   @db.VarChar(255)
  profilePicture String?  @map("profile_picture")
  role           UserRole
  isVerified     Boolean  @map("is_verified")
  lastLatitude   Decimal? @map("last_latitude") @db.Decimal(10, 7)
  lastLongitude  Decimal? @map("last_longitude") @db.Decimal(10, 7)
  createdAt      DateTime @default(now()) @map("created_at")
  updatedAt      DateTime @updatedAt @map("updated_at")

  refreshTokens         RefreshToken[]
  emergencyContacts     EmergencyContact[]
  incidentReports       IncidentReport[]
  receivedNotifications Notification[]     @relation("NotificationReceiver")
  sentNotifications     Notification[]     @relation("NotificationSender")
  friendshipsRequested  Friendship[]       @relation("FriendshipRequester")
  friendshipsReceived   Friendship[]       @relation("FriendshipAddressee")
  ownedSharingSessions  SharingSession[]   @relation("SharingSessionOwner")
  viewedSharingSessions SharingSession[]   @relation("SharingSessionViewer")

  @@map("user")
}

model Guide {
  id        String        @id @default(uuid())
  title     String        @db.VarChar(255)
  category  GuideCategory
  content   String
  thumbnail String?
  createdAt DateTime      @default(now()) @map("created_at")
  updatedAt DateTime      @updatedAt @map("updated_at")

  @@map("guide")
}

model RefreshToken {
  id        String   @id @default(uuid())
  userId    String   @map("user_id")
  token     String
  expiresAt DateTime @map("expires_at")
  createdAt DateTime @default(now()) @map("created_at")

  user User @relation(fields: [userId], references: [id], onDelete: Cascade)

  @@index([userId])
  @@map("refresh_token")
}

model EmergencyContact {
  id          String   @id @default(uuid())
  userId      String   @map("user_id")
  contactName String   @map("contact_name") @db.VarChar(255)
  phoneNumber String   @map("phone_number") @db.VarChar(20)
  priority    Int      @db.SmallInt
  createdAt   DateTime @default(now()) @map("created_at")

  user User @relation(fields: [userId], references: [id], onDelete: Cascade)

  @@index([userId])
  @@map("emergency_contact")
}

model IncidentReport {
  id          String           @id @default(uuid())
  userId      String           @map("user_id")
  category    IncidentCategory
  title       String           @db.VarChar(255)
  description String?
  latitude    Decimal          @db.Decimal(10, 7)
  longitude   Decimal          @db.Decimal(10, 7)
  createdAt   DateTime         @default(now()) @map("created_at")
  updatedAt   DateTime         @updatedAt @map("updated_at")

  user  User          @relation(fields: [userId], references: [id], onDelete: Cascade)
  media ReportMedia[]

  @@index([userId])
  @@map("incident_report")
}

model ReportMedia {
  id        String    @id @default(uuid())
  reportId  String    @map("report_id")
  mediaUrl  String    @map("media_url")
  mediaType MediaType @map("media_type")
  createdAt DateTime  @default(now()) @map("created_at")

  report IncidentReport @relation(fields: [reportId], references: [id], onDelete: Cascade)

  @@index([reportId])
  @@map("report_media")
}

model Notification {
  id        String           @id @default(uuid())
  userId    String           @map("user_id")
  senderId  String?          @map("sender_id")
  type      NotificationType
  title     String           @db.VarChar(255)
  message   String
  isRead    Boolean          @map("is_read")
  createdAt DateTime         @default(now()) @map("created_at")

  user   User  @relation("NotificationReceiver", fields: [userId], references: [id], onDelete: Cascade)
  sender User? @relation("NotificationSender", fields: [senderId], references: [id])

  @@index([userId])
  @@index([senderId])
  @@map("notification")
}

model Friendship {
  id          String           @id @default(uuid())
  requesterId String           @map("requester_id")
  addresseeId String           @map("addressee_id")
  status      FriendshipStatus
  createdAt   DateTime         @default(now()) @map("created_at")
  updatedAt   DateTime         @updatedAt @map("updated_at")

  requester User @relation("FriendshipRequester", fields: [requesterId], references: [id], onDelete: Cascade)
  addressee User @relation("FriendshipAddressee", fields: [addresseeId], references: [id], onDelete: Cascade)

  @@unique([requesterId, addresseeId])
  @@index([requesterId])
  @@index([addresseeId])
  @@map("friendship")
}

model SharingSession {
  id        String    @id @default(uuid())
  ownerId   String    @map("owner_id")
  viewerId  String    @map("viewer_id")
  isActive  Boolean   @map("is_active")
  startedAt DateTime  @default(now()) @map("started_at")
  endedAt   DateTime? @map("ended_at")

  owner  User @relation("SharingSessionOwner", fields: [ownerId], references: [id], onDelete: Cascade)
  viewer User @relation("SharingSessionViewer", fields: [viewerId], references: [id], onDelete: Cascade)

  @@index([ownerId])
  @@index([viewerId])
  @@map("sharing_session")
}
```

> Lampiran 1 disalin dari `prisma/schema.prisma`. Komentar header dihilangkan agar ringkas; struktur model/field/relasi/enum identik dengan file asli (aman, tanpa secret).

---

## Lampiran 2: Daftar ketidaksesuaian dokumen vs kode

| # | Dokumen | Klaim dokumen | Kondisi kode | Tingkat |
|---|---|---|---|---|
| 1 | (enum) | — | `NotificationType.REPORT` dan `SYSTEM` ada di `schema.prisma` tetapi **tidak pernah dipakai** kode mana pun (hanya FRIEND_REQUEST, FRIEND_ACCEPTED, SHARE_SESSION yang dibuat). | Rendah |
| 2 | PROJECT_OVERVIEW / README | Langkah run menyebut `prisma migrate deploy` | Tidak ada otomatisasi di `package.json`; harus manual. Bukan salah, tapi perlu ditegaskan. | Rendah |
| 3 | Semua docs | Menyiratkan backend "matang/selesai" untuk fitur inti | Fitur inti aplikasi (SOS, peta, lokasi real-time, feed) belum ada di kode. Dokumen tidak membandingkan dengan **ide aplikasi**, hanya dengan modul yang ada. | Sedang |
| 4 | (umum) | API_REFERENCE tidak menyebut ketiadaan pagination | Tidak ada pagination di endpoint list mana pun; sebaiknya didokumentasikan sebagai batasan. | Rendah |
| 5 | mailer vs routes | Link reset `GET .../reset-password?token=` | Endpoint reset adalah **POST** dengan body; link GET tidak cocok dengan endpoint API. Sudah dicatat di PROGRESS, tetapi tetap belum diperbaiki di kode. | Sedang |

> Catatan: pada audit dokumen sebelumnya, `docs/` (API_REFERENCE, PROGRESS, PROJECT_OVERVIEW, CONVENTIONS, README) sudah disinkronkan dengan modul yang ADA di kode. Ketidaksesuaian tersisa di atas bersifat halus (enum tak terpakai, batasan tak tertulis) atau terkait perbandingan terhadap **ide aplikasi** (bukan sekadar modul), yang memang menjadi fokus audit ini.
