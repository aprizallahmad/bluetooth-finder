package com.example.bluetoothfinder.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bluetoothfinder.data.ble.ScanStatus
import com.example.bluetoothfinder.data.local.DeviceHistoryEntity
import com.example.bluetoothfinder.data.repository.DeviceRepository
import com.example.bluetoothfinder.domain.BleDevice
import com.example.bluetoothfinder.domain.SignalCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel untuk Screen 2 (Detail Pelacakan / Radar View).
 *
 * Target dilacak berdasarkan alamat perangkat yang dikirim melalui navigation argument.
 * Scanning otomatis dijalankan selama layar pelacakan terbuka (dan dihentikan kembali
 * bila layar ini yang memulainya) agar estimasi jarak selalu real-time.
 */
@HiltViewModel
class RadarViewModel @Inject constructor(
    private val repository: DeviceRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val address: String = savedStateHandle.get<String>(ARG_ADDRESS)
        // runCatching: Uri.decode bisa gagal pada unit test (stub) -> pakai nilai asli.
        ?.let { raw -> runCatching { android.net.Uri.decode(raw) }.getOrDefault(raw) }
        .orEmpty()

    /** Perangkat target yang sedang dilacak (null bila belum terlihat di sesi ini). */
    val target: StateFlow<BleDevice?> = repository.devices
        .map { list -> list.firstOrNull { it.address == address } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )

    /** Data terakhir dari database lokal (nama, waktu pertama/terakhir terdeteksi). */
    val history: StateFlow<DeviceHistoryEntity?> = repository.observeDevice(address)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )

    val scanStatus: StateFlow<ScanStatus> = repository.scanStatus
    val bluetoothOn: StateFlow<Boolean> = repository.bluetoothOn

    private val _isLost = MutableStateFlow(true)
    val isLost: StateFlow<Boolean> = _isLost.asStateFlow()

    /** True selama nama perangkat dibaca via koneksi GATT (bila tidak diiklankan). */
    private val _nameReading = MutableStateFlow(false)
    val nameReading: StateFlow<Boolean> = _nameReading.asStateFlow()

    /** True bila pembacaan nama via GATT gagal (perangkat tidak connectable, dll). */
    private val _nameFailed = MutableStateFlow(false)
    val nameFailed: StateFlow<Boolean> = _nameFailed.asStateFlow()

    private var nameReadTried = false

    private val _events = MutableSharedFlow<String>(
        extraBufferCapacity = 8,
        onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST,
    )
    val events: SharedFlow<String> = _events.asSharedFlow()

    /** Kategori sinyal terakhir yang pernah terlihat (untuk menampilkan status Lost). */
    val lastKnownCategory: SignalCategory?
        get() = target.value?.category ?: history.value?.let {
            SignalCategory.fromRssi(it.lastRssi)
        }

    private var autoStarted = false
    private var pollJob: Job? = null

    init {
        if (repository.scanStatus.value != ScanStatus.SCANNING) {
            repository.startScan()
            autoStarted = true
        }
        viewModelScope.launch {
            repository.errors.collect { _events.tryEmit(it) }
        }
        viewModelScope.launch {
            // Begitu target terlihat pertama kali, baca langsung namanya via GATT
            // bila nama tidak tersedia dari iklan BLE (kasus umum: HP).
            target.filterNotNull().first()
            maybeResolveName()
        }
    }

    /**
     * Membaca nama perangkat via koneksi GATT (characteristic Device Name 0x2A00)
     * untuk target yang namanya tidak dibroadcast dalam iklan BLE. Nama hasil baca
     * sama dengan nama di Pengaturan -> Bluetooth perangkat tersebut dan otomatis
     * tersinkron ke daftar scan + riwayat.
     */
    private fun maybeResolveName() {
        if (nameReadTried || address.isBlank()) return
        val current = target.value ?: return
        if (current.name != BleDevice.UNKNOWN_NAME) return
        resolveName()
    }

    /** Percobaan ulang membaca nama (dipanggil tombol "Coba lagi" di layar Detail). */
    fun onRetryName() {
        if (address.isBlank() || _nameReading.value) return
        resolveName()
    }

    private fun resolveName() {
        nameReadTried = true
        _nameReading.value = true
        _nameFailed.value = false
        repository.readDeviceName(address) { result ->
            _nameReading.value = false
            result
                .onSuccess { name -> _events.tryEmit("Nama perangkat ditemukan: $name") }
                .onFailure { _nameFailed.value = true }
        }
    }

    /**
     * Dipanggil saat layar pelacakan masuk ke STARTED.
     * Penjeda scan saat aplikasi ke latar belakang ditangani repository
     * (lifecycle aplikasi), sehingga berpindah layar tidak mematikan scan.
     */
    fun onUiStarted() {
        repository.refreshBluetoothState()
        startTicker()
    }

    /** Dipanggil saat layar pelacakan keluar dari STARTED (pindah layar / background). */
    fun onUiStopped() {
        stopTicker()
    }

    fun startTracking() {
        repository.refreshBluetoothState()
        repository.startScan()
    }

    /**
     * Menandai status "Lost": target tidak terlihat lebih dari [LOST_TIMEOUT_MS]
     * atau pemindaian/Bluetooth berhenti.
     */
    private fun startTicker() {
        if (pollJob?.isActive == true) return
        pollJob = viewModelScope.launch {
            while (isActive) {
                delay(TICK_INTERVAL_MS)
                repository.refreshBluetoothState()
                evaluateLost()
            }
        }
    }

    private fun stopTicker() {
        pollJob?.cancel()
        pollJob = null
    }

    private fun evaluateLost() {
        val current = target.value
        val now = System.currentTimeMillis()
        _isLost.value = when {
            scanStatus.value != ScanStatus.SCANNING -> true
            !bluetoothOn.value -> true
            current == null -> true
            else -> now - current.lastSeen > LOST_TIMEOUT_MS
        }
    }

    override fun onCleared() {
        stopTicker()
        // Layar ini yang memulai scan -> layar ini juga yang menghentikannya.
        if (autoStarted) repository.stopScan()
        super.onCleared()
    }

    companion object {
        const val ARG_ADDRESS = "address"
        private const val TICK_INTERVAL_MS = 1_000L
        private const val LOST_TIMEOUT_MS = 8_000L
    }
}
