package com.example.bluetoothfinder.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Memastikan filter & pengurutan daftar bekerja sesuai brief:
 * - pengurutan otomatis dari RSSI tertinggi,
 * - filter nama / MAC address,
 * - filter ambang batas sinyal minimum (mis. ≥ -80 dBm).
 */
class DeviceFilterTest {

    private fun device(
        name: String,
        address: String,
        rssi: Int,
    ): BleDevice = BleDevice(
        address = address,
        name = name,
        rssi = rssi,
        smoothedRssi = rssi,
        txPower = null,
        lastSeen = 0L,
        seenCount = 1,
    )

    private val devices = listOf(
        device("Speaker Lemah", "AA:BB:CC:00:00:01", -85),
        device("Galaxy Buds", "AA:BB:CC:00:00:02", -62),
        device("Mi Smart Band", "AA:BB:CC:00:00:03", -71),
        device("JBL Flip", "AA:BB:CC:00:00:04", -48),
    )

    @Test
    fun `diurutkan dari sinyal terkuat - rssi tertinggi`() {
        val sorted = devices.sortedBySignal()
        assertEquals(listOf(-48, -62, -71, -85), sorted.map { it.rssi })
    }

    @Test
    fun `filter ambang sinyal memakai nilai rssi mentah yang sama dengan tampilan`() {
        val filtered = devices.filteredBy(query = "", minRssi = -80)
        // -85 dBm < -80 dBm -> ikut terbuang
        assertEquals(listOf(-48, -62, -71), filtered.map { it.rssi }.sortedDescending())
        assertTrue(filtered.none { it.name == "Speaker Lemah" })
    }

    @Test
    fun `filter ambang sangat ketat hanya menyisakan sinyal kuat`() {
        val filtered = devices.filteredBy(query = "", minRssi = -60)
        assertEquals(listOf("JBL Flip"), filtered.map { it.name })
    }

    @Test
    fun `tanpa ambang sinyal semua perangkat tampil`() {
        val filtered = devices.filteredBy(query = "", minRssi = NO_RSSI_THRESHOLD)
        assertEquals(devices.size, filtered.size)
    }

    @Test
    fun `pencarian nama tidak membedakan huruf besar kecil`() {
        val filtered = devices.filteredBy(query = "galaxy", minRssi = NO_RSSI_THRESHOLD)
        assertEquals(listOf("Galaxy Buds"), filtered.map { it.name })
    }

    @Test
    fun `pencarian mendukung sebagian MAC address`() {
        val filtered = devices.filteredBy(query = "00:00:03", minRssi = NO_RSSI_THRESHOLD)
        assertEquals(listOf("Mi Smart Band"), filtered.map { it.name })
    }

    @Test
    fun `kata kunci kosong tidak menyaring apa pun`() {
        assertEquals(devices.size, devices.filteredBy("   ", NO_RSSI_THRESHOLD).size)
    }

    @Test
    fun `kombinasi pencarian dan ambang sinyal`() {
        val filtered = devices.filteredBy(query = "Mi", minRssi = -70)
        // "Mi Smart Band" (-71 dBm) gugur karena di bawah ambang -70 dBm
        assertTrue(filtered.isEmpty())
    }

    // ---------------------------------------------------------------- EMA vs raw

    /** Perangkat dengan nilai RSSI mentah yang berbeda dari nilai EMA (halus). */
    private fun deviceWithEma(
        name: String,
        address: String,
        rssi: Int,
        ema: Int,
    ): BleDevice = BleDevice(
        address = address,
        name = name,
        rssi = rssi,
        smoothedRssi = ema,
        txPower = null,
        lastSeen = 0L,
        seenCount = 1,
    )

    @Test
    fun `pengurutan memakai nilai ema agar urutan baris tidak serobot tiap paket`() {
        // Raw: A (-50) lebih kuat dari B (-70), tetapi EMA: B (-60) lebih kuat dari A (-75).
        val a = deviceWithEma(name = "Perangkat A", address = "AA:01", rssi = -50, ema = -75)
        val b = deviceWithEma(name = "Perangkat B", address = "AA:02", rssi = -70, ema = -60)

        val sorted = listOf(a, b).sortedBySignal()

        assertEquals(listOf("Perangkat B", "Perangkat A"), sorted.map { it.name })
    }

    @Test
    fun `filter ambang tetap memakai nilai mentah bukan ema`() {
        // Raw -65 >= -80 -> lolos, walau EMA-nya (-95) berada di bawah ambang.
        val device = deviceWithEma(name = "Lonceng", address = "AA:03", rssi = -65, ema = -95)

        val filtered = listOf(device).filteredBy(query = "", minRssi = -80)

        assertEquals(1, filtered.size)
        // Angka yang ditampilkan di layar = nilai mentah yang dipakai filter.
        assertEquals(-65, filtered.first().rssi)
    }
}
