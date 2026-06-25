package com.ian.forcemultiplier.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ian.forcemultiplier.data.local.entity.NoteEntity

@Dao
interface NoteDao {
    @Query("SELECT * FROM noteentity WHERE id = :noteId")
    suspend fun getNoteById(noteId: String): NoteEntity?

    @Query("SELECT * FROM noteentity WHERE user_id = :userId ORDER BY created_at DESC")
    suspend fun getNotesByUser(userId: String): List<NoteEntity>

    @Query("SELECT * FROM noteentity WHERE user_id = :userId AND (title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%') ORDER BY created_at DESC")
    suspend fun searchNotesByUser(userId: String, query: String): List<NoteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity)

    @Update
    suspend fun updateNote(note: NoteEntity): Int

    @Delete
    suspend fun deleteNote(note: NoteEntity): Int
}