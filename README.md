# Bluetooth Finder 📡

Aplikasi mobile (Android, Kotlin + Jetpack Compose) untuk melacak kedekatan perangkat
Bluetooth Low Energy (BLE) di sekitar. Aplikasi memindai paket *advertising* BLE secara
real-time, menampilkan daftar perangkat aktif beserta RSSI mentah dan estimasi jarak,
memungkinkan pencarian/filter, dan menyediakan layar **Radar View** untuk melacak satu
perangkat target berdasarkan kekuatan sinyalnya. Riwayat perangkat disimpan ke database
lokal (Room) sehingga tidak hilang saat aplikasi ditutup.

> Study case: **Mobile Engineer – 48 jam**

---

## 1. Fitur (pemetaan ke Core Requirements)

| # | Kebutuhan | Implementasi |
|---|-----------|--------------|
| 2.1 | Pemindaian perangkat | `AndroidBleScanner` memakai `BluetoothLeScanner` (mode *low latency*) dengan **throttle tampilan 1 detik per flush** agar data live tidak bergetar. Menampilkan **Nama**, **MAC Address/UUID**, **estimasi jarak**, dan **RSSI mentah (dBm)**. Tombol **Start/Scan** & **Stop** pada `BotBar`. Perangkat yang berhenti mengiklankan **tidak dihapus** — tetap tampil redup dengan "terakhir terlihat". |
| 2.2 | Filter & pengurutan | Daftar selalu diurutkan otomatis dari **RSSI terkuat** (EMA agar stabil), **perangkat hidup di atas yang hilang**. Search bar memfilter berdasarkan **nama** atau **MAC address**; chip filter membatasi **ambang minimum sinyal** (mis. hanya `≥ -80 dBm`, memakai RSSI mentah = angka di layar) dan **opsi menyembunyikan perangkat hilang**. |
| 2.3 | Pelacakan target / Radar | Tap satu perangkat → layar **Detail Pelacakan**: radar beranimasi, jarak & kategori berubah warna mengikuti tabel RSSI, plus kartu detail koneksi (RSSI raw/EMA, Tx Power, terakhir terdeteksi, status Live/Lost). Untuk perangkat tanpa nama (mis. HP), tersedia **pembacaan nama via koneksi GATT** (Device Name `0x2A00`). |
| 2.4 | Penyimpanan lokal | Room (`device_history`): nama, alamat, waktu pertama & terakhir terlihat, RSSI terakhir, RSSI terkuat, jumlah deteksi. |
| 3.1 | Screen 1: Dashboard (Scanner) | `HomeScreen` |
| 3.2 | Screen 2: Detail Pelacakan (Radar) | `DetailScreen` |
| 3.3 | Screen 3: Riwayat Perangkat (History Log) | `HistoryScreen` |
| 4.1 | Arsitektur MVVM + DI | `ui (Compose) → viewmodel → repository → data (BLE/Room)` + Hilt |
| 4.2 | Error handling & resilience | Izin ditolak, Bluetooth mati, `scan failed`, kehilangan sinyal → banner/snackbar dengan aksi perbaikan |
| 4.3 | App lifecycle | Scan **hanya** dikendalikan tombol Start/Stop (tidak auto-pause di background, sesuai brief); state scan hidup di singleton sehingga **rotasi layar maupun pindah layar tidak menghentikan scan** |

### Pemetaan RSSI → Kategori → Perkiraan Jarak (sesuai brief)

| RSSI (dBm) | Kategori Sinyal | Perkiraan Jarak |
|------------|-----------------|-----------------|
| -10 s/d -30 | Sangat Kuat (Sangat Dekat) | < 1 meter |
| -30 s/d -50 | Kuat (Dekat) | 1 – 3 meter |
| -50 s/d -70 | Cukup / Baik | 3 – 10 meter |
| -70 s/d -80 | Lemah | 10 – 20 meter |
| -80 s/d -90 | Sangat Lemah / Putus-putus | > 20 meter (batas jangkauan) |
| < -90 | Sinyal Hilang (Lost) | Terputus / di luar jangkauan |

