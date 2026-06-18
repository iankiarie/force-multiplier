package com.ian.forcemultiplier.data.local.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import com.ian.forcemultiplier.data.local.entity.BetEntity
import com.ian.forcemultiplier.data.local.entity.NoteEntity
import com.ian.forcemultiplier.data.local.entity.PredictionEntity
import com.ian.forcemultiplier.data.local.entity.TransactionEntity
import com.ian.forcemultiplier.data.local.entity.UserEntity
import com.ian.forcemultiplier.data.local.dao.BetDao
import com.ian.forcemultiplier.data.local.dao.NoteDao
import com.ian.forcemultiplier.data.local.dao.PredictionDao
import com.ian.forcemultiplier.data.local.dao.TransactionDao
import com.ian.forcemultiplier.data.local.dao.UserDao

@Database(
    entities = [
        UserEntity::class,
        PredictionEntity::class,
        BetEntity::class,
        TransactionEntity::class,
        NoteEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun predictionDao(): PredictionDao
    abstract fun betDao(): BetDao
    abstract fun transactionDao(): TransactionDao
    abstract fun noteDao(): NoteDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "forcemultiplier_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}