package com.example.bluetoothfinder.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bluetoothfinder.data.ble.ScanStatus
import com.example.bluetoothfinder.data.repository.DeviceRepository
import com.example.bluetoothfinder.domain.BleDevice
import com.example.bluetoothfinder.model.BluetoothModel
import com.example.bluetoothfinder.util.BluetoothPermissions
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel untuk Screen 1 (Dashboard / Scanner).
 *
 * Menangani:
 * - kontrol Start / Stop pemindaian,
 * - filter pencarian (nama / MAC) & ambang batas minimum RSSI,
 * - pengurutan otomatis RSSI tertinggi (dilakukan repository),
 * - error handling izin, Bluetooth mati, dan kegagalan scan,
 * - lifecycle aplikasi: scan dijeda saat ke latar belakang & dilanjutkan saat kembali.
 */
@HiltViewModel
class BluetoothFinderViewModel @Inject constructor(
    private val repository: DeviceRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    val scanStatus: StateFlow<ScanStatus> = repository.scanStatus
    val bluetoothOn: StateFlow<Boolean> = repository.bluetoothOn

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _minRssi = MutableStateFlow(NO_THRESHOLD)
    val minRssi: StateFlow<Int> = _minRssi.asStateFlow()

    private val _hasPermissions = MutableStateFlow(BluetoothPermissions.hasAll(context))
    val hasPermissions: StateFlow<Boolean> = _hasPermissions.asStateFlow()

    private val _events = MutableSharedFlow<String>(
        extraBufferCapacity = 8,
        onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST,
    )
    val events: SharedFlow<String> = _events.asSharedFlow()

    /** Daftar perangkat hasil scan yang sudah difilter + diurutkan RSSI tertinggi. */
    val devices: StateFlow<List<BluetoothModel>> =
        combine(repository.devices, _query, _minRssi) { list, query, minRssi ->
            list.asSequence()
                .filter { it.matches(query) }
                .filter { it.smoothedRssi >= minRssi }
                .map(BleDevice::toUiModel)
                .toList()
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    val isScanning: Boolean get() = scanStatus.value == ScanStatus.SCANNING

    /** True bila pengguna menginginkan scan aktif (dipakai untuk resume otomatis). */
    private var userWantsScan = false

    /** True bila scan dijeda otomatis karena aplikasi masuk latar belakang. */
    private var pausedByBackground = false

    private var pollJob: Job? = null
    private var pauseJob: Job? = null

    init {
        viewModelScope.launch {
            repository.errors.collect { _events.tryEmit(it) }
        }
    }

    // ------------------------------------------------------------------ control

    /**
     * Dipanggil UI setelah tombol Start ditekan dan izin sudah diberikan.
     * Jika izin belum diberikan, UI harus meminta izin lebih dulu.
     */
    fun startScan() {
        if (!BluetoothPermissions.hasAll(context)) {
            _hasPermissions.value = false
            _events.tryEmit("Izin Bluetooth belum diberikan. Izinkan akses agar bisa memindai.")
            return
        }
        userWantsScan = true
        pausedByBackground = false
        repository.refreshBluetoothState()
        repository.startScan()
    }

    fun stopScan() {
        userWantsScan = false
        pausedByBackground = false
        repository.stopScan()
    }

    fun onPermissionsResult(granted: Boolean) {
        _hasPermissions.value = granted
        repository.refreshBluetoothState()
        if (granted && userWantsScan) repository.startScan()
        if (!granted) _events.tryEmit("Izin Bluetooth ditolak. Beberapa fitur tidak dapat berjalan.")
    }

    fun onBluetoothEnabled() {
        repository.refreshBluetoothState()
        if (userWantsScan) repository.startScan()
    }

    fun refreshPermissions() {
        _hasPermissions.value = BluetoothPermissions.hasAll(context)
        repository.refreshBluetoothState()
    }

    // ------------------------------------------------------------------ filters

    fun onQueryChange(value: String) {
        _query.value = value
    }

    fun onMinRssiChange(value: Int) {
        _minRssi.value = value
    }

    fun clearFilters() {
        _query.value = ""
        _minRssi.value = NO_THRESHOLD
    }

    // ------------------------------------------------------------------ lifecycle

    /** Dipanggil saat layar masuk ke STARTED (termasuk setelah rotasi). */
    fun onUiStarted() {
        pauseJob?.cancel()
        pauseJob = null
        refreshPermissions()
        startPolling()
        if (pausedByBackground && userWantsScan) {
            pausedByBackground = false
            repository.startScan()
        }
    }

    /** Dipanggil saat layar keluar dari STARTED (latar belakang / rotasi). */
    fun onUiStopped() {
        stopPolling()
        // Beri waktu beberapa detik: bila ini hanya rotasi layar, onUiStarted()
        // akan membatalkan penjadwalan sehingga scan tidak pernah berhenti.
        pauseJob?.cancel()
        pauseJob = viewModelScope.launch {
            delay(BACKGROUND_PAUSE_DELAY_MS)
            if (isScanning) {
                pausedByBackground = true
                repository.stopScan()
            }
        }
    }

    private fun startPolling() {
        if (pollJob?.isActive == true) return
        pollJob = viewModelScope.launch {
            while (isActive) {
                delay(BLUETOOTH_POLL_INTERVAL_MS)
                repository.refreshBluetoothState()
            }
        }
    }

    private fun stopPolling() {
        pollJob?.cancel()
        pollJob = null
    }

    override fun onCleared() {
        stopPolling()
        super.onCleared()
    }

    // ------------------------------------------------------------------ helpers

    private fun BleDevice.matches(query: String): Boolean {
        if (query.isBlank()) return true
        return name.contains(query, ignoreCase = true) ||
            address.contains(query, ignoreCase = true)
    }

    companion object {
        /** Ambang RSSI "tanpa filter". */
        const val NO_THRESHOLD = -200

        private const val BLUETOOTH_POLL_INTERVAL_MS = 1_500L
        private const val BACKGROUND_PAUSE_DELAY_MS = 3_000L
    }
}

fun BleDevice.toUiModel(): BluetoothModel = BluetoothModel(
    id = address.hashCode(),
    name = name,
    macAddress = address,
    distance = distanceLabel,
    rssi = "$rssi dBm",
    signalCategory = category.label,
    rssiValue = smoothedRssi,
    distanceMeters = distanceMeters,
    lastSeen = lastSeen,
)
