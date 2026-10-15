package com.example.bluetoothfinder.domain

/**
 * Nilai ambang RSSI yang berarti "tanpa filter" (Semua sinyal).
 * Lebih kecil dari nilai RSSI BLE mana pun (-127 dBm).
 */
const val NO_RSSI_THRESHOLD = -200

/**
 * Urutkan daftar dari sinyal terkuat (**RSSI tertinggi**) sesuai brief.
 *
 * Pemakaian nilai **EMA (RSSI halus)** agar urutan baris tidak saling serobot setiap
 * paket advertising masuk. Angka yang ditampilkan di layar tetap RSSI mentah, dan
 * filter ambang tetap memakai RSSI mentah (lihat [filteredBy]).
 */
fun List<BleDevice>.sortedBySignal(): List<BleDevice> =
    sortedWith(
        compareByDescending<BleDevice> { it.smoothedRssi }
            .thenByDescending { it.rssi }
            .thenBy { it.name.lowercase() }
    )

/**
 * Filter daftar berdasarkan:
 * - kata kunci pencarian pada **nama perangkat** atau **MAC address/UUID** (case-insensitive), dan
 * - **ambang batas minimum sinyal** (mis. hanya tampilkan perangkat dengan RSSI ≥ -80 dBm).
 *
 * @param minRssi nilai [NO_RSSI_THRESHOLD] berarti tanpa ambang sinyal.
 */
fun List<BleDevice>.filteredBy(query: String, minRssi: Int): List<BleDevice> {
    val keyword = query.trim()
    return filter { device ->
        val matchesQuery = keyword.isEmpty() ||
            device.name.contains(keyword, ignoreCase = true) ||
            device.address.contains(keyword, ignoreCase = true)
        val matchesThreshold = minRssi == NO_RSSI_THRESHOLD || device.rssi >= minRssi
        matchesQuery && matchesThreshold
    }
}
