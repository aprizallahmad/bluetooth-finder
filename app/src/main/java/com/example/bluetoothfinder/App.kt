package com.example.bluetoothfinder

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

/**
 * Entry point aplikasi untuk Dependency Injection (Hilt).
 *
 * Seluruh binding (database, scanner, repository) didefinisikan di package `di`.
 */
@HiltAndroidApp
class App : Application() {

    override fun onCreate() {
        super.onCreate()
        // Logging hanya di build debug; hilang otomatis di build release.
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}
