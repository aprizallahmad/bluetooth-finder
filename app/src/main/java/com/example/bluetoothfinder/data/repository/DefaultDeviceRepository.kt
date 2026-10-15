package com.example.bluetoothfinder.data.repository

import com.example.bluetoothfinder.data.ble.BleScanner
import com.example.bluetoothfinder.data.ble.ScanStatus
import com.example.bluetoothfinder.data.local.DeviceDao
import com.example.bluetoothfinder.data.local.DeviceHistoryEntity
import com.example.bluetoothfinder.di.ApplicationScope
import com.example.bluetoothfinder.domain.BleDevice
import com.example.bluetoothfinder.domain.sortedBySignal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Menggabungkan [BleScanner] (real-time) dengan [DeviceDao] (Room).
 *
 * - Menyediakan daftar perangkat yang sudah diurutkan otomatis RSSI tertinggi.
 * - Menulis riwayat ke Room dengan throttle [PERSIST_INTERVAL_MS] agar database
 *   tidak dibanjiri puluhan tulisan per detik saat sinyal berubah cepat.
 *
 * Kontrol scan sepenuhnya di tangan pengguna (tombol Start/Stop sesuai brief) —
 * tidak ada penjeda/penghentian otomatis berdasarkan lifecycle aplikasi.
 */
@Singleton
class DefaultDeviceRepository @Inject constructor(
    private val bleScanner: BleScanner,
    private val deviceDao: DeviceDao,
    @ApplicationScope private val scope: CoroutineScope,
) : DeviceRepository {

    private val _devices = MutableStateFlow<List<BleDevice>>(emptyList())
    override val devices: StateFlow<List<BleDevice>> = _devices.asStateFlow()

    override val scanStatus: StateFlow<ScanStatus> = bleScanner.status
    override val bluetoothOn: StateFlow<Boolean> = bleScanner.bluetoothOn
    override val errors: SharedFlow<String> = bleScanner.errors

    override val history: Flow<List<DeviceHistoryEntity>> = deviceDao.observeHistory()

    private val lastPersistedAt = mutableMapOf<String, Long>()

    init {
        scope.launch {
            bleScanner.devices.collect { snapshot ->
                _devices.value = snapshot.values.toList().sortedBySignal()
                persist(snapshot.values, force = false)
            }
        }

        scope.launch {
            bleScanner.status.collect { status ->
                // Scan berhenti -> tulis nilai final yang belum tersimpan (force flush).
                if (status == ScanStatus.IDLE) {
                    persist(bleScanner.devices.value.values, force = true)
                }
            }
        }
    }

    override fun observeDevice(address: String): Flow<DeviceHistoryEntity?> =
        deviceDao.observeByAddress(address)

    override fun startScan() = bleScanner.startScan()

    override fun stopScan() = bleScanner.stopScan()

    override fun readDeviceName(address: String, onResult: (Result<String>) -> Unit) {
        bleScanner.readDeviceName(address) { result ->
            // Simpan nama hasil baca GATT ke riwayat lokal agar tampil juga di History.
            result.onSuccess { name ->
                scope.launch { runCatching { deviceDao.updateName(address, name) } }
            }
            onResult(result)
        }
    }

    override fun refreshBluetoothState() = bleScanner.refreshBluetoothState()

    override suspend fun clearHistory() {
        lastPersistedAt.clear()
        deviceDao.clearHistory()
    }

    private suspend fun persist(devices: Collection<BleDevice>, force: Boolean) {
        val now = System.currentTimeMillis()
        devices.forEach { device ->
            val last = lastPersistedAt[device.address]
            if (!force && last != null && now - last < PERSIST_INTERVAL_MS) return@forEach
            lastPersistedAt[device.address] = now

            val existing = deviceDao.getByAddress(device.address)
            deviceDao.upsert(device.toEntity(existing))
        }
    }

    private fun BleDevice.toEntity(existing: DeviceHistoryEntity?): DeviceHistoryEntity =
        if (existing == null) {
            DeviceHistoryEntity(
                address = address,
                name = name,
                firstSeenAt = lastSeen,
                lastSeenAt = lastSeen,
                lastRssi = rssi,
                bestRssi = rssi,
                seenCount = 1,
            )
        } else {
            existing.copy(
                name = name.ifBlank { existing.name },
                lastSeenAt = maxOf(existing.lastSeenAt, lastSeen),
                lastRssi = rssi,
                bestRssi = maxOf(existing.bestRssi, rssi),
                seenCount = existing.seenCount + 1,
                // Jaga-jaga bila jam sistem berubah mundur.
                firstSeenAt = minOf(existing.firstSeenAt, lastSeen),
            )
        }

    private companion object {
        /** Minimal jeda tulisan per perangkat ke database. */
        const val PERSIST_INTERVAL_MS = 2_000L
    }
}
