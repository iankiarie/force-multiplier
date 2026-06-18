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
    suspend fun getNoteById(noteId: Int): NoteEntity?

    @Query("SELECT * FROM noteentity WHERE user_id = :userId ORDER BY created_at DESC")
    suspend fun getNotesByUser(userId: Int): List<NoteEntity>

    @Query("SELECT * FROM noteentity WHERE user_id = :userId AND (title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%') ORDER BY created_at DESC")
    suspend fun searchNotesByUser(userId: Int, query: String): List<NoteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity): Long

    @Update
    suspend fun updateNote(note: NoteEntity): Int

    @Delete
    suspend fun deleteNote(note: NoteEntity): Int
}