# ResQ Backend: Roadmap Penutupan Gap

Dasar dokumen: `AUDIT_REPORT.md` (commit `46e94f1`, 2026-10-25) dibandingkan dengan ide awal ResQ dan ketentuan mata kuliah COSC6103001.
Status backend saat ini: fondasi matang (auth, kontak darurat, laporan, media, panduan, pertemanan, notifikasi, sesi berbagi, utilitas user), tetapi fitur inti (SOS, peta, lokasi live, feed publik) belum ada. Estimasi kelengkapan ±45%.

Dokumen ini berisi: (1) keputusan desain, (2) perubahan skema Prisma, (3) spesifikasi endpoint, (4) rencana kerja bertahap dan **pembagian tugas final**, (5) catatan implementasi.

---

## Kedudukan Dokumen Ini (baca dulu)

**Untuk pengerjaan selanjutnya, roadmap inilah yang dipakai.** Dokumen ini menggantikan daftar "Prioritas Berikutnya" di `docs/PROGRESS.md` (Swagger, testing, Postman, FE integration, bug fixing, deployment), yang semuanya sudah ditampung di sini.

**Penjelasan istilah (agar tidak tertukar):**

| Istilah | Dipakai di | Artinya |
|---|---|---|
| "Phase 1" dan "Phase 2" | `PROGRESS.md` | Pekerjaan **lama yang sudah selesai** (sembilan modul fitur, notifikasi bisnis, global error handler). Jangan diubah. |
| "Fase 0" sampai "Fase 3" | Roadmap ini | Pekerjaan **baru yang akan dikerjakan**. Sumber kebenaran untuk apa yang dikerjakan berikutnya. |
| ID tugas (`0.1`, `1.3`, `2.3`, dst.) | Roadmap ini | Dipakai di nama branch, judul PR, dan catatan progres. |

Jika ada pertentangan: **roadmap menentukan apa yang dikerjakan berikutnya; `PROGRESS.md` mencatat apa yang sudah selesai; kode tetap kebenaran akhir.**

**Cara kerja:** urutan dan aturan kolaborasi ada di `docs/WORKFLOW.md` (estafet: satu orang bekerja, push dan merge, baru rekan mulai). Roadmap ini mendefinisikan **apa** yang dikerjakan; WORKFLOW mendefinisikan **kapan dan oleh siapa**.

**Kondisi awal:** rekan BE belum mengerjakan apa pun sejak handover, jadi kode saat ini sama dengan hasil audit (commit `46e94f1`).

**Cara menyimpan di repo:**
1. Simpan file ini sebagai `docs/ROADMAP.md`.
2. Simpan `RESQ_WORKFLOW.md` sebagai `docs/WORKFLOW.md`, lalu tambahkan dua baris ke tabel "Daftar File" di `docs/README.md`: `ROADMAP.md | Rencana pengerjaan lanjutan, spesifikasi endpoint baru, dan pembagian tugas` dan `WORKFLOW.md | Urutan giliran (estafet) dan prosedur kerja dua orang BE`.
3. Di `docs/PROGRESS.md`, ganti isi bagian "Prioritas Berikutnya" dengan teks berikut:

```markdown
## Rencana Lanjutan

Pengerjaan selanjutnya mengikuti `docs/ROADMAP.md` (Fase 0-3 dan ID tugas di sana).
Istilah "Phase" di dokumen ini merujuk pekerjaan lama yang sudah selesai;
"Fase" di roadmap merujuk pekerjaan baru. Setiap tugas roadmap yang selesai
dicatat di tabel "Status Per Fitur" dengan menyebut ID tugasnya (contoh: "SOS (R-1.3) ✅").
```

---

## 1. Keputusan Desain (jawaban untuk "Pertanyaan Terbuka" audit)

Semua di bawah adalah **default yang diusulkan**. Tim boleh mengubah, tetapi putuskan sebelum mulai Fase 1.

