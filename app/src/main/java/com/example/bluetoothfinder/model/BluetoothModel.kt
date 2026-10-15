package com.example.bluetoothfinder.model

/**
 * Model presentation untuk daftar perangkat pada layar scanner.
 *
 * @property rssi nilai RSSI mentah (raw dBm) dalam format tampilan, contoh: "-62 dBm".
 *   Sesuai brief, kolom daftar menampilkan **nilai mentah**.
 * @property rssiValue RSSI mentah (numerik) — dipakai filter ambang sinyal agar
 *   angka yang dilihat pengguna = angka yang difilter.
 * @property indicatorRssi RSSI hasil EMA (halus) — dipakai untuk **warna indikator**
 *   agar warna/kategori tidak berkedip tiap paket.
 * @property isStale true bila perangkat **hilang sinyal** (tidak terlihat ≥ 15 dtk).
 *   Perangkat hilang tetap ditampilkan (retensi sesi) tapi dirender redup dengan
 *   keterangan "terakhir terlihat"; otomatis nyala lagi saat paket masuk.
 */
data class BluetoothModel(
    val id: Int,
    val name: String,
    val macAddress: String,
    val distance: String,
    val rssi: String,
    val signalCategory: String = "",
    val rssiValue: Int = 0,
    val indicatorRssi: Int = 0,
    val smoothedRssiValue: Int = 0,
    val distanceMeters: Double = 0.0,
    val lastSeen: Long = 0L,
    val isStale: Boolean = false,
)
