package com.example.bluetoothfinder.domain

/**
 * Model domain sebuah perangkat BLE yang sedang/telah terdeteksi scanner.
 *
 * @property address MAC address (Android) / UUID (iOS) perangkat.
 * @property name nama perangkat; "Tanpa Nama" bila advertising tidak membawa nama.
 * @property rssi nilai RSSI mentah (raw dBm) dari paket advertising terakhir.
 * @property smoothedRssi RSSI yang sudah dihaluskan (EMA) agar indikator tidak berkedip.
 * @property txPower level daya transmit dari ScanRecord (nullable, bila tersedia).
 * @property lastSeen timestamp epoch ms saat paket terakhir diterima.
 * @property seenCount berapa kali perangkat terlihat selama sesi scan berjalan.
 */
data class BleDevice(
    val address: String,
    val name: String,
    val rssi: Int,
    val smoothedRssi: Int,
    val txPower: Int?,
    val lastSeen: Long,
    val seenCount: Int,
) {
    val category: SignalCategory get() = SignalCategory.fromRssi(smoothedRssi)

    /** Perkiraan jarak berdasarkan tabel pemetaan RSSI -> meter. */
    val distanceMeters: Double get() = category.approxMeters

    /** Rentang jarak (string) sesuai brief. */
    val distanceLabel: String get() = category.distanceLabel

    companion object {
        const val UNKNOWN_NAME = "Tanpa Nama"
    }
}
