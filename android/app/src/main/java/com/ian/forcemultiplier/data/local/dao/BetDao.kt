package com.ian.forcemultiplier.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ian.forcemultiplier.data.local.entity.BetEntity

@Dao
interface BetDao {
    @Query("SELECT * FROM betentity WHERE id = :betId")
    suspend fun getBetById(betId: String): BetEntity?

    @Query("SELECT * FROM betentity WHERE user_id = :userId ORDER BY created_at DESC")
    suspend fun getBetsByUser(userId: String): List<BetEntity>

    @Query("SELECT * FROM betentity WHERE prediction_id = :predictionId")
    suspend fun getBetsByPrediction(predictionId: String): List<BetEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBet(bet: BetEntity)

    @Update
    suspend fun updateBet(bet: BetEntity): Int

    @Delete
    suspend fun deleteBet(bet: BetEntity): Int
}