| # | Pertanyaan | Default yang diusulkan | Alasan |
|---|---|---|---|
| 1 | Apakah SOS, peta, lokasi live in-scope? | **Ya, semua in-scope.** | Itu inti ide aplikasi dan menopang SDG 3, 5, 11. |
| 2 | Cara FE mengirim lokasi | **REST + polling** (FE kirim tiap 5-10 detik, viewer polling). Bukan WebSocket. | Lebih sederhana, cukup untuk demo, tidak butuh infrastruktur tambahan. |
| 3 | Penerima "pengaduan ke aparat" | **Disimulasikan lewat `status` laporan** (`SUBMITTED` → `IN_REVIEW` → `FORWARDED` → `RESOLVED`/`REJECTED`) yang diubah admin. Jelaskan di laporan bahwa integrasi aparat nyata di luar scope. | Tidak ada API resmi aparat. Alur status tetap menunjukkan fungsi end-to-end. |
| 4 | `APP_URL` menunjuk ke mana | **Ke backend itu sendiri.** Backend menyajikan halaman HTML sederhana untuk reset password. | Tidak butuh web FE terpisah, bisa dibuka dari browser HP. |
| 5 | Kontak darurat: nomor mentah atau user terdaftar | **Tetap nomor mentah**, tetapi backend mencocokkan dengan `User.phoneNumber`. Yang cocok mendapat notifikasi in-app. Sisanya dihubungi FE lewat intent SMS/WhatsApp. | Tanpa perubahan struktur kontak, tetapi alert tetap bisa terkirim. |
| 6 | Isi "database export" | **Dump lengkap (struktur + data seed demo)** dengan `pg_dump`, plus `schema.prisma` dan migrasi. | Memudahkan penilai menjalankan ulang. |
| 7 | Lokasi: "Find My" (selalu terlihat) atau berbasis sesi | **Berbasis sesi saja.** Lokasi hanya diterima dan dibaca saat ada sesi aktif, dan dihapus (set null) saat sesi berhenti atau kedaluwarsa. | Lebih aman secara privasi, relevan dengan SDG 5. Ide awal "otomatis terlihat" cukup dijelaskan sebagai pengembangan lanjutan. |
| 8 | Admin di dalam aplikasi Android atau web terpisah | **Diputuskan: di dalam aplikasi Android yang sama**, datanya di backend. Admin login dengan email dan password yang disediakan lewat seed backend; FE membaca `role` dari respons login lalu otomatis mengarahkan ke halaman admin. | Satu aplikasi, satu alur login. Backend cukup menyediakan `/api/admin/*` dan akun admin hasil seed. |

---

## 2. Perubahan Skema Prisma

Pakai satu migrasi per fase (`npx prisma migrate dev --name <nama>`). Jangan edit migrasi lama. Prisma menangani `ALTER TYPE ... ADD VALUE` untuk penambahan nilai enum.

### 2.1 Fase 0-1: enum dan field tambahan

```prisma
// --- enum yang sudah ada: tambah nilai ---
enum GuideCategory {
  SAFETY
  FIRST_AID
  DISASTER
  EMERGENCY
  LEGAL            // BARU: langkah hukum dasar

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
  HARASSMENT       // BARU
  PUBLIC_FACILITY  // BARU: lampu jalan mati, CCTV rusak, dll.

  @@map("incident_category")
}

enum NotificationType {
  REPORT           // sudah ada, belum terpakai: pakai untuk perubahan status laporan
  FRIEND_REQUEST
  FRIEND_ACCEPTED
  SHARE_SESSION
  SYSTEM
  SOS_ALERT        // BARU
  SHARE_ARRIVED    // BARU: teman sampai tujuan

  @@map("notification_type")
}

// --- enum baru ---
enum ReportStatus {
  SUBMITTED
  IN_REVIEW
  FORWARDED
  RESOLVED
  REJECTED

  @@map("report_status")
}

enum SosCategory {
  POLICE
  MEDICAL
  FIRE

  @@map("sos_category")
}

enum SosStatus {
  ACTIVE
  RESOLVED
  CANCELLED

  @@map("sos_status")
}

enum FacilityType {
  POLICE
  HOSPITAL
  CLINIC
  FIRE_STATION
  BUS_STOP
  TRAIN_STATION

  @@map("facility_type")
}
```

```prisma
// --- model User: tambah field ---
model User {
  // ...field lama (lastLatitude & lastLongitude SUDAH ADA, tinggal dipakai)
  locationUpdatedAt DateTime? @map("location_updated_at")

  sosAlerts       SosAlert[]
  reportLikes     ReportLike[]
  reportComments  ReportComment[]
}

// --- model SharingSession: tambah field ---
model SharingSession {
  // ...field lama
  expiresAt            DateTime? @map("expires_at")
  destinationName      String?   @map("destination_name") @db.VarChar(255)
  destinationLatitude  Decimal?  @map("destination_latitude") @db.Decimal(10, 7)
  destinationLongitude Decimal?  @map("destination_longitude") @db.Decimal(10, 7)
}

// --- model IncidentReport: tambah field, relasi, dan index ---
model IncidentReport {
  // ...field lama
  status      ReportStatus @default(SUBMITTED)
  statusNote  String?      @map("status_note")
  isAnonymous Boolean      @default(false) @map("is_anonymous")

  likes    ReportLike[]
  comments ReportComment[]

  @@index([createdAt])
  @@index([category])
  @@index([status])
  @@index([latitude, longitude])
}
```

