# ResQ Backend — Konvensi & Pola

Dokumen ini menjaga konsistensi gaya kode saat berganti kontributor atau AI Agent. **Ikuti pola yang sudah ada di kode sebelum memperkenalkan pola baru.**

## Prinsip Arsitektur

1. **Pemisahan lapisan ketat:**
   - `routes/` hanya mendefinisikan endpoint + memasang middleware.
   - `controllers/` hanya menangani `req`/`res`, memanggil service, memformat respons. **Tidak ada logika bisnis atau query Prisma langsung.**
   - `services/` berisi semua logika bisnis dan akses database. **Tidak pernah menyentuh `req`/`res`.**
   - `validations/` memvalidasi & mensanitasi input sebelum masuk service.
2. **Error service pakai `ServiceError` bersama.** Service melempar `ServiceError(message, statusCode, errors?)` dari `utils/service-error.js` (satu definisi, di-import & di-ekspor ulang tiap service). Error diteruskan ke **global error handler** (`middleware/error.middleware.js`) yang memetakannya ke respons standar.
3. **Respons selalu lewat helper** `sendSuccess` / `sendError` di `utils/response.js`, bukan `res.json()` manual yang tak konsisten.
4. **Controller dibungkus `asyncHandler`** (`utils/async-handler.js`) agar promise yang reject diteruskan ke `next()` → error handler global. (Pengecualian historis: controller **auth** masih memakai `try/catch` lokal; perilaku respons identik.)

## Konvensi Penamaan

- **File:** `kebab-case` dengan sufiks peran — `*.controller.js`, `*.service.js`, `*.routes.js`, `*.validation.js`, `*.middleware.js`.
- **Model Prisma:** `PascalCase` + field `camelCase`, tapi dipetakan ke kolom/tabel `snake_case` lewat `@map()` / `@@map()`.
- **Variabel & fungsi JS:** `camelCase`.
- **Enum:** nilai `UPPER_SNAKE_CASE`.

## Gaya Kode

- Modul memakai **CommonJS** (`require` / `module.exports`), bukan ESM. (`"type": "commonjs"` di `package.json`.)
- Tulis **komentar JSDoc** untuk fungsi service dan helper, seperti pada `auth.service.js`.
- Beri komentar penjelas pada bagian yang tidak jelas niatnya (mis. alasan keamanan, trik token).
- Jaga agar fungsi fokus pada satu tanggung jawab.

## Pola Konkret yang Harus Diikuti (diverifikasi dari kode)

### Validator
- Buatan sendiri, **tanpa library eksternal**. Setiap fungsi menerima `body` dan mengembalikan:
  ```js
  { valid: boolean, errors: { field: "pesan" }, data: { /* nilai bersih */ } }
  ```
- `data` berisi nilai yang sudah di-trim/di-normalisasi (email lowercase, koordinat jadi number, dll).
- Update parsial: izinkan subset field, tapi wajib minimal satu; jika kosong beri error `_`.

### ServiceError (bersama)
- **Satu kelas `ServiceError`** di `utils/service-error.js` (message, statusCode, errors). Setiap service meng-import lalu **meng-ekspor ulang** (`module.exports = { ServiceError, ... }`) agar import lama seperti `authService.ServiceError` dan cek `instanceof` tetap bekerja.
- Service `throw new ServiceError(...)`; global error handler memetakannya ke `sendError`.

### Pola controller
- Bungkus handler dengan **`asyncHandler`**; error (termasuk `ServiceError`) otomatis diteruskan ke error handler global. Tidak perlu lagi `try/catch` atau `handleError` lokal untuk modul baru.
- Validasi dulu → jika gagal kembalikan **422** `"Validation failed."` dengan `errors` (ini tetap dilakukan eksplisit di dalam handler).
- Error tak terduga ditangani terpusat oleh `errorHandler` → `console.error(...)` + **500** generik `"Something went wrong."`.
- Pengecualian: controller **auth** masih memakai `try/catch` lokal (tidak di-refactor; respons tetap sama).

### Kepemilikan data (ownership)
- Data selalu diambil lalu dicek `record.userId === req.user.id`.
- Jika tidak ada **atau** milik user lain → **404** yang sama (jangan bedakan, agar tidak membocorkan keberadaan data).

### Status code yang konsisten
- Create sukses → **201**; operasi lain sukses → **200**.
- Validasi gagal → **422**; konflik unik → **409**; auth gagal → **401**; terlarang → **403**; tidak ada → **404**; gagal dependensi eksternal (Cloudinary) → **502**.

## Pola Keamanan yang Harus Dipertahankan

- **Jangan pernah mengembalikan `password`** dalam respons. Pakai pola `toPublicUser()` atau `select` eksplisit.
- **Cegah enumerasi akun:** pesan error untuk login gagal & forgot-password dibuat generik.
- **Password di-hash** dengan bcrypt (`utils/hash.js`), tidak pernah disimpan plaintext.
- **Refresh token disimpan di DB** agar bisa di-revoke saat logout/reset.
- Data pengguna **selalu terikat ke pemilik** (`userId`), satu user tak boleh mengakses data user lain.
- Token verifikasi email & reset password bersifat **stateless (JWT)** dengan klaim `type` — selalu cek `type` saat verifikasi agar token tak bisa dipakai lintas-tujuan.
- Jangan commit `.env` atau menampilkan nilai rahasia di log/respons.
- Semua `JWT_*_SECRET` (dan `DATABASE_URL`) **wajib diisi di semua environment** (development, test, production). Server menolak start tanpanya (`config/env.js`), dan `utils/jwt.js` tidak memiliki fallback. Jangan menambahkan nilai default untuk secret.

## Penambahan Fitur Baru (checklist)

Saat menambah entitas/fitur baru (mis. Friendship), buat berurutan:

1. **Validation** di `src/validations/<fitur>.validation.js`.
2. **Service** di `src/services/<fitur>.service.js` (logika + Prisma, lempar `ServiceError`).
3. **Controller** di `src/controllers/<fitur>.controller.js` (bungkus dengan `asyncHandler`, panggil service, pakai `sendSuccess`/`sendError`). Import `ServiceError` dari `utils/service-error.js`.
4. **Routes** di `src/routes/<fitur>.routes.js` (pasang `authenticate` / `authorize`/`isAdmin` sesuai kebutuhan). Deklarasikan path literal (mis. `/search`, `/active`) sebelum route `/:id`.
5. **Daftarkan route** di `src/app.js` dengan prefix `/api/<fitur>`.
6. Jika mengubah skema → `prisma migrate dev` + `prisma generate`.
7. **Perbarui `docs/PROGRESS.md` dan `docs/API_REFERENCE.md`.**

## Database & Migrasi

- Skema adalah **sumber kebenaran model** di `prisma/schema.prisma`.
- Ubah skema → buat migrasi (`npx prisma migrate dev --name <deskripsi>`), jangan edit SQL migrasi lama secara manual.
- Relasi memakai `onDelete: Cascade` di banyak tempat; hapus user akan menghapus data terkait otomatis.

## Yang Harus Dihindari

- Query Prisma di dalam controller.
- Logika bisnis di dalam route atau validation.
- Mengembalikan field sensitif.
- Memperkenalkan library/paket baru tanpa alasan kuat jika sudah ada yang setara.
- Membiarkan dokumentasi `docs/` basi setelah perubahan kode.
