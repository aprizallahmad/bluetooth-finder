package com.example.bluetoothfinder.data.ble

import com.example.bluetoothfinder.domain.BleDevice
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/** Status mesin pemindaian BLE. */
enum class ScanStatus {
    /** Pemindaian tidak berjalan. */
    IDLE,

    /** Pemindaian berjalan. */
    SCANNING,

    /** Bluetooth dimatikan oleh sistem/pengguna. */
    BLUETOOTH_OFF,

    /** Perangkat tidak mendukung BLE. */
    UNSUPPORTED,
}

/**
 * Kontrak mesin pemindaian BLE.
 *
 * Implementasi (lihat [AndroidBleScanner]) adalah singleton sehingga state pemindaian
 * tetap hidup saat terjadi rotasi layar / perubahan konfigurasi.
 */
interface BleScanner {

    /** Perangkat terdeteksi, di-key berdasarkan alamat unik perangkat. */
    val devices: StateFlow<Map<String, BleDevice>>

    val status: StateFlow<ScanStatus>

    val bluetoothOn: StateFlow<Boolean>

    /** Event error/edge-case (izin ditolak, bluetooth mati, scan gagal, dst). */
    val errors: SharedFlow<String>

    fun startScan()

    fun stopScan()

    /**
     * Memeriksa ulang status adapter Bluetooth.
     * Dipanggil berkala oleh ViewModel selama aplikasi aktif agar perubahan
     * (pengguna mematikan Bluetooth) tertangkap tanpa broadcast receiver.
     */
    fun refreshBluetoothState()

    fun isReady(): Boolean
}
