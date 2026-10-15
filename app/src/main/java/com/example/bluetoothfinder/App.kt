package com.example.bluetoothfinder

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Entry point aplikasi untuk Dependency Injection (Hilt).
 *
 * Seluruh binding (database, scanner, repository) didefinisikan di package `di`.
 */
@HiltAndroidApp
class App : Application()