Pemetaan ini dikodekan pada `domain/SignalCategory.kt` dan diuji lewat unit test.

---

## 2. Setup & Cara Menjalankan

### Prasyarat
- **Android Studio** (Ladybug/Meerkat atau lebih baru) dengan **JDK 17+** (build ini dijalankan dengan JDK 21).
- **Android SDK Platform 36** (compileSdk 36) dan Build-Tools yang sesuai.
- Perangkat fisik dengan BLE (**disarankan**, emulator umumnya tidak memiliki Bluetooth).
- `minSdk 24` (Android 7.0) s/d `targetSdk 36`.

### Langkah
```bash
git clone <url-repositori>
cd BluetoothFinder
./gradlew :app:assembleDebug          # build debug APK
./gradlew :app:testDebugUnitTest      # jalankan unit test
./gradlew :app:lintDebug              # static analysis
```
Buka project di Android Studio → **Run ▶** ke perangkat fisik.
Saat pertama kali menekan **Scan**, aplikasi akan meminta izin:
- Android 12+: `BLUETOOTH_SCAN` + `BLUETOOTH_CONNECT`
- Android 11 ke bawah: `ACCESS_FINE_LOCATION`

### Membuat APK rilis
```bash
./gradlew :app:assembleRelease
# hasil: app/build/outputs/apk/release/app-release-unsigned.apk
```
APK rilis masih perlu ditandatangani. Alternatif cepat untuk demo/uji coba:
gunakan **debug APK** (`app/build/outputs/apk/debug/app-debug.apk`) yang sudah
bisa langsung di-install (`adb install -r ...`).

> Untuk pengumpulan submission, artifact APK disimpan di folder
> [`artifacts/`](artifacts/) (`BluetoothFinder-v1.0-debug.apk`).

---

## 3. Panduan Uji Manual

1. **Scan** → tekan *Scan*; daftar perangkat muncul & terurut dari sinyal terkuat.
2. **Stop** → daftar membeku (hasil terakhir tetap bisa dilihat).
3. **Filter** → ketik nama/MAC pada kolom pencarian; pilih chip `≥ -80 dBm`; atau chip
   **Sembunyikan hilang** untuk menyembunyikan perangkat yang sinyalnya putus.
4. **Radar** → tap salah satu baris; radar menampilkan posisi target sesuai kategori
   sinyal. Mendekat/menjauh dari perangkat → warna, jarak, dan posisi titik berubah.
5. **Kehilangan sinyal** → jauhkan perangkat (atau matikan Bluetooth) → status berubah
   menjadi **Sinyal Hilang (Lost)** dengan banner penjelasan.
6. **Riwayat** → icon 🕘 di kanan atas layar utama; data tersimpan di Room dan bertahan
   setelah aplikasi ditutup. Ada tombol hapus dengan konfirmasi.
7. **Rotasi layar** → pemindaian tetap berjalan.
8. **Ke latar belakang** → pemindaian **tetap berjalan** sampai Anda menekan *Stop*
   (sesuai brief). Catatan: di Android 8+, sistem boleh memperlambat hasil scan saat
   layar mati — lihat bagian *Known Issues*.
9. **Daftar tidak pernah kosong** → perangkat yang berhenti mengiklankan tetap tampil
   redup ("Terakhir terlihat"); saat sinyalnya muncul lagi, baris otomatis nyala.
10. **Tolak izin / matikan Bluetooth** → banner peringatan muncul dengan tombol aksi.

---

## 4. Arsitektur

Pola **MVVM** dengan lapisan yang jelas (*separation of concerns*):

