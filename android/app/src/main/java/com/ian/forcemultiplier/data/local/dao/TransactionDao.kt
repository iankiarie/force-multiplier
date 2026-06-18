package com.ian.forcemultiplier.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ian.forcemultiplier.data.local.entity.TransactionEntity

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactionentity WHERE id = :transactionId")
    suspend fun getTransactionById(transactionId: Int): TransactionEntity?

    @Query("SELECT * FROM transactionentity WHERE user_id = :userId ORDER BY created_at DESC")
    suspend fun getTransactionsByUser(userId: Int): List<TransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity): Int

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity): Int
}
