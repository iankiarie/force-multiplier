package com.ian.forcemultiplier.data.repository

import com.ian.forcemultiplier.data.local.dao.NoteDao
import com.ian.forcemultiplier.data.mapper.toNoteDto
import com.ian.forcemultiplier.data.mapper.toNoteEntity
import com.ian.forcemultiplier.data.remote.dto.NoteDto
import com.ian.forcemultiplier.domain.repository.NoteRepository
import com.ian.forcemultiplier.util.Resource
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class NoteRepositoryImpl @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val dao: NoteDao
) : NoteRepository {

    override suspend fun getNotes(): Flow<Resource<List<NoteDto>>> = flow {
        emit(Resource.Loading())
        try {
            val user = supabaseClient.auth.currentUserOrNull()
            if (user != null) {
                val remoteNotes = supabaseClient.postgrest["notes"]
                    .select {
                        filter { eq("user_id", user.id) }
                    }.decodeList<NoteDto>()

                // Cache remotely fetched notes locally
                remoteNotes.forEach { dao.insertNote(it.toNoteEntity()) }

                // Merge with any offline-only notes for this user
                val localOnly = dao.getNotesByUser(user.id)
                val merged = (remoteNotes + localOnly.map { it.toNoteDto() }).distinctBy { it.id }
                emit(Resource.Success(merged))
            } else {
                // Offline / logged-out: serve from local DB
                val localNotes = dao.getNotesByUser("demo_user")
                emit(Resource.Success(localNotes.map { it.toNoteDto() }))
            }
        } catch (e: Exception) {
            // Network error — fall back to local cache
            val user = supabaseClient.auth.currentUserOrNull()
            val userId = user?.id ?: "demo_user"
            val localNotes = dao.getNotesByUser(userId)
            if (localNotes.isNotEmpty()) {
                emit(Resource.Success(localNotes.map { it.toNoteDto() }.distinctBy { it.id }))
            } else {
                emit(Resource.Error(e.localizedMessage ?: "Could not load notes"))
            }
        }
    }

    override suspend fun getNoteById(noteId: String): Flow<Resource<NoteDto>> = flow {
        emit(Resource.Loading())
        try {
            val note = supabaseClient.postgrest["notes"]
                .select { filter { eq("id", noteId) } }
                .decodeSingle<NoteDto>()
            emit(Resource.Success(note))
        } catch (e: Exception) {
            // Fallback to local
            val localNote = dao.getNoteById(noteId)
            if (localNote != null) {
                emit(Resource.Success(localNote.toNoteDto()))
            } else {
                emit(Resource.Error(e.localizedMessage ?: "Note not found"))
            }
        }
    }

    override suspend fun createNote(note: NoteDto): Flow<Resource<NoteDto>> = flow {
        emit(Resource.Loading())
        val user = supabaseClient.auth.currentUserOrNull()
        val noteToInsert = note.copy(userId = user?.id ?: "demo_user")
        try {
            if (user != null) {
                // Persist to Supabase and get back the server-assigned record
                val inserted = supabaseClient.postgrest["notes"]
                    .insert(noteToInsert) { select() }
                    .decodeSingle<NoteDto>()
                dao.insertNote(inserted.toNoteEntity())
                emit(Resource.Success(inserted))
            } else {
                // Offline save
                val localNote = noteToInsert.copy(id = java.util.UUID.randomUUID().toString())
                dao.insertNote(localNote.toNoteEntity())
                emit(Resource.Success(localNote))
            }
        } catch (e: Exception) {
            // Supabase failed — save locally so nothing is lost
            val localId = java.util.UUID.randomUUID().toString()
            // Use noteToInsert so userId is the real auth user, not "demo_user"
            val fallback = noteToInsert.copy(id = localId)
            try {
                dao.insertNote(fallback.toNoteEntity())
                emit(Resource.Success(fallback))
            } catch (dbEx: Exception) {
                emit(Resource.Error(e.localizedMessage ?: "Failed to save note"))
            }
        }
    }

    override suspend fun updateNote(note: NoteDto): Flow<Resource<NoteDto>> = flow {
        emit(Resource.Loading())
        try {
            val user = supabaseClient.auth.currentUserOrNull()
            if (user != null && note.id != null) {
                // Push update to Supabase (UUIDs always contain hyphens — bug was previously reversed)
                val noteToUpdate = note.copy(userId = user.id)
                val updated = supabaseClient.postgrest["notes"]
                    .update(noteToUpdate) {
                        filter { eq("id", note.id) }
                        select()
                    }.decodeSingle<NoteDto>()
                dao.insertNote(updated.toNoteEntity())
                emit(Resource.Success(updated))
            } else {
                // Offline or no ID — update locally
                if (note.id != null) dao.insertNote(note.toNoteEntity())
                emit(Resource.Success(note))
            }
        } catch (e: Exception) {
            // Supabase failed — keep local copy up to date
            if (note.id != null) {
                try { dao.insertNote(note.toNoteEntity()) } catch (_: Exception) {}
            }
            emit(Resource.Success(note))
        }
    }

    override suspend fun deleteNote(noteId: String): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            supabaseClient.postgrest["notes"]
                .delete { filter { eq("id", noteId) } }
        } catch (_: Exception) { /* continue with local delete */ }
        try {
            val local = dao.getNoteById(noteId)
            if (local != null) dao.deleteNote(local)
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Failed to delete note"))
        }
    }
}