### 2.2 Model baru: SOS

```prisma
model SosAlert {
  id         String      @id @default(uuid())
  userId     String      @map("user_id")
  category   SosCategory
  status     SosStatus   @default(ACTIVE)
  latitude   Decimal     @db.Decimal(10, 7)
  longitude  Decimal     @db.Decimal(10, 7)
  message    String?
  createdAt  DateTime    @default(now()) @map("created_at")
  resolvedAt DateTime?   @map("resolved_at")

  user User @relation(fields: [userId], references: [id], onDelete: Cascade)

  @@index([userId])
  @@index([status, createdAt])
  @@map("sos_alert")
}
```

### 2.3 Model baru: Fasilitas (Fase 2)

```prisma
model Facility {
  id        String       @id @default(uuid())
  name      String       @db.VarChar(255)
  type      FacilityType
  address   String?
  phone     String?      @db.VarChar(30)
  latitude  Decimal      @db.Decimal(10, 7)
  longitude Decimal      @db.Decimal(10, 7)
  createdAt DateTime     @default(now()) @map("created_at")
  updatedAt DateTime     @updatedAt @map("updated_at")

  @@index([type])
  @@index([latitude, longitude])
  @@map("facility")
}
```

### 2.4 Model baru: Like dan komentar (Fase 3)

```prisma
model ReportLike {
  id        String   @id @default(uuid())
  reportId  String   @map("report_id")
  userId    String   @map("user_id")
  createdAt DateTime @default(now()) @map("created_at")

  report IncidentReport @relation(fields: [reportId], references: [id], onDelete: Cascade)
  user   User           @relation(fields: [userId], references: [id], onDelete: Cascade)

  @@unique([reportId, userId])
  @@index([userId])
  @@map("report_like")
}

model ReportComment {
  id        String   @id @default(uuid())
  reportId  String   @map("report_id")
  userId    String   @map("user_id")
  content   String   @db.VarChar(500)
  createdAt DateTime @default(now()) @map("created_at")

  report IncidentReport @relation(fields: [reportId], references: [id], onDelete: Cascade)
  user   User           @relation(fields: [userId], references: [id], onDelete: Cascade)

  @@index([reportId, createdAt])
  @@map("report_comment")
}
```

---

## 3. Spesifikasi Endpoint

Semua endpoint di bawah mengikuti konvensi lama: `authenticate`, validator buatan sendiri, `asyncHandler`, `sendSuccess`/`sendError`, status code konsisten. Path literal dideklarasikan **sebelum** `/:id`.

### 3.1 Pagination standar (berlaku untuk semua endpoint list)

- Query: `page` (default 1), `limit` (default 20, maks 50).
- Respons: tetap `{ success, message, data }`, ditambah field **`meta`**: `{ page, limit, total, totalPages }`. Penambahan `meta` tidak merusak klien lama.
- Terapkan ke: reports, notifications, guides, friendships, friend requests, users/search, users/:id/friends, sharing-sessions/active, semua endpoint admin.

### 3.2 Lokasi dan Safe Companion

| Method | Path | Auth | Keterangan |
|---|---|---|---|
| PUT | `/api/location` | user | Body `{ latitude, longitude }`. Memperbarui `lastLatitude/lastLongitude/locationUpdatedAt`. Ditolak **409** bila user tidak punya sesi berbagi aktif. |
| POST | `/api/sharing-sessions` (diubah) | user | Body opsional: `viewerIds` (subset teman `ACCEPTED`, default semua), `durationMinutes` (default 120, maks 720, mengisi `expiresAt`), `destination { name, latitude, longitude }`. Aturan "satu sesi aktif per owner" tetap. |
| GET | `/api/sharing-sessions/active` (diubah) | user | Untuk baris di mana user adalah viewer, sertakan `ownerLocation { latitude, longitude, updatedAt }`, sehingga peta cukup satu panggilan. |
| GET | `/api/sharing-sessions/:id/location` | viewer | Lokasi terakhir owner. Bukan viewer atau tidak ada → **404**. Sesi berhenti atau kedaluwarsa → **409**. |
| PATCH | `/api/sharing-sessions/:id/stop` (diubah) | owner | Body opsional `{ arrived: true }`. Bila `arrived`, kirim notifikasi `SHARE_ARRIVED` ke viewer. Saat berhenti, `lastLatitude/lastLongitude` owner di-set `null`. |

