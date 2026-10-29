# ResQ Backend — Dokumentasi Proyek

Folder `docs/` adalah sumber dokumentasi proyek. Tujuannya agar setiap developer baru dapat memahami proyek dengan cepat, menjaga konsistensi implementasi, dan memudahkan proses handover.

## Cara Pakai (untuk AI Agent & Developer)

1. Baca `PROJECT_OVERVIEW.md` lebih dulu untuk memahami tujuan dan arsitektur.
2. Baca `PROGRESS.md` untuk tahu status terkini (apa yang sudah selesai, sedang dikerjakan, dan belum).
3. Ikuti `CONVENTIONS.md` agar gaya kode dan pola tetap seragam.
4. Pakai `API_REFERENCE.md` sebagai acuan endpoint yang sudah ada.
5. **Setelah menyelesaikan pekerjaan apa pun, WAJIB perbarui `PROGRESS.md`** (dan file lain yang relevan) supaya dokumentasi tidak basi.

## Daftar File

| File | Isi |
| --- | --- |
| `PROJECT_OVERVIEW.md` | Deskripsi proyek, tujuan, tech stack, arsitektur, struktur folder |
| `PROGRESS.md` | Status pengerjaan per fitur: selesai / sebagian / belum |
| `API_REFERENCE.md` | Daftar endpoint API yang sudah diimplementasikan |
| `CONVENTIONS.md` | Aturan penulisan kode, pola, dan konvensi proyek |
| `RESQ_BACKEND_ROADMAP.md` | Rencana pengerjaan lanjutan, spesifikasi endpoint baru, dan pembagian tugas |
| `DEPLOYMENT.md` | Panduan deploy ke hosting cloud (Render, alternatif Railway), variabel lingkungan, verifikasi pasca-deploy, dan pemecahan masalah |

## Aturan Emas

> Jangan pernah menganggap dokumentasi ini otomatis akurat tanpa cek kode.
> Jika menemukan ketidaksesuaian antara dokumen dan kode, **kode adalah kebenaran** — lalu perbarui dokumen ini agar cocok.

## Status Akurasi

Dokumentasi ini disusun dengan membaca **seluruh** file di `src/` (controllers, services, validations, middleware, utils, config), `prisma/schema.prisma`, dan migrasi — bukan inferensi. Terakhir diverifikasi: 2026-10-25, setelah penyelesaian Phase 1 (Guide, Emergency Contact, Friendship, Notification, Sharing Session, User Utility) dan Phase 2 item 1-2 (Notification Business Integration, Global Error Handling). Yang masih tersisa: API Documentation interaktif (Swagger) dan automated testing. Jika kode berubah setelah tanggal ini, verifikasi ulang.
