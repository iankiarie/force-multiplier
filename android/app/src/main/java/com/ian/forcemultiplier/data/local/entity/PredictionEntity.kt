package com.ian.forcemultiplier.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index
import androidx.room.ColumnInfo
import androidx.room.ForeignKey

@Entity(
    indices = [Index(value = ["deadline"]), Index(value = ["created_by"])],
    foreignKeys = [ForeignKey(entity = UserEntity::class, parentColumns = ["id"], childColumns = ["created_by"])]
)
data class PredictionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    @ColumnInfo(name = "title")
    val title: String,
    @ColumnInfo(name = "description")
    val description: String?,
    @ColumnInfo(name = "outcome")
    val outcome: String?, // We'll store as string, or use an enum class with Room
    @ColumnInfo(name = "deadline")
    val deadline: Long, // Store as timestamp (milliseconds)
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "resolved_at")
    val resolvedAt: Long? = null,
    @ColumnInfo(name = "created_by")
    val createdBy: Int
)