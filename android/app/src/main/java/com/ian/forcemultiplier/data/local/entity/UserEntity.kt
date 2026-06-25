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
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "email")
    val email: String,
    @ColumnInfo(name = "username")
    val username: String,
    @ColumnInfo(name = "full_name")
    val fullName: String?,
    @ColumnInfo(name = "hashed_password")
    val hashedPassword: String? = null,
    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,
    @ColumnInfo(name = "role")
    val role: String = "user",
    @ColumnInfo(name = "points")
    val points: Int = 0,
    @ColumnInfo(name = "streak")
    val streak: Int = 0,
    @ColumnInfo(name = "accuracy")
    val accuracy: Float = 0f,
    @ColumnInfo(name = "daily_points")
    val dailyPoints: Int = 0,
    @ColumnInfo(name = "rank")
    val rank: Int? = null,
    @ColumnInfo(name = "location")
    val location: String? = null,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long? = null
)