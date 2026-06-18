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
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    @ColumnInfo(name = "amount")
    val amount: Int, // Can be negative or positive
    @ColumnInfo(name = "type")
    val type: String, // bet_placed, bet_win, bonus, adjustment
    @ColumnInfo(name = "description")
    val description: String?,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "user_id")
    val userId: Int
)