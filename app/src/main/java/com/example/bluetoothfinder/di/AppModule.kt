package com.example.bluetoothfinder.di

import android.content.Context
import androidx.room.Room
import com.example.bluetoothfinder.data.ble.AndroidBleScanner
import com.example.bluetoothfinder.data.ble.BleScanner
import com.example.bluetoothfinder.data.local.AppDatabase
import com.example.bluetoothfinder.data.local.DeviceDao
import com.example.bluetoothfinder.data.repository.DefaultDeviceRepository
import com.example.bluetoothfinder.data.repository.DeviceRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

/**
 * Dependency Injection graph.
 *
 * Semua dependensi bersifat singleton sehingga state pemindaian BLE
 * tidak hilang saat rotasi layar.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.NAME)
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideDeviceDao(database: AppDatabase): DeviceDao = database.deviceDao()

    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(): CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class BindingsModule {

    @Binds
    @Singleton
    abstract fun bindBleScanner(impl: AndroidBleScanner): BleScanner

    @Binds
    @Singleton
    abstract fun bindDeviceRepository(impl: DefaultDeviceRepository): DeviceRepository
}