```
┌────────────────────────────────────────────────────────────┐
│ UI (Jetpack Compose)                                       │
│   HomeScreen / DetailScreen / HistoryScreen                │
│   + komponen: BotBar, FilterBar, DeviceRow, RadarView, ... │
└───────────────▲────────────────────────────────────────────┘
                │ StateFlow / SharedFlow  (one-way data flow)
┌───────────────┴────────────────────────────────────────────┐
│ ViewModel (Hilt)                                           │
│   BluetoothFinderViewModel · RadarViewModel ·              │
│   HistoryViewModel  — filter, event error, lifecycle       │
└───────────────▲────────────────────────────────────────────┘
                │ interface DeviceRepository
┌───────────────┴────────────────────────────────────────────┐
│ Repository                                                  │
│   DefaultDeviceRepository — sort, throttle, gabung data    │
└───────▲───────────────────────────────▲────────────────────┘
        │                               │
┌───────┴───────────┐         ┌─────────┴──────────┐
│ data/ble          │         │ data/local (Room)  │
│ AndroidBleScanner │         │ DeviceDao · DB     │
└───────────────────┘         └────────────────────┘
        domain/ : BleDevice · SignalCategory (pure Kotlin, mudah diuji)
        di/     : binding Hilt (AppModule · BindingsModule · ApplicationScope)
```

**Alasan pemilihan teknologi**

| Teknologi | Alasan |
|-----------|--------|
| **Kotlin** | Bahasa resmi Android, coroutines/nullable-safety, sesuai pilihan bebas pada brief. |
| **Jetpack Compose** | Declarative UI, state-driven → pas untuk daftar & radar yang berubah real-time; mengurangi boilerplate XML. |
| **MVVM + StateFlow** | Pola teruji & diminta brief; data flow satu arah, mudah diuji & di-preview. |
| **Hilt** | DI resmi dari Android/Dagger → dependensi (scanner, DB, repository) singleton & ter-generate secara compile-time. |
| **Room** | Persistence lokal resmi Android (brief: "seperti Room untuk Android"), dengan `Flow` untuk update riwayat secara reaktif. |
| **Navigation Compose** | Navigasi tipis-aman antar 3 layar + *nav argument* untuk target pelacakan. |
| **Coroutines / Flow** | Stream hasil scan + penyimpanan ber-*throttle* tanpa memblokir main thread. |
| **Material 3** | Komponen aksesibel & konsisten (TopAppBar, FilterChip, Card, AlertDialog). |

**Kenapa mesin BLE & repository di-scope `@Singleton`?**
State pemindaian harus selamat dari rotasi layar (Activity di-recreate, ViewModel tetap,
dan scanner tetap hidup), sehingga hasil scan tidak "kosong" sesaat setelah rotasi.

---

## 5. Struktur Package

```
com.example.bluetoothfinder
├── App.kt, MainActivity.kt
├── domain/            BleDevice, SignalCategory (tabel RSSI → jarak)
├── data/
│   ├── ble/           BleScanner (interface), AndroidBleScanner (impl)
│   ├── local/         AppDatabase, DeviceDao, DeviceHistoryEntity
│   └── repository/    DeviceRepository, DefaultDeviceRepository
├── di/                AppModule, BindingsModule, ApplicationScope
├── util/              BluetoothPermissions
├── viewmodel/         BluetoothFinderViewModel, RadarViewModel, HistoryViewModel
├── model/             BluetoothModel (presentation model)
└── ui/
    ├── navigation/    AppNavigation
    ├── screen/        HomeScreen, DetailScreen, HistoryScreen
    ├── screen/component/  BotBar, FilterBar, DeviceRow, RadarView, StatusBanner, TableCell, HeaderData
    └── theme/         Theme, Color, SignalColors
```

---

## 6. Known Issues / Keterbatasan

1. **Estimasi jarak bersifat heuristik.** Nilai meter diambil dari tabel RSSI→jarak pada
   brief (bukan hasil kalibrasi *path-loss* per ruangan), sehingga angka pasti akan
   berbeda antar perangkat/lingkungan. Nilai `Tx Power` tetap ditampilkan bila tersedia.
2. **Membutuhkan perangkat fisik.** Emulator umumnya tidak memiliki Bluetooth adapter,
   jadi pemindaian hanya bisa diuji di HP sungguhan.
3. **MAC address bisa "palsu" (`02:00:00:00:00:00`) untuk beberapa perangkat**
   (kebijakan privasi Android 10+ untuk iklan). Aplikasi memberi fallback ID stabil agar
   daftar & pelacakan tetap berfungsi.