Kedaluwarsa dihitung **lazy**: setiap membaca sesi atau lokasi, baris dengan `expiresAt < now()` dinonaktifkan (`isActive=false`, `endedAt` diisi) dan lokasi owner dihapus. Tidak perlu cron.

### 3.3 SOS

| Method | Path | Auth | Keterangan |
|---|---|---|---|
| POST | `/api/sos` | user | Body `{ category, latitude, longitude, message? }`. **Idempoten:** bila user sudah punya SOS `ACTIVE`, perbarui lokasinya dan kembalikan (200), bukan buat baru (201). Efek samping lihat di bawah. |
| GET | `/api/sos/active` | user | SOS aktif milik user (atau `null`). |
| GET | `/api/sos` | user | Riwayat SOS user (paginated). |
| PATCH | `/api/sos/:id/resolve` | user | Body `{ status: "RESOLVED" \| "CANCELLED" }` (CANCELLED = salah tekan). |

Efek samping `POST /api/sos`:
1. Buat notifikasi `SOS_ALERT` (best-effort, lewat `notification.service`) ke:
   - semua teman `ACCEPTED`, dan
   - pemilik akun yang `phoneNumber`-nya cocok dengan salah satu `EmergencyContact` user (setelah normalisasi nomor, lihat bagian 5).
2. Respons `data` berisi:
   - `alert` (data SOS),
   - `emergencyContacts` (urut `priority`: `contactName`, `phoneNumber`, `priority`),
   - `shareText` (teks siap kirim, memuat kategori dan tautan peta `https://maps.google.com/?q=<lat>,<lng>`).
   FE memakai dua yang terakhir untuk membuka intent SMS/WhatsApp ke kontak yang belum terdaftar.
3. Rate limit khusus: maks 5 request per menit per user.

### 3.4 Laporan (feed publik, status, like, komentar)

| Method | Path | Auth | Keterangan |
|---|---|---|---|
| GET | `/api/reports` (**diubah**) | user | **Feed publik** semua laporan, terbaru dulu, paginated. Filter: `category`, `status`, `lat`+`lng`+`radiusKm`, `days`. Tiap item: media pertama, `author` (di-`null`-kan bila `isAnonymous`), `likeCount`, `commentCount`, `likedByMe`. |
| GET | `/api/reports/mine` | user | Laporan milik user sendiri (perilaku lama `GET /api/reports`). |
| GET | `/api/reports/hotspots` | user | Area rawan: `lat`, `lng`, `radiusKm`, `days` (default 30). Mengelompokkan laporan ke grid ±500 m. Mengembalikan `[{ latitude, longitude, count, topCategory, level }]`, `level` = `LOW`/`MEDIUM`/`HIGH` berdasarkan ambang jumlah. Tanpa tabel baru. |
| GET | `/api/reports/:id` (diubah) | user | Dapat dibaca semua user terautentikasi (laporan bersifat publik). `author` disembunyikan bila anonim, kecuali untuk pemilik. |
| POST | `/api/reports` (diubah) | user | Terima `isAnonymous`. Kategori baru `HARASSMENT`, `PUBLIC_FACILITY`. `status` selalu mulai `SUBMITTED`. |
| PUT / DELETE | `/api/reports/:id` | pemilik | Tetap hanya pemilik (bukan pemilik → 404). **`status` tidak bisa diubah lewat PUT.** |
| POST / DELETE | `/api/reports/:id/like` | user | Idempoten. |
| GET / POST | `/api/reports/:id/comments` | user | Daftar komentar (paginated) dan tambah komentar (maks 500 karakter). |
| DELETE | `/api/reports/:id/comments/:commentId` | penulis atau admin | Hapus komentar. |

Catatan perubahan perilaku: konvensi lama "laporan milik user lain → 404" hanya berlaku untuk **PUT/DELETE**. Catat perubahan ini di `CONVENTIONS.md` dan `API_REFERENCE.md`. Karena FE belum mengintegrasikan modul laporan, mengubah semantik `GET /api/reports` sekarang masih aman.

### 3.5 Peta dan fasilitas

