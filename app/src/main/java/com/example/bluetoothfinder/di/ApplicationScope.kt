package com.example.bluetoothfinder.di

import javax.inject.Qualifier

/**
 * Qualifier untuk CoroutineScope aplikasi (SupervisorJob + Main.immediate).
 * Dipakai oleh scanner & repository agar proses background tetap hidup
 * melebihi umur sebuah Activity/ViewModel.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope
