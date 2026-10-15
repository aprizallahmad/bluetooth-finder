package com.example.bluetoothfinder.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bluetoothfinder.data.ble.ScanStatus
import com.example.bluetoothfinder.data.repository.DeviceRepository
import com.example.bluetoothfinder.domain.BleDevice
import com.example.bluetoothfinder.domain.NO_RSSI_THRESHOLD
import com.example.bluetoothfinder.domain.filteredBy
import com.example.bluetoothfinder.model.BluetoothModel
import com.example.bluetoothfinder.util.BluetoothPermissions
import com.example.bluetoothfinder.util.DeviceLog
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel untuk Screen 1 (Dashboard / Scanner).
 *
 * Menangani:
 * - kontrol Start / Stop pemindaian (scan **hanya** berhenti lewat tombol Stop;
 *   tidak ada auto-pause di background — sesuai brief),
 * - filter pencarian (nama / MAC), ambang batas minimum RSSI, & opsi sembunyikan
 *   perangkat hilang,
 * - pengurutan otomatis RSSI tertinggi (perangkat hidup di atas yang hilang),
 * - error handling izin, Bluetooth mati, dan kegagalan scan,
 * - polling status Bluetooth selama layar terlihat.
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

    private val _hideStale = MutableStateFlow(false)
    val hideStale: StateFlow<Boolean> = _hideStale.asStateFlow()

    private val _hasPermissions = MutableStateFlow(BluetoothPermissions.hasAll(context))
    val hasPermissions: StateFlow<Boolean> = _hasPermissions.asStateFlow()

    private val _events = MutableSharedFlow<String>(
        extraBufferCapacity = 8,
        onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST,
    )
    val events: SharedFlow<String> = _events.asSharedFlow()

    /** Jumlah total perangkat hasil scan (sebelum difilter) — untuk indikator filter. */
    val totalDevices: StateFlow<Int> = repository.devices
        .map { it.size }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = 0,
        )

    /** Jumlah perangkat yang **masih hidup** (sinyal terlihat dalam 15 dtk terakhir). */
    val activeDevices: StateFlow<Int> = repository.devices
        .map { list -> list.count { !it.isStaleAt(System.currentTimeMillis()) } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = 0,
        )

    /**
     * Daftar perangkat hasil scan yang sudah difilter (nama/MAC + ambang RSSI +
     * sembunyikan hilang) dan tetap terurut dari RSSI tertinggi (hidup dulu).
     */
    val devices: StateFlow<List<BluetoothModel>> =
        combine(repository.devices, _query, _minRssi, _hideStale) { list, query, minRssi, hideStale ->
            list.filteredBy(query, minRssi, hideStale).map(BleDevice::toUiModel)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    val isScanning: Boolean get() = scanStatus.value == ScanStatus.SCANNING

    /** True bila pengguna menginginkan scan aktif (dipakai saat izin/bluetooth pulih). */
    private var userWantsScan = false

    private var pollJob: Job? = null

    init {
        viewModelScope.launch {
            repository.errors.collect { _events.tryEmit(it) }
        }

        // ---- Logging Timber (hanya build debug) untuk mengintip data perangkat ----
        // RAW  : seluruh daftar dari repository (array of object), sebelum difilter.
        // UI    : daftar yang benar-benar dirender di LazyColumn (sudah difilter + terurut).
        viewModelScope.launch {
            repository.devices.collect { list ->
                DeviceLog.logDevices("RAW", "dari repository (sebelum filter)", list.toUiModels())
            }
        }
        viewModelScope.launch {
            devices.collect { list ->
                DeviceLog.logDevices("UI", "dirender di LazyColumn (setelah filter)", list)
            }
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
        repository.refreshBluetoothState()
        repository.startScan()
    }

    fun stopScan() {
        userWantsScan = false
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

    fun onHideStaleChange(value: Boolean) {
        _hideStale.value = value
    }

    fun clearFilters() {
        _query.value = ""
        _minRssi.value = NO_THRESHOLD
        _hideStale.value = false
    }

    /** Deskripsi ringkas filter yang sedang aktif, mis. "≥ -80 dBm" atau "nama mengandung \"mi\"". */
    fun activeFilterDescription(): String {
        val parts = mutableListOf<String>()
        if (_query.value.isNotBlank()) parts += "pencarian \"${_query.value.trim()}\""
        if (_minRssi.value != NO_THRESHOLD) parts += "sinyal ≥ ${_minRssi.value} dBm"
        if (_hideStale.value) parts += "yang hilang disembunyikan"
        return parts.joinToString(" • ")
    }

    fun hasActiveFilter(): Boolean =
        _query.value.isNotBlank() || _minRssi.value != NO_THRESHOLD || _hideStale.value

    // ------------------------------------------------------------------ lifecycle

    /** Dipanggil saat layar masuk ke STARTED (termasuk setelah rotasi). */
    fun onUiStarted() {
        refreshPermissions()
        startPolling()
    }

    /** Dipanggil saat layar keluar dari STARTED (ke latar belakang / berpindah layar). */
    fun onUiStopped() {
        stopPolling()
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

    companion object {
        /** Ambang RSSI "tanpa filter" (Semua sinyal). */
        const val NO_THRESHOLD = NO_RSSI_THRESHOLD

        private const val BLUETOOTH_POLL_INTERVAL_MS = 1_500L
    }
}

/** Konversi daftar domain menjadi model UI (dipakai juga untuk logging RAW). */
fun List<BleDevice>.toUiModels(): List<BluetoothModel> = map { it.toUiModel() }

fun BleDevice.toUiModel(): BluetoothModel = BluetoothModel(
    id = address.hashCode(),
    name = name,
    macAddress = address,
    distance = distanceLabel,
    // Kolom RSSI menampilkan nilai MENTAH sesuai brief.
    rssi = "$rssi dBm",
    rssiValue = rssi,
    // Warna indikator, kategori & estimasi jarak memakai EMA agar tidak berkedip.
    signalCategory = category.label,
    indicatorRssi = smoothedRssi,
    smoothedRssiValue = smoothedRssi,
    distanceMeters = distanceMeters,
    lastSeen = lastSeen,
    isStale = isStaleAt(System.currentTimeMillis()),
)