| Method | Path | Auth | Keterangan |
|---|---|---|---|
| GET | `/api/facilities` | user | Query: `lat`, `lng` (wajib), `radiusKm` (default 5, maks 50), `type` (boleh banyak, dipisah koma), `limit` (default 30, maks 100). Hasil terurut jarak, tiap item memuat `distanceKm`. |
| GET | `/api/facilities/:id` | user | Detail fasilitas. |
| POST / PUT / DELETE | `/api/facilities` | admin | CRUD fasilitas. |

### 3.6 Admin

Admin berada di dalam aplikasi Android yang sama. Alurnya: admin login lewat `POST /api/auth/login` dengan akun hasil seed, FE membaca `data.user.role`, dan bila `ADMIN` langsung membuka halaman admin. **Keamanan ditegakkan di server:** semua endpoint di bawah wajib `authenticate` + `isAdmin`, sehingga menyembunyikan menu di FE saja tidak dianggap cukup.

| Method | Path | Keterangan |
|---|---|---|
| GET | `/api/admin/stats` | Jumlah user, laporan per status dan kategori, SOS 24 jam terakhir, SOS aktif. Untuk dashboard admin. |
| GET | `/api/admin/reports` | Semua laporan, filter `status`, `category`, paginated. |
| PATCH | `/api/admin/reports/:id/status` | Body `{ status, statusNote? }`. Membuat notifikasi `REPORT` ke pelapor (best-effort). |
| DELETE | `/api/admin/reports/:id` | Moderasi (hapus laporan melanggar). |
| GET | `/api/admin/sos` | Semua SOS, filter `status`, paginated. |
| GET | `/api/admin/users` | Daftar user (read-only, filter `q`, paginated, tanpa field sensitif). |
| (existing) | `/api/guides` POST/PUT/DELETE | Sudah ada. |
| (baru) | `/api/facilities` POST/PUT/DELETE | Lihat 3.5. |

### 3.7 Perbaikan autentikasi

| Perubahan | Detail |
|---|---|
| `POST /api/auth/resend-verification` | Body `{ email }`. Selalu **200** dengan pesan generik (anti-enumerasi). Rate limit ketat. |
| Halaman reset password | `GET /reset-password?token=` menyajikan HTML minimal (form password baru) yang memanggil `POST /api/auth/reset-password`. Ubah link di `utils/mailer.js` ke path ini. |
| Halaman hasil verifikasi email | `GET /api/auth/verify-email` merespons HTML ramah untuk browser (Accept: text/html), JSON tetap untuk klien API. |
| `role` di respons login dan profil | `POST /api/auth/login` mengembalikan `data.user.role`, begitu pula `GET /api/auth/profile` (`USER` atau `ADMIN`). Verifikasi di kode, tambahkan bila belum ada. FE memakainya untuk memilih halaman awal. Jangan menaruh kredensial admin di kode FE. |
| Hapus fallback secret JWT | `utils/jwt.js` melempar error saat start jika `JWT_*` kosong (minimal di `NODE_ENV=production`). |
| Flag demo `AUTO_VERIFY_EMAIL` | Default `false`. Bila `true`, akun baru langsung `isVerified=true` (jaring pengaman bila SMTP mati saat demo). Dokumentasikan agar tidak dipakai di luar demo. |

---

## 4. Rencana Kerja Bertahap

Ukuran: **S** ≤ setengah hari, **M** ≈ 1 hari, **L** > 1 hari. Tentukan tanggal setelah deadline pengumpulan jelas. Urutan mengikuti dampak ke rubrik (API Integration 25%, Data Handling 20%, SDG 10%) dan kebutuhan FE.

### Fase 0: Fondasi dan unblock FE

Selesaikan **semuanya** lalu **deploy** sebelum mulai tugas Fase 1 lainnya, supaya FE bisa mulai integrasi. **Pengecualian: tugas 1.1 (migrasi skema) dikerjakan paling awal**, karena seed 0.5 butuh enum `LEGAL` dan deploy 0.8 menjalankan migrasi.

