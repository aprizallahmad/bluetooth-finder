package com.example.bluetoothfinder.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entitas riwayat perangkat BLE yang pernah terdeteksi.
 * Disimpan di database lokal (Room) agar tidak hilang saat aplikasi ditutup.
 */
@Entity(tableName = "device_history")
data class DeviceHistoryEntity(
    @PrimaryKey val address: String,
    val name: String,
    val firstSeenAt: Long,
    val lastSeenAt: Long,
    val lastRssi: Int,
    val bestRssi: Int,
    val seenCount: Int,
)
