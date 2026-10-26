# ResQ Backend: Workflow Estafet (Push → Giliran Berikutnya)

Dokumen ini mengatur **siapa bekerja kapan** agar dua orang BE tidak saling konflik. Simpan sebagai `docs/WORKFLOW.md`. Daftar tugas, spesifikasi endpoint, dan skema ada di `docs/ROADMAP.md`.

**Peran:** **BE-A** = pemilik awal kode backend. **BE-B** = rekan BE yang menerima handover.

---

## 1. Prinsip Estafet

Hanya **satu orang yang memegang "baton"** pada satu waktu. Baton pindah **hanya setelah** pekerjaan pemegang baton ter-push, direview, dan di-**merge** ke `main`.

```
 BE-A kerja ──push + PR──▶ BE-B review & test ──merge──▶ BE-B pull main ──▶ BE-B kerja ──push + PR──▶ BE-A review & test ──merge──▶ ...
   (baton A)                 (menunggu produktif)              (baton B)                                  (menunggu produktif)
```

Aturan emas:
1. **Yang tidak memegang baton tidak mengubah kode backend.** Ia menunggu secara produktif (lihat bagian 5).
2. **Baton pindah saat PR di-merge**, bukan saat "sudah push". Yang me-merge adalah **reviewer** setelah menyetujui.
3. **Selalu mulai dari `main` terbaru:** `git pull` dulu sebelum membuat branch.
4. **Satu giliran = satu branch = satu PR.**
5. **Skema database (`schema.prisma`) hanya diubah di giliran T1 oleh BE-A**, dalam satu migrasi yang memuat semua perubahan skema di ROADMAP bagian 2. Setelah itu, tidak ada migrasi baru kecuali lewat giliran khusus (bagian 7).
6. **Database dev tidak dipakai bersama** (lihat bagian 4).

---

## 2. Urutan Giliran

Total 17 giliran. BE-A memegang giliran ganjil, BE-B memegang giliran genap, kecuali T17 yang dikerjakan bersama.

