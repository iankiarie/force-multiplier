package com.ian.forcemultiplier.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index
import androidx.room.ColumnInfo
import androidx.room.ForeignKey

@Entity(
    indices = [Index(value = ["user_id"]), Index(value = ["created_at"])],
    foreignKeys = [ForeignKey(entity = UserEntity::class, parentColumns = ["id"], childColumns = ["user_id"])]
)
data class NoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    @ColumnInfo(name = "title")
    val title: String,
    @ColumnInfo(name = "content")
    val content: String,
    @ColumnInfo(name = "tags")
    val tags: String?, // Store as comma-separated string
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long? = null,
    @ColumnInfo(name = "user_id")
    val userId: Int
)