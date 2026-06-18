package com.ian.forcemultiplier.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index
import androidx.room.ColumnInfo
import androidx.room.ForeignKey

@Entity(
    indices = [Index(value = ["email"], unique = true), Index(value = ["username"], unique = true)],
    foreignKeys = []
)
data class UserEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    @ColumnInfo(name = "email")
    val email: String,
    @ColumnInfo(name = "username")
    val username: String,
    @ColumnInfo(name = "full_name")
    val fullName: String?,
    @ColumnInfo(name = "hashed_password")
    val hashedPassword: String,
    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,
    @ColumnInfo(name = "role")
    val role: String = "user", // We'll store as string for simplicity, or use an enum class with Room
    @ColumnInfo(name = "points")
    val points: Int = 0,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long? = null
)