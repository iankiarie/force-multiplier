package com.ian.forcemultiplier.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.ian.forcemultiplier.data.local.entity.SyncEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncDao {
    @Query("SELECT * FROM sync_queue WHERE status = 'PENDING' OR status = 'FAILED' ORDER BY timestamp ASC")
    fun getPendingItems(): Flow<List<SyncEntity>>

    @Insert
    suspend fun insertSyncItem(item: SyncEntity): Long

    @Update
    suspend fun updateSyncItem(item: SyncEntity)

    @Query("DELETE FROM sync_queue WHERE status = 'COMPLETED'")
    suspend fun clearCompleted()
}