| Giliran | Pemegang | Tugas (ID roadmap) | Prasyarat (sudah ter-merge) | Gerbang serah-terima (khusus giliran ini) | Output untuk FE |
|---|---|---|---|---|---|
| **T1** | BE-A | **1.1** Migrasi skema tunggal (semua enum, field, dan model: `SosAlert`, `Facility`, `ReportLike`, `ReportComment`, dst.) | none | `migrate reset` + `migrate dev` sukses di DB dev kosong; `prisma generate`; app start; login dan list kontak masih berfungsi (smoke test) | none |
| **T2** | BE-B | **0.2** helmet, rate limit, morgan. **0.3** pagination + `meta` | T1 | Semua endpoint list (reports, notifications, guides, friendships, requests, users/search, users/:id/friends, sharing-sessions/active) mengembalikan `meta`; `limit` maks 50; rate limit auth aktif | Format `meta` pagination |
| **T3** | BE-A | **0.1** env + hapus fallback JWT. **0.4** script `package.json`. **0.6** `role` di login/profil. **0.5** seed | T2 | App gagal start bila `JWT_*` kosong (production); `.env.example` lengkap; `npm run seed` aman dijalankan ulang; login admin mengembalikan `role: ADMIN`; panduan seed ≥ 8 item termasuk `LEGAL` | `role` di respons login |
| **T4** | BE-B | **0.7** `resend-verification`, halaman reset password, flag `AUTO_VERIFY_EMAIL` | T3 | Halaman reset terbuka di browser HP dan berhasil mengubah password; link di email benar; flag default `false` | Alur verifikasi dan reset |
| **T5** | BE-A | **0.8** Deploy | T4 | URL HTTPS hidup; `GET /` OK; migrasi + seed terterap di **DB produksi** (bukan DB dev); login admin berhasil di server; env diisi di hosting; tidak ada secret di repo | **Base URL** |
| **T6** | BE-B | **0.9** Postman collection + README akar `backend/` | T5 | Collection mencakup semua endpoint yang ada + login admin; README bisa diikuti orang baru dari nol | **Postman collection** |
| | | 🏁 **Milestone Fase 0 selesai.** Beri tag `fase-0`, bagikan base URL dan Postman ke FE. | | | |
| **T7** | BE-A | **1.3** SOS (+ util normalisasi nomor telepon) | T6 | Idempoten; rate limit 5/menit; notifikasi ke teman dan kontak yang cocok; `shareText` benar; riwayat paginated | Endpoint SOS |
| **T8** | BE-B | **1.4** Feed publik laporan, `/mine`, status, filter jarak, aturan baca detail | T7 | `GET /reports` = feed publik; `/mine` = milik sendiri; PUT/DELETE tetap hanya pemilik; anonim menyembunyikan `author`; konvensi baru dicatat di `CONVENTIONS.md` | Endpoint feed |
| **T9** | BE-A | **1.2** Lokasi live + Safe Companion | T8 | `PUT /location` ditolak 409 tanpa sesi; viewer bisa baca lokasi owner; lokasi di-null saat stop atau kedaluwarsa; `arrived` memicu notifikasi | Endpoint lokasi dan sesi |
| | | 🏁 **Milestone Fase 1 selesai** (SOS, feed, lokasi live). Tag `fase-1`. | | | |
| **T10** | BE-B | **2.1** Fasilitas: `GET /facilities` (Haversine) + seed ≥ 50 fasilitas | T9 | Hasil terurut jarak dengan `distanceKm`; query pakai parameter ter-bind; seed aman diulang | Endpoint fasilitas |
| **T11** | BE-A | **2.3 (bagian A)** `/api/admin/sos`, `/api/admin/stats` | T10 | Hanya ADMIN (403 untuk USER, 401 tanpa token); router admin terpisah file | Endpoint admin SOS dan statistik |
| **T12** | BE-B | **2.2** Hotspots. **2.3 (bagian B)** admin laporan (+ ubah status memicu notifikasi `REPORT`), admin users, CRUD fasilitas | T11 | Ubah status laporan menghasilkan notifikasi ke pelapor; user biasa ditolak 403 | Endpoint hotspots dan admin |
| | | 🏁 **Milestone Fase 2 selesai.** Tag `fase-2`. | | | |
| **T13** | BE-A | **3.4** Test otomatis minimal (auth, reports, SOS) | T12 | `npm test` hijau | none |
| **T14** | BE-B | **3.1** Like dan komentar laporan | T13 | Like idempoten; komentar maks 500 karakter; hapus komentar oleh penulis atau admin | Endpoint like/komentar |
| **T15** | BE-A | **3.2** Seed data demo. **3.3** Database export | T14 | Feed, hotspots, dan SOS punya data demo; `database/resq_dump.sql` bisa direstore di DB kosong | none |
| **T16** | BE-B | **3.5** Swagger/OpenAPI (opsional, lewati bila waktu mepet) | T15 | `/api-docs` memuat semua endpoint | Dokumentasi interaktif |
| **T17** | **Bersama** | **3.6** Uji alur penuh dari perangkat nyata, rapikan dokumentasi, tag `final` | T16 (atau T15 bila T16 dilewati) | Daftar → verifikasi → SOS → sesi berbagi → laporan → admin ubah status berhasil di server produksi | none |

Perkiraan beban kasar per giliran (S = 0,5, M = 1, L = 2 hari kerja): T1 ≈ 1, T2 ≈ 1,5, T3 ≈ 2,5, T4 ≈ 1, T5 ≈ 1, T6 ≈ 1, T7 ≈ 2, T8 ≈ 2, T9 ≈ 2, T10 ≈ 2, T11 ≈ 1,5, T12 ≈ 2,5, T13 ≈ 1, T14 ≈ 1, T15 ≈ 1,5, T16 ≈ 1, T17 ≈ 1. Total ≈ 26 hari kerja bila benar-benar bergantian. Lihat bagian 8 bila waktu terlalu sempit.

---

## 3. Prosedur Satu Giliran

### 3.1 Saat menerima baton (awal giliran)

```bash
git checkout main
git pull origin main
git checkout -b feat/T7-sos            # format: feat/T<nomor>-<ringkasan>
npm ci
npx prisma generate
npm run dev                            # pastikan jalan sebelum mengubah apa pun
```

Baca bagian roadmap yang relevan dan `docs/CONVENTIONS.md`.

### 3.2 Saat bekerja

- Commit kecil dan sering, dengan ID tugas:
  `feat(1.3): tambah model dan service SOS`, `fix(1.3): perbaiki validasi koordinat`, `docs(1.3): perbarui API_REFERENCE`.
- **Jangan menyentuh pekerjaan di luar tugas giliran ini**, termasuk "perbaikan kecil" di modul lain. Catat temuan sebagai issue atau di catatan PR.
- **Jangan mengubah `schema.prisma`** (kecuali T1). Bila skema ternyata kurang, hentikan dan lihat bagian 7.
- Untuk tugas baru, ikuti urutan di `CONVENTIONS.md`: validation → service → controller (`asyncHandler`) → routes → daftarkan di `app.js` → dokumen.

### 3.3 Gerbang serah-terima (wajib lengkap sebelum membuka PR)

