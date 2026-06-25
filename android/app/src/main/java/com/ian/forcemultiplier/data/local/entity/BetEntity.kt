package com.ian.forcemultiplier.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index
import androidx.room.ColumnInfo
import androidx.room.ForeignKey

@Entity(
    indices = [Index(value = ["user_id"]), Index(value = ["prediction_id"])],
    foreignKeys = [
        ForeignKey(entity = UserEntity::class, parentColumns = ["id"], childColumns = ["user_id"]),
        ForeignKey(entity = PredictionEntity::class, parentColumns = ["id"], childColumns = ["prediction_id"])
    ]
)
data class BetEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "stake")
    val stake: Int,
    @ColumnInfo(name = "chosen_outcome")
    val chosenOutcome: String, // team_a, team_b, draw
    @ColumnInfo(name = "settled")
    val settled: Boolean = false,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "settled_at")
    val settledAt: Long? = null,
    @ColumnInfo(name = "user_id")
    val userId: String,
    @ColumnInfo(name = "prediction_id")
    val predictionId: String
)