4. **Nama perangkat** hanya muncul jika perangkat mengiklankannya; jika tidak, ditampilkan
   `Tanpa Nama` + opsi **"Baca nama"** yang membaca nama **langsung dari perangkat** via
   koneksi GATT (characteristic Device Name `0x2A00`) — hasilnya persis nama di
   Pengaturan -> Bluetooth perangkat (berlaku untuk HP yang tidak broadcast namanya).
5. **Penghentian scan sepenuhnya di tangan pengguna** (tombol *Stop*) — sesuai brief yang
   tidak meminta auto-stop. Tidak ada penjeda otomatis saat latar belakang. Konsekuensinya:
   baterai lebih rakus bila ditinggal scan, dan **Android 8+ boleh memperlambat hasil scan
   saat layar mati** (kebijakan sistem; tidak bisa dipaksa tanpa foreground service).
   Untuk HP dengan layar selalu menyala + aplikasi di depan, ini tidak berpengaruh.
6. **Daftar perangkat di-retensi selama sesi** — perangkat yang berhenti mengiklankan tidak
   dihapus, hanya ditandai "hilang" (dirender redup + "Terakhir terlihat"), dan kembali
   aktif otomatis saat paket masuk. Baris hanya dibuang bila tidak terlihat > 30 menit
   (batas memori).
7. **Belum ada fitur koneksi/pairing/GATT** — sesuai brief yang hanya meminta pelacakan
   berbasis *advertising* RSSI.
8. **Android 14+ membatasi hardware scan slot**; pada pemakaian sangat agresif sistem dapat
   memblock scan (error code 2/5 ditangani dan ditampilkan sebagai snackbar).
9. **Belum ada instrumented test UI/E2E** (unit test logika kategori RSSI & filter/pengurutan)
   karena keterbatasan waktu 48 jam dan ketergantungan pada perangkat fisik.

---

## 7. Asumsi Teknis & Kendala Selama Pengerjaan

- **Toolchain mismatch (kendala utama).** Project awal memakai KSP `2.0.21-1.0.25` +
  Kotlin `2.0.21`, sedangkan AGP 9.0.1 mengaktifkan **built-in Kotlin 2.2.10**. Akibatnya
  KSP gagal (`unexpected jvm signature V`). Solusinya: KSP diselaraskan ke
  `2.2.10-2.0.2`, plugin Kotlin/Compose ke `2.2.10`, dan **Room dinaikkan ke 2.7.1**
  (Room 2.6.1 belum kompatibel dengan KSP2).
- **compileSdk diturunkan dari 37.0 ke 36 (stable).** SDK 37.0 hanya dikenali oleh
  build CLI/AGP, sedangkan Android Studio tidak menemukan target `android-37`
  (platform terpasang bernama `android-37.0`) sehingga muncul notifikasi
  *"Could not find compile target android-37"*. Dengan compileSdk 36, project terbuka
  bersih di Android Studio mana pun (36 didukung resmi AGP 9) dan tidak ada kode
  yang memakai API di atas 36.
- **Asumsi: scan sepenuhnya dikendalikan pengguna** (Start/Stop) tanpa jeda otomatis di
  background — mengikuti brief; lihat *Known Issues* #5 untuk konsekuensi baterai & OS throttle.
- **Asumsi: daftar hasil di-retensi selama sesi** — perangkat hilang sinyal tidak dihapus,
  hanya ditandai redup ("Terakhir terlihat"); pemangkasan hanya untuk entri > 30 menit
  sebagai batas memori.
- **Asumsi: pengurutan memakai RSSI yang sudah dihaluskan (EMA α=0.35)** agar urutan tidak
  "loncat-loncat"; nilai **RSSI mentah tetap ditampilkan** sesuai brief, dan filter ambang
  juga memakai nilai mentah (angka di layar = angka yang difilter); warna indikator memakai EMA.
