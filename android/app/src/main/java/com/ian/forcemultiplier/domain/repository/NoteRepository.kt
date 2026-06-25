package com.ian.forcemultiplier.domain.repository

import com.ian.forcemultiplier.data.remote.dto.NoteDto
import com.ian.forcemultiplier.util.Resource
import kotlinx.coroutines.flow.Flow

interface NoteRepository {
    suspend fun getNotes(): Flow<Resource<List<NoteDto>>>
    suspend fun getNoteById(noteId: String): Flow<Resource<NoteDto>>
    suspend fun createNote(note: NoteDto): Flow<Resource<NoteDto>>
    suspend fun updateNote(note: NoteDto): Flow<Resource<NoteDto>>
    suspend fun deleteNote(noteId: String): Flow<Resource<Unit>>
}
