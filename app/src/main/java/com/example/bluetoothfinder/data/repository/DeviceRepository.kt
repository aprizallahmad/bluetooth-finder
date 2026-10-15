package com.example.bluetoothfinder.data.repository

import com.example.bluetoothfinder.data.ble.ScanStatus
import com.example.bluetoothfinder.data.local.DeviceHistoryEntity
import com.example.bluetoothfinder.domain.BleDevice
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Satu-satunya sumber data bagi layer UI:
 * hasil scan real-time (BLE) + riwayat perangkat (Room).
 */
interface DeviceRepository {

    /** Hasil scan real-time, sudah diurutkan dari sinyal terkuat (RSSI tertinggi). */
    val devices: StateFlow<List<BleDevice>>

    val scanStatus: StateFlow<ScanStatus>

    val bluetoothOn: StateFlow<Boolean>

    /** Edge-case/error dari layer BLE (izin ditolak, bluetooth mati, scan gagal, dst). */
    val errors: SharedFlow<String>

    /** Riwayat perangkat dari database lokal, terbaru di atas. */
    val history: Flow<List<DeviceHistoryEntity>>

    fun observeDevice(address: String): Flow<DeviceHistoryEntity?>

    fun startScan()

    fun stopScan()

    fun refreshBluetoothState()

    suspend fun clearHistory()
}
