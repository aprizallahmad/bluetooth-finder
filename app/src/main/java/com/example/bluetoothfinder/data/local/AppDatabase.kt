package com.example.bluetoothfinder.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [DeviceHistoryEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun deviceDao(): DeviceDao

    companion object {
        const val NAME = "bluetooth_finder.db"
    }
}
