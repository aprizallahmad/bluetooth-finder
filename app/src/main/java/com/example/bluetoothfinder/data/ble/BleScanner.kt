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
     * Membaca nama perangkat **langsung dari perangkat target** lewat koneksi GATT
     * (characteristic Device Name `0x2A00` pada service Generic Access `0x1800`).
     *
     * Nama hasil pembacaan ini persis sama dengan nama di Pengaturan -> Bluetooth
     * perangkat tersebut, sehingga tetap bisa diketahui meski perangkat tidak
     * mengiklankan namanya dalam paket iklan (kasus umum pada HP Android/iOS).
     *
     * @param onResult dipanggil di main thread; [Result.failure] bila perangkat
     *   tidak connectable, putus, atau tidak menyediakan karakteristik nama.
     */
    fun readDeviceName(address: String, onResult: (Result<String>) -> Unit)

    /**
     * Memeriksa ulang status adapter Bluetooth.
     * Dipanggil berkala oleh ViewModel selama aplikasi aktif agar perubahan
     * (pengguna mematikan Bluetooth) tertangkap tanpa broadcast receiver.
     */
    fun refreshBluetoothState()

    fun isReady(): Boolean
}
