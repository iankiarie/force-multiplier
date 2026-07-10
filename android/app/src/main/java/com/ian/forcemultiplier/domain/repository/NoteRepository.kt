package com.ian.forcemultiplier.domain.repository

import com.ian.forcemultiplier.data.remote.dto.NoteDto
import com.ian.forcemultiplier.domain.model.Folder
import com.ian.forcemultiplier.util.Resource
import kotlinx.coroutines.flow.Flow

interface NoteRepository {
    suspend fun getNotes(): Flow<Resource<List<NoteDto>>>
    suspend fun getNoteById(noteId: String): Flow<Resource<NoteDto>>
    suspend fun createNote(note: NoteDto): Flow<Resource<NoteDto>>
    suspend fun updateNote(note: NoteDto): Flow<Resource<NoteDto>>
    suspend fun deleteNote(noteId: String): Flow<Resource<Unit>>
    suspend fun migrateGuestNotesToUser(userId: String): Flow<Resource<Unit>>
    suspend fun getFolders(): Flow<Resource<List<Folder>>>
    suspend fun createFolder(name: String, icon: String? = null, tagFilter: String? = null): Flow<Resource<Unit>>
    suspend fun renameFolder(folderId: String, name: String): Flow<Resource<Unit>>
    suspend fun deleteFolder(folderId: String): Flow<Resource<Unit>>
    suspend fun moveNoteToFolder(noteId: String, folderId: String?): Flow<Resource<Unit>>
}
