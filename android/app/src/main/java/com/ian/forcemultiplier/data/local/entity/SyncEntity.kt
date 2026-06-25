package com.ian.forcemultiplier.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_queue")
data class SyncEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val entityType: String,
    val action: String, // CREATE, UPDATE, DELETE
    val payload: String, // JSON payload
    val retryCount: Int = 0,
    val status: String = "PENDING", // PENDING, PROCESSING, COMPLETED, FAILED
    val timestamp: Long = System.currentTimeMillis()
)
