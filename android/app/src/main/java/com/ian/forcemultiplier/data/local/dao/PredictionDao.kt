package com.ian.forcemultiplier.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ian.forcemultiplier.data.local.entity.PredictionEntity

@Dao
interface PredictionDao {
    @Query("SELECT * FROM predictionentity WHERE id = :predictionId")
    suspend fun getPredictionById(predictionId: Int): PredictionEntity?

    @Query("SELECT * FROM predictionentity WHERE resolved_at IS NULL AND deadline > :currentTime ORDER BY deadline ASC")
    suspend fun getActivePredictions(currentTime: Long): List<PredictionEntity>

    @Query("SELECT * FROM predictionentity WHERE created_by = :userId ORDER BY created_at DESC")
    suspend fun getPredictionsByCreator(userId: Int): List<PredictionEntity>

    @Query("SELECT * FROM predictionentity ORDER BY created_at DESC LIMIT :limit OFFSET :offset")
    suspend fun getPredictions(limit: Int, offset: Int): List<PredictionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrediction(prediction: PredictionEntity): Long

    @Update
    suspend fun updatePrediction(prediction: PredictionEntity): Int

    @Delete
    suspend fun deletePrediction(prediction: PredictionEntity): Int
}