| # | Tugas | Ukuran | Dependensi |
|---|---|---|---|
| 0.1 | `.env.example` (nama variabel saja), hapus fallback JWT, validasi env saat start | S | none |
| 0.2 | `helmet`, `express-rate-limit` (auth: ketat; umum: longgar), `morgan` | S | none |
| 0.3 | Util pagination + field `meta` di `sendSuccess`, terapkan ke semua list yang ada | M | none |
| 0.4 | Script `package.json`: `postinstall: prisma generate`, `start` menjalankan `prisma migrate deploy` lalu server, `seed` | S | none |
| 0.5 | Seed: akun ADMIN (email dan password dari env, **`isVerified=true`**, role `ADMIN`), konten panduan (≥ 8 item termasuk kategori `LEGAL`), user demo | M | 0.4, 1.1 |
| 0.6 | Pastikan `role` ikut di respons `login` dan `profile` (login satu pintu user/admin) | S | none |
| 0.7 | `resend-verification`, halaman reset password, flag `AUTO_VERIFY_EMAIL` | M | none |
| 0.8 | **Deploy** (Render/Railway/Fly/VPS, DB tetap Neon), pastikan HTTPS dan base URL stabil. Set kredensial admin lewat env di hosting, bukan di repo | M | 0.1-0.7 |
| 0.9 | Postman collection (env: `baseUrl`, `accessToken`; sertakan request login admin) + README akar `backend/` | M | 0.8 |

### Fase 1: Fitur inti (nilai utama aplikasi)

| # | Tugas | Ukuran | Dependensi |
|---|---|---|---|
| 1.1 | **Migrasi tunggal berisi semua perubahan skema bagian 2.1-2.4**: enum baru dan nilai enum tambahan, `User.locationUpdatedAt`, `SharingSession` (expiresAt, destinasi), `IncidentReport` (status, statusNote, isAnonymous, index), model `SosAlert`, `Facility`, `ReportLike`, `ReportComment`. **Dikerjakan paling awal (giliran T1) dan di-merge secepatnya** | M | none |
| 1.2 | Lokasi live: `PUT /api/location`, `GET /sharing-sessions/:id/location`, `ownerLocation` di `/active`, expiry lazy, hapus lokasi saat stop, `arrived` | L | 1.1, 0.3 |
| 1.3 | Modul SOS (model, service, endpoint, notifikasi ke teman dan kontak yang cocok, `shareText`, idempoten, rate limit) + util normalisasi nomor | L | 1.1 |
| 1.4 | Feed publik laporan + `/mine` + filter jarak/kategori + status; ubah aturan baca detail | L | 1.1, 0.3 |
| 1.5 | Perbarui `PROGRESS.md`, `API_REFERENCE.md`, `CONVENTIONS.md`, Postman | S | 1.2-1.4 |

### Fase 2: Peta dan admin

| # | Tugas | Ukuran | Dependensi |
|---|---|---|---|
| 2.1 | Model `Facility` + seed (≥ 50 fasilitas Jabodetabek) + `GET /api/facilities` (Haversine) | L | Fase 1 |
| 2.2 | `GET /api/reports/hotspots` | M | 1.4 |
| 2.3 | Endpoint admin: stats, moderasi laporan + update status (+ notifikasi `REPORT`), SOS list, users list, CRUD fasilitas | L | 1.3, 1.4, 2.1 |

### Fase 3: Pelengkap, kualitas, dan deliverable

| # | Tugas | Ukuran | Dependensi |
|---|---|---|---|
| 3.1 | Like dan komentar laporan | M | 1.4 |
| 3.2 | Seed data demo (laporan tersebar untuk feed dan hotspots, SOS contoh) | M | 2.2 |
| 3.3 | Database export: `pg_dump` ke `database/resq_dump.sql` (struktur + data seed), dokumentasikan cara restore | S | 3.2 |
| 3.4 | Test otomatis minimal (auth, reports, SOS) | M | Fase 1 |
| 3.5 | Swagger/OpenAPI (opsional, hanya bila waktu tersisa) | M | semua endpoint stabil |
| 3.6 | Uji alur penuh dari perangkat nyata sebelum demo (daftar → verifikasi → SOS → sesi berbagi → laporan → admin ubah status) | M | semua |

### Pembagian Tugas (Final)

**Peran:** **BE-A** = pemilik awal kode backend (paling paham seluruh modul). **BE-B** = rekan BE yang menerima handover.
Prinsip pembagian: BE-A memegang tugas lintas modul yang berisiko (skema, lokasi, SOS, deploy); BE-B memegang tugas yang lebih terpisah (pagination, feed, fasilitas, admin laporan). Satu fitur dimiliki satu orang dari awal sampai akhir agar tidak ada konflik di file yang sama.

