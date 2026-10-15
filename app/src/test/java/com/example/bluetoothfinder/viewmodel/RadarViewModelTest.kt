package com.example.bluetoothfinder.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.example.bluetoothfinder.data.ble.ScanStatus
import com.example.bluetoothfinder.data.local.DeviceHistoryEntity
import com.example.bluetoothfinder.data.repository.DeviceRepository
import com.example.bluetoothfinder.domain.BleDevice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Memastikan pembacaan nama perangkat via koneksi GATT (untuk perangkat yang tidak
 * mengiklankan nama, mis. HP) berjalan benar di layar Detail:
 * - otomatis terpicu saat target "Tanpa Nama" terdeteksi,
 * - tidak terpicu bila nama sudah tersedia dari iklan BLE,
 * - status loading/gagal + percobaan ulang.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RadarViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeDeviceRepository : DeviceRepository {
        val devicesFlow = MutableStateFlow<List<BleDevice>>(emptyList())
        val scanStatusFlow = MutableStateFlow(ScanStatus.SCANNING)

        val readNameCalls = mutableListOf<String>()
        var pendingNameCallback: ((Result<String>) -> Unit)? = null

        override val devices: StateFlow<List<BleDevice>> = devicesFlow
        override val scanStatus: StateFlow<ScanStatus> = scanStatusFlow
        override val bluetoothOn: StateFlow<Boolean> = MutableStateFlow(true)
        override val errors: SharedFlow<String> = MutableSharedFlow(
            extraBufferCapacity = 8,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )
        override val history: Flow<List<DeviceHistoryEntity>> = MutableStateFlow(emptyList())

        override fun observeDevice(address: String): Flow<DeviceHistoryEntity?> =
            MutableStateFlow(null)

        override fun startScan() = Unit
        override fun stopScan() = Unit
        override fun refreshBluetoothState() = Unit
        override suspend fun clearHistory() = Unit

        override fun readDeviceName(address: String, onResult: (Result<String>) -> Unit) {
            readNameCalls += address
            pendingNameCallback = onResult
        }
    }

    private fun device(name: String) = BleDevice(
        address = TARGET_ADDRESS,
        name = name,
        rssi = -55,
        smoothedRssi = -55,
        txPower = null,
        lastSeen = System.currentTimeMillis(),
        seenCount = 3,
    )

    private fun viewModel(repo: FakeDeviceRepository) = RadarViewModel(
        repository = repo,
        savedStateHandle = SavedStateHandle(
            mapOf(RadarViewModel.ARG_ADDRESS to TARGET_ADDRESS),
        ),
    )

    @Test
    fun `membaca nama otomatis saat target tanpa nama terdeteksi`() = runTest {
        val repo = FakeDeviceRepository().apply {
            devicesFlow.value = listOf(device(BleDevice.UNKNOWN_NAME))
        }
        viewModel(repo)
        advanceUntilIdle()

        assertEquals(listOf(TARGET_ADDRESS), repo.readNameCalls)
    }

    @Test
    fun `tidak membaca nama bila nama sudah tersedia dari iklan BLE`() = runTest {
        val repo = FakeDeviceRepository().apply {
            devicesFlow.value = listOf(device("Galaxy Buds"))
        }
        viewModel(repo)
        advanceUntilIdle()

        assertTrue(repo.readNameCalls.isEmpty())
    }

    @Test
    fun `pembacaan nama berjalan - status loading aktif lalu selesai tanpa gagal`() = runTest {
        val repo = FakeDeviceRepository().apply {
            devicesFlow.value = listOf(device(BleDevice.UNKNOWN_NAME))
        }
        val vm = viewModel(repo)
        advanceUntilIdle()
        assertTrue(vm.nameReading.value)

        repo.pendingNameCallback?.invoke(Result.success("POCO X3 NFC"))
        advanceUntilIdle()

        assertFalse(vm.nameReading.value)
        assertFalse(vm.nameFailed.value)
    }

    @Test
    fun `pembacaan nama gagal - nameFailed dan tombol coba lagi memanggil ulang`() = runTest {
        val repo = FakeDeviceRepository().apply {
            devicesFlow.value = listOf(device(BleDevice.UNKNOWN_NAME))
        }
        val vm = viewModel(repo)
        advanceUntilIdle()

        repo.pendingNameCallback?.invoke(Result.failure(RuntimeException("tidak connectable")))
        advanceUntilIdle()
        assertTrue(vm.nameFailed.value)
        assertFalse(vm.nameReading.value)

        vm.onRetryName()
        advanceUntilIdle()
        assertEquals(2, repo.readNameCalls.size)
        assertTrue(vm.nameReading.value)
    }

    private companion object {
        const val TARGET_ADDRESS = "AA:BB:CC:DD:EE:FF"
    }
}
