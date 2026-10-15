package com.example.bluetoothfinder.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Memastikan pemetaan RSSI (dBm) -> kategori sinyal -> perkiraan jarak
 * sesuai tabel pada brief study case.
 */
class SignalCategoryTest {

    @Test
    fun `rssi -10 sampai -30 dikategorikan sangat kuat`() {
        assertEquals(SignalCategory.VERY_STRONG, SignalCategory.fromRssi(-10))
        assertEquals(SignalCategory.VERY_STRONG, SignalCategory.fromRssi(-20))
        assertEquals(SignalCategory.VERY_STRONG, SignalCategory.fromRssi(-30))
        // Di atas -10 tetap sangat kuat (batas fisik jangkauan dekat)
        assertEquals(SignalCategory.VERY_STRONG, SignalCategory.fromRssi(-5))
    }

    @Test
    fun `rssi -31 sampai -50 dikategorikan kuat`() {
        assertEquals(SignalCategory.STRONG, SignalCategory.fromRssi(-31))
        assertEquals(SignalCategory.STRONG, SignalCategory.fromRssi(-45))
        assertEquals(SignalCategory.STRONG, SignalCategory.fromRssi(-50))
    }

    @Test
    fun `rssi -51 sampai -70 dikategorikan cukup baik`() {
        assertEquals(SignalCategory.GOOD, SignalCategory.fromRssi(-51))
        assertEquals(SignalCategory.GOOD, SignalCategory.fromRssi(-60))
        assertEquals(SignalCategory.GOOD, SignalCategory.fromRssi(-70))
    }

    @Test
    fun `rssi -71 sampai -80 dikategorikan lemah`() {
        assertEquals(SignalCategory.WEAK, SignalCategory.fromRssi(-71))
        assertEquals(SignalCategory.WEAK, SignalCategory.fromRssi(-75))
        assertEquals(SignalCategory.WEAK, SignalCategory.fromRssi(-80))
    }

    @Test
    fun `rssi -81 sampai -90 dikategorikan sangat lemah`() {
        assertEquals(SignalCategory.VERY_WEAK, SignalCategory.fromRssi(-81))
        assertEquals(SignalCategory.VERY_WEAK, SignalCategory.fromRssi(-85))
        assertEquals(SignalCategory.VERY_WEAK, SignalCategory.fromRssi(-90))
    }

    @Test
    fun `rssi di bawah -90 dianggap sinyal hilang`() {
        assertEquals(SignalCategory.LOST, SignalCategory.fromRssi(-91))
        assertEquals(SignalCategory.LOST, SignalCategory.fromRssi(-100))
    }

    @Test
    fun `jarak pada radar semakin besar seiring sinyal melemah`() {
        val fractions = SignalCategory.entries.map { it.radarFraction }
        for (i in 1 until fractions.size) {
            assertTrue(
                "Fraksi radar harus monoton naik dari VERY_STRONG ke LOST",
                fractions[i] > fractions[i - 1],
            )
        }
    }

    @Test
    fun `setiap kategori memiliki label jarak sesuai brief`() {
        assertEquals("< 1 meter", SignalCategory.VERY_STRONG.distanceLabel)
        assertEquals("1 - 3 meter", SignalCategory.STRONG.distanceLabel)
        assertEquals("3 - 10 meter", SignalCategory.GOOD.distanceLabel)
        assertEquals("10 - 20 meter", SignalCategory.WEAK.distanceLabel)
        assertEquals("> 20 meter (batas jangkauan)", SignalCategory.VERY_WEAK.distanceLabel)
        assertEquals("Terputus / di luar jangkauan", SignalCategory.LOST.distanceLabel)
    }
}