| ID | Tugas | Pemilik | Ukuran |
|---|---|---|---|
| 1.1 | Migrasi skema (enum, field baru, `IncidentReport`, `SharingSession`) | **BE-A** | M |
| 0.1 | `.env.example`, hapus fallback JWT, validasi env | **BE-A** | S |
| 0.4 | Script `package.json` (postinstall, start, seed) | **BE-A** | S |
| 0.6 | `role` di respons login dan profil | **BE-A** | S |
| 0.5 | Seed (admin, panduan, user demo) | **BE-A** | M |
| 0.8 | Deploy | **BE-A** | M |
| 1.2 | Lokasi live + Safe Companion | **BE-A** | L |
| 1.3 | Modul SOS + util normalisasi nomor | **BE-A** | L |
| 2.3 (bagian A) | Admin: `/api/admin/sos` dan `/api/admin/stats` | **BE-A** | M |
| 3.2 | Seed data demo (laporan, SOS contoh) | **BE-A** | M |
| 3.3 | Database export (`pg_dump`) | **BE-A** | S |
| 3.4 | Test otomatis minimal | **BE-A** | M |
| 0.2 | `helmet`, rate limit, `morgan` | **BE-B** | S |
| 0.3 | Pagination + `meta` (semua list yang ada) | **BE-B** | M |
| 0.7 | `resend-verification`, halaman reset password, flag `AUTO_VERIFY_EMAIL` | **BE-B** | M |
| 0.9 | Postman collection + README akar `backend/` | **BE-B** | M |
| 1.4 | Feed publik laporan, `/mine`, status, filter jarak | **BE-B** | L |
| 2.1 | Model dan endpoint fasilitas + seed fasilitas | **BE-B** | L |
| 2.2 | Hotspots (area rawan) | **BE-B** | M |
| 2.3 (bagian B) | Admin: `/api/admin/reports*`, `/api/admin/users`, CRUD fasilitas | **BE-B** | M |
| 3.1 | Like dan komentar laporan | **BE-B** | M |
| 3.5 | Swagger/OpenAPI (opsional) | **BE-B** | M |
| 1.5, 3.6 | Dokumentasi per tugas (masing-masing untuk tugasnya) dan uji alur penuh sebelum demo | **Bersama** | S, M |

Perkiraan beban (S = 0,5, M = 1, L = 2 hari kerja, kasar): BE-A ≈ 12,5, BE-B ≈ 11,5. Cukup seimbang. Jadwal kalender ditentukan setelah deadline pengumpulan jelas.

**Urutan eksekusi:** mengikuti **estafet** di `docs/WORKFLOW.md` (17 giliran, T1-T17; satu orang bekerja, push dan merge, baru rekan mulai). Tabel giliran di sana adalah satu-satunya urutan yang berlaku, dan memetakan setiap ID tugas di roadmap ini ke sebuah giliran.

**Aturan kolaborasi:**
- Satu branch per tugas, bernama `feat/<ID>-<ringkasan>` (contoh: `feat/1.3-sos`). Merge lewat PR, **direview silang** oleh orang lain.
- **Hanya BE-A** yang mengubah `schema.prisma` dan membuat migrasi. Seluruh skema dibuat sekaligus di giliran T1. Bila nanti skema ternyata kurang, BE-A membuat giliran khusus berisi satu migrasi kecil (lihat `WORKFLOW.md` bagian 7).
- File rawan bentrok: `schema.prisma`, `app.js` (registrasi route), `notification.service`, `utils/response.js`, `PROGRESS.md`, `API_REFERENCE.md`. Buat PR kecil dan merge sering.
- Endpoint admin dipisah per file router (misalnya `admin-sos.routes.js`, `admin-report.routes.js`, `admin-user.routes.js`, `admin-stats.routes.js`) dan didaftarkan di bawah prefix `/api/admin`, agar BE-A dan BE-B tidak mengedit file yang sama.
- Setiap PR menyertakan pembaruan `PROGRESS.md`, `API_REFERENCE.md`, dan request Postman untuk tugas tersebut (lihat Definition of Done).
- Hambatan atau perubahan kontrak API dikabarkan ke FE sebelum merge.

---

## 5. Catatan Implementasi

