package com.example.bluetoothfinder.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * Menyederhanakan pengecekan izin BLE yang berbeda per level API:
 * - Android 12 (S) ke atas : BLUETOOTH_SCAN + BLUETOOTH_CONNECT (runtime).
 * - Sebelum Android 12     : ACCESS_FINE_LOCATION.
 */
object BluetoothPermissions {

    fun required(): Array<String> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
        )
    } else {
        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    fun hasAll(context: Context): Boolean = required().all { permission ->
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    /** True bila pengguna sudah menekan "Don't ask again" -> harus buka Settings. */
    fun shouldShowRationale(context: Context, activity: android.app.Activity? = null): Boolean =
        required().any { activity?.shouldShowRequestPermissionRationale(it) == true }

    val isLegacyLocationBased: Boolean
        get() = Build.VERSION.SDK_INT < Build.VERSION_CODES.S
}
