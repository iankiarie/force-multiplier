package com.ian.forcemultiplier.data.repository

import com.ian.forcemultiplier.data.local.dao.NoteDao
import com.ian.forcemultiplier.data.local.dao.FolderDao
import com.ian.forcemultiplier.data.local.entity.FolderEntity
import com.ian.forcemultiplier.data.mapper.toFolder
import com.ian.forcemultiplier.data.mapper.toNoteDto
import com.ian.forcemultiplier.data.mapper.toNoteEntity
import com.ian.forcemultiplier.data.remote.dto.NoteDto
import com.ian.forcemultiplier.domain.model.Folder
import com.ian.forcemultiplier.domain.repository.NoteRepository
import com.ian.forcemultiplier.util.Resource
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import java.util.UUID

class NoteRepositoryImpl @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val dao: NoteDao,
    private val folderDao: FolderDao
) : NoteRepository {

    private companion object {
        const val GUEST_USER_ID = "demo_user"
    }

    private val defaultFolderSeed = listOf(
        Triple("My Notes", "ic_note", null),
        Triple("To-do list", "ic_add", "todo"),
        Triple("Journal", "ic_note", "journal"),
        Triple("Projects", "ic_bet", "project"),
        Triple("Reading", "ic_note", "reading")
    )

    private suspend fun resolveSessionUserId(): String {
        return supabaseClient.auth.currentUserOrNull()?.id ?: GUEST_USER_ID
    }

    private suspend fun ensureDefaultFolders(userId: String) {
        if (folderDao.countFoldersByUser(userId) > 0) return
        val defaults = defaultFolderSeed.mapIndexed { index, seed ->
            FolderEntity(
                id = "${userId}_folder_$index",
                name = seed.first,
                icon = seed.second,
                tagFilter = seed.third,
                sortOrder = index,
                userId = userId
            )
        }
        folderDao.insertFolders(defaults)
    }

    private suspend fun getDefaultFolderId(userId: String): String {
        ensureDefaultFolders(userId)
        return folderDao.getFoldersByUser(userId).firstOrNull()?.id ?: "${userId}_folder_0"
    }

    private suspend fun getReplacementFolderId(userId: String, excludingFolderId: String): String {
        ensureDefaultFolders(userId)
        val folders = folderDao.getFoldersByUser(userId)
        folders.firstOrNull { it.id != excludingFolderId }?.let { return it.id }

        val replacement = FolderEntity(
            id = UUID.randomUUID().toString(),
            name = "My Notes",
            icon = "ic_note",
            sortOrder = folders.size,
            userId = userId
        )
        folderDao.insertFolder(replacement)
        return replacement.id
    }

    private suspend fun migrateUnassignedNotesToPrimaryFolder(userId: String) {
        val primaryFolderId = getDefaultFolderId(userId)
        dao.getNotesByUser(userId)
            .filter { it.folderId.isNullOrBlank() }
            .forEach { note ->
                dao.updateNoteFolder(note.id, primaryFolderId)
            }
    }

    private suspend fun getLocalNotesForSession(userId: String?): List<com.ian.forcemultiplier.data.local.entity.NoteEntity> {
        val userKeys = buildList {
            if (userId != null) add(userId)
            add(GUEST_USER_ID)
        }.distinct()

        return userKeys
            .flatMap { dao.getNotesByUser(it) }
            .distinctBy { it.id }
    }

    private suspend fun migrateNotesToUser(userId: String) {
        val guestNotes = dao.getNotesByUser(GUEST_USER_ID)
        if (guestNotes.isEmpty()) return

        // Reassign guest/offline notes to the authenticated user so they remain visible after login.
        dao.reassignNotesUser(GUEST_USER_ID, userId)

        // Ensure they are cached under the new user id as well.
        guestNotes.forEach { note ->
            dao.insertNote(note.copy(userId = userId))
        }
    }

    override suspend fun getNotes(): Flow<Resource<List<NoteDto>>> = flow {
        emit(Resource.Loading())
        try {
            val user = supabaseClient.auth.currentUserOrNull()
            ensureDefaultFolders(user?.id ?: GUEST_USER_ID)
            if (user != null) {
                val remoteNotes = supabaseClient.postgrest["notes"]
                    .select {
                        filter { eq("user_id", user.id) }
                    }.decodeList<NoteDto>()

                migrateNotesToUser(user.id)

                // Cache remotely fetched notes locally
                remoteNotes.forEach { dao.insertNote(it.toNoteEntity()) }

                migrateUnassignedNotesToPrimaryFolder(user.id)

                // Merge with any offline-only notes for this user and guest/offline scope
                val localOnly = getLocalNotesForSession(user.id)
                val merged = (remoteNotes + localOnly.map { it.toNoteDto() }).distinctBy { it.id }
                emit(Resource.Success(merged))
            } else {
                // Offline / logged-out: serve from local DB
                migrateUnassignedNotesToPrimaryFolder(GUEST_USER_ID)
                val localNotes = dao.getNotesByUser(GUEST_USER_ID)
                emit(Resource.Success(localNotes.map { it.toNoteDto() }))
            }
        } catch (e: Exception) {
            // Network error — fall back to local cache
            val user = supabaseClient.auth.currentUserOrNull()
            migrateUnassignedNotesToPrimaryFolder(user?.id ?: GUEST_USER_ID)
            val localNotes = getLocalNotesForSession(user?.id)
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
        val generatedId = note.id ?: java.util.UUID.randomUUID().toString()
        val sessionUserId = user?.id ?: GUEST_USER_ID
        val noteToInsert = note.copy(
            id = generatedId,
            userId = sessionUserId,
            folderId = note.folderId ?: getDefaultFolderId(sessionUserId)
        )
        try {
            if (user != null) {
                // Persist to Supabase and get back the server-assigned record
                val inserted = supabaseClient.postgrest["notes"]
                    .insert(noteToInsert) { select() }
                    .decodeSingle<NoteDto>()
                val persisted = inserted.copy(id = inserted.id ?: generatedId)
                dao.insertNote(persisted.toNoteEntity())
                emit(Resource.Success(persisted))
            } else {
                // Offline save
                dao.insertNote(noteToInsert.toNoteEntity())
                emit(Resource.Success(noteToInsert))
            }
        } catch (e: Exception) {
            // Supabase failed — save locally so nothing is lost
            val fallback = noteToInsert
            try {
                dao.insertNote(fallback.toNoteEntity())
                emit(Resource.Success(fallback))
            } catch (_: Exception) {
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
                val noteToUpdate = note.copy(
                    userId = user.id,
                    folderId = note.folderId ?: getDefaultFolderId(user.id)
                )
                val updated = supabaseClient.postgrest["notes"]
                    .update(noteToUpdate) {
                        filter { eq("id", note.id) }
                        select()
                    }.decodeSingle<NoteDto>()
                dao.insertNote(updated.toNoteEntity())
                emit(Resource.Success(updated))
            } else {
                // Offline or no ID — update locally
                if (note.id != null) {
                    val sessionUserId = user?.id ?: GUEST_USER_ID
                    dao.insertNote(
                        note.copy(
                            userId = sessionUserId,
                            folderId = note.folderId ?: getDefaultFolderId(sessionUserId)
                        ).toNoteEntity()
                    )
                }
                emit(Resource.Success(note))
            }
        } catch (_: Exception) {
            // Supabase failed — keep local copy up to date
            if (note.id != null) {
                try {
                    val sessionUserId = supabaseClient.auth.currentUserOrNull()?.id ?: GUEST_USER_ID
                    dao.insertNote(
                        note.copy(
                            userId = sessionUserId,
                            folderId = note.folderId ?: getDefaultFolderId(sessionUserId)
                        ).toNoteEntity()
                    )
                } catch (dbEx: Exception) {
                    emit(Resource.Error(dbEx.localizedMessage ?: "Failed to save note locally"))
                    return@flow
                }
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

    override suspend fun migrateGuestNotesToUser(userId: String): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            migrateNotesToUser(userId)
            ensureDefaultFolders(userId)
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Failed to migrate guest notes"))
        }
    }

    override suspend fun getFolders(): Flow<Resource<List<Folder>>> = flow {
        emit(Resource.Loading())
        try {
            val userId = resolveSessionUserId()
            ensureDefaultFolders(userId)
            val folders = folderDao.getFoldersByUser(userId).map { it.toFolder() }
            emit(Resource.Success(folders))
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Could not load folders"))
        }
    }

    override suspend fun createFolder(name: String, icon: String?, tagFilter: String?): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            val userId = resolveSessionUserId()
            val cleanName = name.trim()
            if (cleanName.isEmpty()) {
                emit(Resource.Error("Folder name cannot be empty"))
                return@flow
            }
            val folder = FolderEntity(
                id = UUID.randomUUID().toString(),
                name = cleanName,
                icon = icon,
                tagFilter = tagFilter,
                sortOrder = folderDao.countFoldersByUser(userId),
                userId = userId
            )
            folderDao.insertFolder(folder)
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Could not create folder"))
        }
    }

    override suspend fun renameFolder(folderId: String, name: String): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            val cleanName = name.trim()
            if (cleanName.isEmpty()) {
                emit(Resource.Error("Folder name cannot be empty"))
                return@flow
            }
            folderDao.renameFolder(folderId, cleanName)
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Could not rename folder"))
        }
    }

    override suspend fun deleteFolder(folderId: String): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            val userId = resolveSessionUserId()
            folderDao.getFoldersByUser(userId).firstOrNull { it.id == folderId } ?: return@flow emit(Resource.Success(Unit))
            val replacementFolderId = getReplacementFolderId(userId, folderId)
            dao.getNotesByFolder(userId, folderId).forEach { note ->
                dao.updateNoteFolder(note.id, replacementFolderId)
            }
            folderDao.deleteFolderById(folderId)
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Could not delete folder"))
        }
    }

    override suspend fun moveNoteToFolder(noteId: String, folderId: String?): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            dao.updateNoteFolder(noteId, folderId)
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Could not move note"))
        }
    }
}
