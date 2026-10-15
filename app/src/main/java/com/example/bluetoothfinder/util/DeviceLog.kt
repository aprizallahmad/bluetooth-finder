package com.example.bluetoothfinder.util

import com.example.bluetoothfinder.model.BluetoothModel
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Utility logging (Timber) untuk mengintip isi data perangkat selama pengujian.
 *
 * Dua level cetak:
 *  - [logDevices] : snapshot **seluruh** daftar (array of object) — dipanggil saat
 *    data baru diterima repository (sebelum filter).
 *  - [logDeviceRender] : satu object lengkap ketika barisnya dirender di LazyColumn.
 *
 * Logging hanya ditanam pada build debug (lihat [App.onCreate]); di build release
 * tidak ada tree yang ditanam sehingga panggilan ini jadi no-op (hemat & aman).
 */
object DeviceLog {

    /** Format waktu yang mudah dibaca di Logcat. */
    private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

    /** Waktu sekarang dengan milidetik — untuk melacak urutan & jeda antar update. */
    fun now(): String = timeFormat.format(Date())

    /**
     * Cetak seluruh daftar perangkat sebagai **array of object** JSON (indentasi 2 spasi).
     *
     * @param tag   penanda alur, mis. "RAW" (dari repository) atau "UI" (hasil filter).
     * @param label keterangan tambahan yang ikut dicetak di baris header.
     */
    fun logDevices(tag: String, label: String, devices: List<BluetoothModel>) {
        val array = JSONArray()
        devices.forEach { array.put(it.toJson()) }
        // JSONObject.toString(indent) menghasilkan JSON ber-indentasi rapi.
        val pretty = array.toString(2)
        Timber.tag(TAG).d("[%s] %s • %s • %d perangkat:\n%s", tag, label, now(), devices.size, pretty)
    }

    /** Cetak satu object perangkat ketika barisnya dirender di LazyColumn. */
    fun logDeviceRender(device: BluetoothModel) {
        Timber.tag(TAG).d("[RENDER] %s • %s", now(), device.toJson().toString(0))
    }

    private const val TAG = "DeviceData"
}

/** Format [epochMillis] menjadi jam ber-milikdetik, mis. "14:32:07.812". */
internal fun formatMillis(epochMillis: Long): String =
    SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date(epochMillis))

/**
 * Konversi model UI menjadi JSON **lengkap** (semua field), agar mudah dipantau di Logcat.
 */
fun BluetoothModel.toJson(): JSONObject = JSONObject().apply {
    put("id", id)
    put("name", name)
    put("macAddress", macAddress)
    put("distance", distance)
    put("rssi", rssi) // string tampilan, mis. "-62 dBm"
    put("signalCategory", signalCategory)
    put("rssiValue", rssiValue) // raw dBm numerik (dipakai filter ambang)
    put("indicatorRssi", indicatorRssi) // EMA (dipakai warna indikator)
    put("smoothedRssiValue", smoothedRssiValue)
    put("distanceMeters", distanceMeters)
    put("lastSeen", lastSeen) // epoch millis
    put("lastSeenAt", formatMillis(lastSeen)) // versi mudah dibaca
    put("isStale", isStale)
}
