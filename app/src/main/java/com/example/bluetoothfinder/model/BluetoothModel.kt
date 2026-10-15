package com.example.bluetoothfinder.model

/**
 * Model presentation untuk daftar perangkat pada layar scanner.
 *
 * @property rssi nilai RSSI mentah (raw dBm) dalam format tampilan, contoh: "-62 dBm".
 * @property rssiValue nilai RSSI numerik (sudah dihaluskan) untuk kebutuhan filter.
 */
data class BluetoothModel(
    val id: Int,
    val name: String,
    val macAddress: String,
    val distance: String,
    val rssi: String,
    val signalCategory: String = "",
    val rssiValue: Int = 0,
    val distanceMeters: Double = 0.0,
    val lastSeen: Long = 0L,
)
