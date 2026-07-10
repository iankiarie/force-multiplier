package com.ian.forcemultiplier.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index
import androidx.room.ColumnInfo

// FK removed: Supabase manages auth separately; local userId may not exist in UserEntity
@Entity(
    indices = [
        Index(value = ["user_id"]),
        Index(value = ["folder_id"]),
        Index(value = ["user_id", "folder_id"]),
        Index(value = ["created_at"])
    ]
)
data class NoteEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "title")
    val title: String,
    @ColumnInfo(name = "content")
    val content: String?,
    @ColumnInfo(name = "tags")
    val tags: String? = null,
    @ColumnInfo(name = "preview")
    val preview: String?,
    @ColumnInfo(name = "parent_id")
    val parentId: String? = null,
    @ColumnInfo(name = "icon")
    val icon: String? = null,
    @ColumnInfo(name = "cover_url")
    val coverUrl: String? = null,
    @ColumnInfo(name = "is_bookmarked")
    val isBookmarked: Boolean = false,
    @ColumnInfo(name = "folder_id")
    val folderId: String? = null,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long? = null,
    @ColumnInfo(name = "user_id")
    val userId: String?
)