- **Haversine di Prisma:** pakai `prisma.$queryRaw` dengan *bounding box* di `WHERE` (memanfaatkan index `[latitude, longitude]`), lalu hitung jarak dengan rumus Haversine di `SELECT`, urutkan, dan beri `LIMIT`. Selalu gunakan parameter ter-bind (jangan menyambung string SQL). Konversi `Decimal` ke `Number` di respons seperti pada modul laporan.
- **Hotspots:** `GROUP BY ROUND(latitude / 0.005), ROUND(longitude / 0.005)` pada laporan dalam radius dan rentang `days`. Ambang `level` disimpan sebagai konstanta (misalnya `count ≥ 10` = `HIGH`, `≥ 4` = `MEDIUM`).
- **Normalisasi nomor telepon (util baru):** buang spasi dan tanda baca; `08…` → `+628…`; `628…` → `+628…`. Pakai hasilnya untuk mencocokkan `EmergencyContact.phoneNumber` dengan `User.phoneNumber`. Catat bahwa nomor pada DB bisa berformat beragam, jadi normalkan **kedua** sisi saat membandingkan.
- **Privasi lokasi:** lokasi hanya diterima saat ada sesi aktif; viewer hanya bisa membaca lokasi lewat sesi yang menyertakannya; lokasi dihapus saat sesi berhenti atau kedaluwarsa; endpoint tidak pernah mengembalikan `lastLatitude/lastLongitude` pada profil publik user lain. Bila perilaku `getProfile` milik sendiri tetap menampilkannya, itu tidak masalah.
- **Penyalahgunaan:** rate limit pada SOS, friend request, laporan, dan komentar. SOS salah tekan ditangani dengan status `CANCELLED`, bukan penghapusan, agar ada jejak.
- **Anonimitas laporan:** `author` di-`null`-kan di feed dan detail untuk selain pemilik dan admin. Pemilik dan admin tetap dapat melihat identitas.
- **Notifikasi:** tetap best-effort dan in-app. FE memakai polling `GET /api/notifications/unread-count` (misalnya tiap 10-15 detik). FCM hanya dipertimbangkan bila waktu sangat longgar.
- **Data fasilitas:** untuk seed, kumpulkan sekali (manual atau dari OpenStreetMap lewat Overpass) ke `prisma/seed/facilities.json`, lalu commit. Bila memakai data OSM, cantumkan atribusi "© OpenStreetMap contributors" di laporan.
- **Konten panduan:** verifikasi nomor hotline dan prosedur ke sumber resmi sebelum dimasukkan ke seed.
- **Konvensi tetap berlaku:** urutan modul baru = validation → service → controller (`asyncHandler`) → routes → daftarkan di `app.js` → migrasi → perbarui dokumen. Jangan menaruh query Prisma di controller.

## 6. Definition of Done (per tugas)

1. Mengikuti `CONVENTIONS.md` (layer, `ServiceError`, `sendSuccess`/`sendError`, status code konsisten).
2. Validasi input lengkap, error 4xx yang jelas, tidak ada field sensitif di respons.
3. Pengecekan hak akses (pemilik/viewer/admin) tercakup.
4. Migrasi ter-commit, `prisma generate` berjalan.
5. `PROGRESS.md`, `API_REFERENCE.md`, dan Postman collection diperbarui.
6. Sudah dicoba manual end-to-end dari Postman (atau dari FE bila sudah terhubung).
7. Tidak ada secret yang ter-commit (repo akhir harus public).

## 7. Sinkronisasi dengan FE dan UI/UX

- Setelah Fase 0 selesai, bagikan **base URL + Postman collection** ke FE. Menurut riwayat Git, FE baru memiliki tampilan beranda dan layar login/register, jadi integrasi auth bisa langsung dimulai.
- Bagikan spesifikasi bagian 3 ke UI/UX agar desain layar SOS, peta (filter fasilitas + hotspots), sesi berbagi (tujuan, durasi), feed laporan (anonim, status, like/komentar), dan layar admin sesuai kemampuan API.
- **Layar admin (di dalam aplikasi yang sama)** dirancang UI/UX berdasarkan endpoint di 3.6. Usulan cakupan minimal yang berbasis daftar:
  1. dashboard statistik (`/api/admin/stats`),
  2. moderasi laporan dan ubah status,
  3. pemantau SOS,
  4. kelola fasilitas,
  5. kelola panduan,
  6. daftar user (hanya lihat).
- **Alur login:** satu layar login untuk user dan admin. FE mengarahkan berdasarkan `role` pada respons login. Tidak ada pendaftaran admin dari aplikasi.
- **Prioritas FE:** karena FE hanya satu orang, kerjakan layar admin **setelah** fitur user inti (SOS, peta, laporan, sesi berbagi) berjalan. Mulai dari daftar laporan + ubah status dan daftar SOS, karena paling terlihat saat demo.
- **Kredensial admin untuk penilai:** repo akan public, jadi jangan menaruh password admin di repo. Cantumkan akun admin demo (password sekali pakai khusus demo) di laporan final atau video demo, dan set nilainya lewat env di hosting.