- [ ] `npm run dev` berjalan tanpa error, `GET /` OK.
- [ ] Endpoint tugas diuji di Postman: kasus sukses **dan** error (401, 403, 404, 409, 422 yang relevan). Request ditambahkan ke Postman collection (setelah T6).
- [ ] Gerbang khusus giliran (kolom tabel di bagian 2) terpenuhi.
- [ ] Tidak ada `.env`, secret, atau nilai rahasia di diff (`git diff main --stat` dan periksa manual).
- [ ] Mengikuti `CONVENTIONS.md` (layer, `ServiceError`, `sendSuccess`/`sendError`, status code).
- [ ] `docs/PROGRESS.md` dan `docs/API_REFERENCE.md` diperbarui dengan ID tugas.
- [ ] Papan Baton (bagian 6) diperbarui.

### 3.4 Membuka PR dan mengoper baton

```bash
git push -u origin feat/T7-sos
```

Buka PR ke `main` dengan judul `[T7] SOS (1.3)` dan isi:

```markdown
## Tugas
T7: 1.3 SOS

## Yang dikerjakan
- ...

## Cara menguji
1. Login sebagai user ...
2. POST /api/sos ...

## Perubahan yang berdampak ke FE
- ...

## Catatan / hal yang belum selesai
- ...
```

Kirim pesan serah-terima ke rekan:

```
[BATON-REVIEW] T7 → BE-B
PR: <link>
Selesai: 1.3 (SOS)
Perlu dites: POST /api/sos (idempoten), PATCH /api/sos/:id/resolve
Setelah merge, giliranmu: T8 (1.4 Feed laporan)
```

### 3.5 Review (dikerjakan penerima baton selagi menunggu)

Reviewer memeriksa:
- [ ] Kode mengikuti `CONVENTIONS.md`; tidak ada query Prisma di controller.
- [ ] Validasi input dan pengecekan kepemilikan/role benar; respons tidak membocorkan field sensitif.
- [ ] Endpoint dites sendiri di Postman (jangan hanya membaca kode).
- [ ] Tidak ada secret; dokumen dan Postman sudah diperbarui.

Bila ada masalah: tulis komentar di PR, pemegang baton memperbaiki di branch yang sama dan push ulang. **Baton belum pindah** sampai merge.
Bila lolos: reviewer **Approve → Squash and merge → hapus branch**, lalu langsung menjadi pemegang baton:

```bash
git checkout main
git pull origin main
```

Kirim pesan singkat: `[BATON-DITERIMA] T7 ter-merge. Aku mulai T8.`

### 3.6 Setelah milestone

Beri tag di `main` setelah giliran penutup milestone di-merge:

```bash
git tag fase-0 && git push origin fase-0     # fase-1 setelah T9, fase-2 setelah T12, final setelah T17
```

---

## 4. Database dan Environment (agar tidak saling menimpa)

- **Setiap orang memakai database dev sendiri.** Pakai *branch* Neon per orang (atau PostgreSQL lokal). `DATABASE_URL` di `.env` masing-masing berbeda.
- **Database produksi (demo) hanya disentuh oleh deploy** (`prisma migrate deploy`), tidak pernah lewat `prisma migrate dev` atau `db push` dari laptop.
- `.env` tidak pernah di-commit. Bila ada variabel baru, tambahkan **namanya** ke `.env.example` di PR yang sama, dan beri tahu rekan lewat pesan serah-terima.
- Kredensial (Cloudinary, SMTP, secret JWT) dibagikan lewat kanal privat, bukan lewat repo.

---

## 5. Menunggu Giliran: Apa yang Dikerjakan

Orang yang sedang tidak memegang baton **tidak menganggur**. Prioritas:

1. **Mereview dan mengetes PR** pemegang baton (ini yang utama, dan memang syarat untuk baton berpindah).
2. **Mendukung FE:** menjawab pertanyaan API, mencoba endpoint baru bersama FE, memperbaiki Postman bila perlu (catat sebagai saran, jangan edit repo di luar giliran).
3. **Menyiapkan bahan non-kode** untuk giliran berikutnya, di luar repo (Drive/Notion): isi konten panduan keselamatan, daftar fasilitas dan koordinat untuk seed, data demo.
4. **Menyusun laporan final:** Introduction, SDG Justification, Features Overview, Challenges & Learnings; menyiapkan skenario demo video.
5. **Mencatat temuan bug** di issue GitHub, bukan langsung memperbaiki.

---

## 6. Papan Baton (perbarui di setiap serah-terima)