- **Asumsi: tampilan live scan di-throttle 1 detik per flush** (`UI_REFRESH_INTERVAL_MS`).
  Paket advertising pada `SCAN_MODE_LOW_LATENCY` bisa datang ~10×/detik; semua hasil ditampung
  buffer in-memory lalu di-flush berkala sehingga daftar tidak bergetar terlalu cepat
  (`lastSeen` tetap memakai waktu paket asli agar deteksi *Sinyal Hilang* akurat).
- **Asumsi: penyimpanan riwayat di-throttle 2 detik per perangkat** agar database tidak
  dibanjiri (nilai final selalu di-flush saat scan berhenti).
- **Asumsi: logging data perangkat (Timber) hanya untuk build debug.** Untuk memudahkan
  pengujian, data perangkat dicetak ke Logcat (tag `DeviceData`): (1) seluruh daftar
  *array of object* saat data diterima repository, dan (2) *object* lengkap tiap baris
  saat dirender di LazyColumn. Ditanam hanya di `App.onCreate` untuk `BuildConfig.DEBUG`,
  sehingga di build release tidak ada tree yang ditanam dan panggilan logging jadi no-op.
- **Dilarangnya location untuk `neverForLocation`** — `BLUETOOTH_SCAN` dideklarasikan
  dengan `android:usesPermissionFlags="neverForLocation"` sehingga tidak perlu izin lokasi
  di Android 12+ (berkurangnya permintaan izin ke pengguna).

---

## 8. Pengumpulan (Submission Checklist)

- [x] **Source code** + commit history yang jelas
- [x] **README.md** (setup, arsitektur, library, known issues, asumsi/kendala)
- [x] **Build artifact**: `artifacts/BluetoothFinder-v1.0-debug.apk`

---

## 9. Unit Test

```bash
./gradlew :app:testDebugUnitTest
```
- `SignalCategoryTest` — memastikan seluruh batas RSSI (± 1 kelas) terpetakan tepat ke
  kategori & label jarak sesuai tabel brief, serta skala radar monoton.
- `DeviceFilterTest` — pengurutan dari sinyal terkuat (EMA), filter nama/MAC
  (case-insensitive), dan filter ambang sinyal yang memakai nilai RSSI mentah.
- `RadarViewModelTest` — alur pembacaan nama via GATT (otomatis saat target tanpa nama,
  status loading/gagal, dan percobaan ulang).

---

## 10. Pernyataan Orisinalitas & Penggunaan Alat Bantu (AI)

Saya menyatakan bahwa proyek ini dikerjakan secara mandiri sebagai bagian dari study case
yang diberikan. Selama pengerjaan saya **menggunakan bantuan alat generatif AI** (asisten
kode berbasis LLM yang dijalankan lewat agen CLI **OpenCode**, model **MiMo-V2.6**), dengan
rincian sebagai berikut.

### Bagian yang dibantu AI

- Penulisan dan penyusunan kode sumber (Kotlin, Jetpack Compose, Hilt, Room, Coroutines):
  arsitektur MVVM, scanner BLE, persistence, hingga ketiga layar.
- Penulisan unit test (`SignalCategoryTest`, `DeviceFilterTest`, `RadarViewModelTest`).
- Penulisan dokumentasi README ini.
- Diagnosa & perbaikan masalah build (compatibilitas KSP / AGP 9 built-in Kotlin / Room 2.7.1)
  serta penurunan compileSdk ke 36 agar kompatibel dengan Android Studio.
- Saran praktik terbaik lifecycle, error handling, dan penskalaan data (throttle flush,
  EMA smoothing, retensi daftar).

### Peran saya (manusia)

- Menentukan kebutuhan dan mengambil seluruh keputusan desain: memilih Kotlin + Jetpack
  Compose, menolak mode *hybrid scan* klasik-BLE, memilih retensi daftar, dsb.
- Menguji langsung di perangkat fisik (POCO X3 NFC, Infinix NOTE 12) dan melaporkan
  hasil pengujian sebagai bahan iterasi.
- Review, penyesuaian, dan verifikasi setiap perubahan kode sebelum commit.
- Mengelola repository, commit history, dan artefak submission (APK).
