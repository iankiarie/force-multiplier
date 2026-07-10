package com.ian.forcemultiplier.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ian.forcemultiplier.data.local.entity.FolderEntity

@Dao
interface FolderDao {
    @Query("SELECT * FROM folderentity WHERE user_id = :userId ORDER BY sort_order ASC, name ASC")
    suspend fun getFoldersByUser(userId: String): List<FolderEntity>

    @Query("SELECT COUNT(*) FROM folderentity WHERE user_id = :userId")
    suspend fun countFoldersByUser(userId: String): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertFolders(folders: List<FolderEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: FolderEntity)

    @Query("UPDATE folderentity SET name = :name WHERE id = :folderId")
    suspend fun renameFolder(folderId: String, name: String): Int

    @Delete
    suspend fun deleteFolder(folder: FolderEntity): Int

    @Query("DELETE FROM folderentity WHERE id = :folderId")
    suspend fun deleteFolderById(folderId: String): Int
}

