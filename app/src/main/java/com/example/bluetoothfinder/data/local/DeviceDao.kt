package com.example.bluetoothfinder.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface DeviceDao {

    @Query("SELECT * FROM device_history WHERE address = :address")
    fun observeByAddress(address: String): Flow<DeviceHistoryEntity?>

    @Query("SELECT * FROM device_history WHERE address = :address")
    suspend fun getByAddress(address: String): DeviceHistoryEntity?

    @Query("SELECT * FROM device_history ORDER BY lastSeenAt DESC")
    fun observeHistory(): Flow<List<DeviceHistoryEntity>>

    @Upsert
    suspend fun upsert(entity: DeviceHistoryEntity)

    @Query("DELETE FROM device_history")
    suspend fun clearHistory(): Int
}
