package com.example.bluetoothfinder.domain

/**
 * Nilai ambang RSSI yang berarti "tanpa filter" (Semua sinyal).
 * Lebih kecil dari nilai RSSI BLE mana pun (-127 dBm).
 */
const val NO_RSSI_THRESHOLD = -200

/**
 * Urutkan daftar dari sinyal terkuat (**RSSI tertinggi**) sesuai brief:
 * - **Perangkat hidup selalu di atas** yang hilang/redup.
 * - Kelompok hidup diurut dari EMA (RSSI halus) terkuat agar tidak serobot.
 * - Kelompok hilang diurut dari yang **terakhir terlihat paling baru**.
 */
fun List<BleDevice>.sortedBySignal(now: Long = System.currentTimeMillis()): List<BleDevice> =
    sortedWith(
        compareBy<BleDevice> { it.isStaleAt(now) } // false (hidup) < true (hilang)
            .thenComparator { a, b ->
                if (a.isStaleAt(now)) {
                    b.lastSeen.compareTo(a.lastSeen) // hilang: terbaru terlihat dulu
                } else {
                    b.smoothedRssi.compareTo(a.smoothedRssi) // hidup: EMA terkuat dulu
                }
            }
            .thenBy { it.name.lowercase() } // stabilitas urutan
    )

/**
 * Filter daftar berdasarkan:
 * - kata kunci pencarian pada **nama perangkat** atau **MAC address/UUID** (case-insensitive),
 * - **ambang batas minimum sinyal** (mis. hanya tampilkan perangkat dengan RSSI ≥ -80 dBm), dan
 * - opsional: **sembunyikan perangkat yang hilang/redup** ([hideStale]).
 *
 * @param minRssi nilai [NO_RSSI_THRESHOLD] berarti tanpa ambang sinyal.
 */
fun List<BleDevice>.filteredBy(
    query: String,
    minRssi: Int,
    hideStale: Boolean = false,
    now: Long = System.currentTimeMillis(),
): List<BleDevice> {
    val keyword = query.trim()
    return filter { device ->
        val matchesQuery = keyword.isEmpty() ||
            device.name.contains(keyword, ignoreCase = true) ||
            device.address.contains(keyword, ignoreCase = true)
        val matchesThreshold = minRssi == NO_RSSI_THRESHOLD || device.rssi >= minRssi
        val matchesStale = !hideStale || !device.isStaleAt(now)
        matchesQuery && matchesThreshold && matchesStale
    }
}