Salin tabel ini ke `docs/WORKFLOW.md` atau ke Notion, lalu isi saat bekerja.

| Giliran | Pemegang | Status | PR | Tanggal merge | Catatan |
|---|---|---|---|---|---|
| T1 | BE-A | ⬜ | | | |
| T2 | BE-B | ⬜ | | | |
| T3 | BE-A | ⬜ | | | |
| T4 | BE-B | ⬜ | | | |
| T5 | BE-A | ⬜ | | | |
| T6 | BE-B | ⬜ | | | |
| T7 | BE-A | ⬜ | | | |
| T8 | BE-B | ⬜ | | | |
| T9 | BE-A | ⬜ | | | |
| T10 | BE-B | ⬜ | | | |
| T11 | BE-A | ⬜ | | | |
| T12 | BE-B | ⬜ | | | |
| T13 | BE-A | ⬜ | | | |
| T14 | BE-B | ⬜ | | | |
| T15 | BE-A | ⬜ | | | |
| T16 | BE-B | ⬜ | | | |
| T17 | Bersama | ⬜ | | | |

Legenda: ⬜ belum, 🔄 sedang dikerjakan (pemegang baton), 👀 menunggu review, ✅ ter-merge.

---

## 7. Situasi Khusus

| Situasi | Tindakan |
|---|---|
| **Bug ditemukan di pekerjaan rekan** | Tulis issue atau komentar PR. Jangan memperbaiki langsung. Pemilik giliran yang bersangkutan memperbaikinya di giliran berikutnya, atau di *giliran hotfix* bila mendesak. |
| **Hotfix mendesak** (server demo bermasalah) | Sepakati lewat pesan siapa yang memegang baton hotfix. Buat branch `hotfix/<ringkasan>`, perbaikan sekecil mungkin, PR direview rekan, merge, lalu kembali ke urutan semula. Catat di Papan Baton. |
| **Skema ternyata kurang setelah T1** | Hentikan giliran. BE-A membuat **giliran khusus** `S<n>`: satu migrasi kecil, PR, merge, baru giliran dilanjutkan. Jangan membuat migrasi di branch tugas biasa. |
| **Kontrak API berubah** (nama field, status code) | Kabari FE **sebelum** merge, dan catat di bagian "Perubahan yang berdampak ke FE" pada PR. |
| **Merge conflict tetap muncul** | Seharusnya tidak terjadi dalam estafet. Bila muncul, pemegang baton yang menyelesaikan dengan `git pull --rebase origin main`, lalu tes ulang sebelum push. |
| **Pemegang baton tidak bisa lanjut** (sakit, ujian) | Kabari rekan. Baton boleh dialihkan dengan sepakat bersama, dicatat di Papan Baton. |
| **Reviewer lambat** | Batas wajar: 1 hari kerja. Bila lewat, tanyakan langsung. Jangan merge sendiri tanpa review. |

---

## 8. Opsi Percepatan (hanya bila waktu terlalu sempit)

Estafet murni paling aman dari konflik, tetapi total waktunya ≈ 26 hari kerja karena tidak ada pekerjaan paralel. Bila deadline tidak cukup, pasangan giliran berikut menyentuh **modul yang berbeda** dan boleh dikerjakan **paralel**, setelah syarat awalnya ter-merge:

| Pasangan paralel | Syarat awal | Mengapa relatif aman |
|---|---|---|
| T7 (SOS) dan T8 (feed laporan) | T6 ter-merge | Modul berbeda (`sos` vs `reports`) |
| T9 (lokasi live) dan T10 (fasilitas) | T8 ter-merge | Modul berbeda (`sharing-session`/`location` vs `facility`) |
| T11 (admin SOS dan stats) dan T12 (hotspots dan admin laporan) | T10 ter-merge | Router admin dipisah per file |

Aturan tambahan bila memakai opsi ini:
1. Kedua orang mulai dari `main` yang sama (hasil merge syarat awal).
2. **Merge bergantian, bukan bersamaan.** Yang merge kedua wajib `git pull --rebase origin main` dulu, menyelesaikan konflik (biasanya hanya di `app.js`, `PROGRESS.md`, `API_REFERENCE.md`, atau Postman collection), lalu tes ulang.
3. Perubahan di file bersama (`app.js`, `utils/response.js`, `notification.service`, `schema.prisma`) tidak dilakukan di giliran paralel.
4. Pasangan lain (T1-T6, T13-T17) tetap estafet murni karena menyentuh lintas modul.

Dengan opsi ini total waktu turun menjadi sekitar 20 hari kerja. Putuskan bersama sebelum T6 selesai, dan catat keputusannya di Papan Baton.
