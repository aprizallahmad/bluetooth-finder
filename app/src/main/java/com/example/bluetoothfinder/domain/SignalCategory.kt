package com.example.bluetoothfinder.domain

/**
 * Kategori kekuatan sinyal BLE sesuai tabel pemetaan pada brief:
 *
 * | RSSI (dBm)     | Kategori Sinyal                          | Perkiraan Jarak          |
 * |----------------|------------------------------------------|---------------------------|
 * | -10 s/d -30    | Sangat Kuat (Sangat Dekat)               | < 1 meter                 |
 * | -30 s/d -50    | Kuat (Dekat)                             | 1 - 3 meter               |
 * | -50 s/d -70    | Cukup / Baik                             | 3 - 10 meter              |
 * | -70 s/d -80    | Lemah                                    | 10 - 20 meter             |
 * | -80 s/d -90    | Sangat Lemah / Putus-putus               | > 20 meter (batas jangkauan) |
 * | < -90          | Sinyal Hilang (Lost)                     | Terputus / di luar jangkauan |
 *
 * @param label nama kategori yang ditampilkan ke pengguna.
 * @param distanceLabel rentang jarak sesuai brief.
 * @param approxMeters nilai jarak representatif (meter) dipakai untuk skala radar & urutan.
 * @param minRssi batas bawah rentang (inklusif).
 * @param maxRssi batas atas rentang (inklusif).
 */
enum class SignalCategory(
    val label: String,
    val distanceLabel: String,
    val approxMeters: Double,
    val minRssi: Int,
    val maxRssi: Int,
) {
    VERY_STRONG(
        label = "Sangat Kuat (Sangat Dekat)",
        distanceLabel = "< 1 meter",
        approxMeters = 0.7,
        minRssi = -30,
        maxRssi = -10,
    ),
    STRONG(
        label = "Kuat (Dekat)",
        distanceLabel = "1 - 3 meter",
        approxMeters = 2.0,
        minRssi = -50,
        maxRssi = -31,
    ),
    GOOD(
        label = "Cukup / Baik",
        distanceLabel = "3 - 10 meter",
        approxMeters = 6.0,
        minRssi = -70,
        maxRssi = -51,
    ),
    WEAK(
        label = "Lemah",
        distanceLabel = "10 - 20 meter",
        approxMeters = 15.0,
        minRssi = -80,
        maxRssi = -71,
    ),
    VERY_WEAK(
        label = "Sangat Lemah / Putus-putus",
        distanceLabel = "> 20 meter (batas jangkauan)",
        approxMeters = 25.0,
        minRssi = -90,
        maxRssi = -81,
    ),
    LOST(
        label = "Sinyal Hilang (Lost)",
        distanceLabel = "Terputus / di luar jangkauan",
        approxMeters = 40.0,
        minRssi = Int.MIN_VALUE,
        maxRssi = -91,
    );

    /** Seberapa "dekat" posisi target pada radar, 0f = pusat, 1f = tepi luar. */
    val radarFraction: Float
        get() = when (this) {
            VERY_STRONG -> 0.18f
            STRONG -> 0.34f
            GOOD -> 0.52f
            WEAK -> 0.68f
            VERY_WEAK -> 0.84f
            LOST -> 0.95f
        }

    companion object {
        /**
         * Memetakan nilai RSSI mentah (dBm) ke kategori sinyal.
         * Nilai yang lebih besar dari -10 tetap dianggap "Sangat Kuat".
         */
        fun fromRssi(rssi: Int): SignalCategory = when {
            rssi >= VERY_STRONG.minRssi -> VERY_STRONG
            rssi >= STRONG.minRssi -> STRONG
            rssi >= GOOD.minRssi -> GOOD
            rssi >= WEAK.minRssi -> WEAK
            rssi >= VERY_WEAK.minRssi -> VERY_WEAK
            else -> LOST
        }
    }
}
