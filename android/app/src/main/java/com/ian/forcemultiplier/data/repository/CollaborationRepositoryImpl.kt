package com.ian.forcemultiplier.data.repository

import com.ian.forcemultiplier.data.remote.dto.CollaboratorDto
import com.ian.forcemultiplier.data.remote.dto.NoteCommentDto
import com.ian.forcemultiplier.domain.model.Collaborator
import com.ian.forcemultiplier.domain.model.CollaborationRole
import com.ian.forcemultiplier.domain.model.NoteComment
import com.ian.forcemultiplier.domain.repository.CollaborationRepository
import com.ian.forcemultiplier.util.Resource
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.realtime.decodeRecord
import io.github.jan.supabase.realtime.decodeOldRecord
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.coroutines.launch
import javax.inject.Inject

class CollaborationRepositoryImpl @Inject constructor(
    private val supabase: SupabaseClient
) : CollaborationRepository {

    // ── Mapping helpers ───────────────────────────────────────────────────────

    private fun CollaboratorDto.toDomain() = Collaborator(
        id           = id ?: "",
        noteId       = noteId ?: "",
        userId       = userId,
        invitedEmail = invitedEmail,
        role         = CollaborationRole.fromKey(role),
        invitedBy    = invitedBy,
        accepted     = accepted,
        createdAt    = createdAt
    )

    private fun NoteCommentDto.toDomain() = NoteComment(
        id        = id ?: "",
        noteId    = noteId,
        userId    = userId ?: "",
        content   = content,
        resolved  = resolved,
        createdAt = createdAt
    )

    private val currentUserId get() = supabase.auth.currentUserOrNull()?.id

    // ── Collaborators ─────────────────────────────────────────────────────────

    override suspend fun getCollaborators(noteId: String): Flow<Resource<List<Collaborator>>> = flow {
        emit(Resource.Loading())
        try {
            val rows = supabase.postgrest["note_collaborators"]
                .select { filter { eq("note_id", noteId) } }
                .decodeList<CollaboratorDto>()
            emit(Resource.Success(rows.map { it.toDomain() }))
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Could not load collaborators"))
        }
    }

    override suspend fun inviteCollaborator(
        noteId: String,
        email: String,
        role: CollaborationRole
    ): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        val uid = currentUserId ?: run {
            emit(Resource.Error("Not authenticated"))
            return@flow
        }
        try {
            val dto = CollaboratorDto(
                noteId       = noteId,
                userId       = null,
                invitedEmail = email.trim().lowercase(),
                role         = role.key,
                invitedBy    = uid,
                accepted     = false
            )
            supabase.postgrest["note_collaborators"].insert(dto)
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Could not send invite"))
        }
    }

    override suspend fun updateCollaboratorRole(
        collaboratorId: String,
        role: CollaborationRole
    ): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            supabase.postgrest["note_collaborators"].update(
                buildJsonObject { put("role", role.key) }
            ) { filter { eq("id", collaboratorId) } }
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Could not update role"))
        }
    }

    override suspend fun removeCollaborator(collaboratorId: String): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            supabase.postgrest["note_collaborators"]
                .delete { filter { eq("id", collaboratorId) } }
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Could not remove collaborator"))
        }
    }

    override suspend fun claimPendingInvites(email: String): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        val uid = currentUserId ?: run { emit(Resource.Success(Unit)); return@flow }
        try {
            supabase.postgrest["note_collaborators"].update(
                buildJsonObject { put("user_id", uid); put("accepted", true) }
            ) {
                filter {
                    eq("invited_email", email.trim().lowercase())
                    exact("user_id", null)
                }
            }
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Could not claim invites"))
        }
    }

    override suspend fun getMyRole(noteId: String): CollaborationRole? {
        val uid = currentUserId ?: return null
        return try {
            // Check if owner
            val noteRows = supabase.postgrest["notes"]
                .select { filter { eq("id", noteId); eq("user_id", uid) } }
                .decodeList<Map<String, String>>()
            if (noteRows.isNotEmpty()) return CollaborationRole.OWNER

            // Check collaborator role
            val collab = supabase.postgrest["note_collaborators"]
                .select { filter { eq("note_id", noteId); eq("user_id", uid) } }
                .decodeList<CollaboratorDto>()
                .firstOrNull()
            collab?.let { CollaborationRole.fromKey(it.role) }
        } catch (_: Exception) { null }
    }

    // ── Comments ──────────────────────────────────────────────────────────────

    override suspend fun getComments(noteId: String): Flow<Resource<List<NoteComment>>> = flow {
        emit(Resource.Loading())
        try {
            val rows = supabase.postgrest["note_comments"]
                .select { filter { eq("note_id", noteId) } }
                .decodeList<NoteCommentDto>()
            emit(Resource.Success(rows.map { it.toDomain() }))
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Could not load comments"))
        }
    }

    override suspend fun addComment(noteId: String, content: String): Flow<Resource<NoteComment>> = flow {
        emit(Resource.Loading())
        val uid = currentUserId ?: run {
            emit(Resource.Error("Not authenticated"))
            return@flow
        }
        try {
            val dto = NoteCommentDto(noteId = noteId, userId = uid, content = content.trim())
            val inserted = supabase.postgrest["note_comments"]
                .insert(dto) { select() }
                .decodeSingle<NoteCommentDto>()
            emit(Resource.Success(inserted.toDomain()))
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Could not post comment"))
        }
    }

    override suspend fun resolveComment(commentId: String): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            supabase.postgrest["note_comments"].update(
                buildJsonObject { put("resolved", true) }
            ) { filter { eq("id", commentId) } }
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Could not resolve comment"))
        }
    }

    override suspend fun deleteComment(commentId: String): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            supabase.postgrest["note_comments"]
                .delete { filter { eq("id", commentId) } }
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Could not delete comment"))
        }
    }

    override fun subscribeToComments(noteId: String): Flow<List<NoteComment>> = channelFlow {
        val channel = supabase.realtime.channel("comments-$noteId")
        val comments = mutableListOf<NoteComment>()

        // Seed with current comments
        try {
            val seed = supabase.postgrest["note_comments"]
                .select { filter { eq("note_id", noteId) } }
                .decodeList<NoteCommentDto>()
                .map { it.toDomain() }
            comments.addAll(seed)
            trySend(comments.toList())
        } catch (_: Exception) {}

        // 1. Define the flow BEFORE subscribing
        val changes = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "note_comments"
            filter("note_id", FilterOperator.EQ, noteId)
        }

        // 2. Subscribe to the channel
        channel.subscribe()

        // 3. Collect changes
        launch {
            changes.collect { action ->
                when (action) {
                    is PostgresAction.Insert -> {
                        val new = action.decodeRecord<NoteCommentDto>().toDomain()
                        if (comments.none { it.id == new.id }) {
                            comments.add(new)
                        }
                    }
                    is PostgresAction.Update -> {
                        val updated = action.decodeRecord<NoteCommentDto>().toDomain()
                        val index = comments.indexOfFirst { it.id == updated.id }
                        if (index != -1) {
                            comments[index] = updated
                        } else {
                            comments.add(updated)
                        }
                    }
                    is PostgresAction.Delete -> {
                        val old = action.decodeOldRecord<NoteCommentDto>()
                        comments.removeAll { it.id == old.id }
                    }
                    else -> {}
                }
                trySend(comments.toList().sortedBy { it.createdAt ?: "" })
            }
        }

        awaitClose {
            launch {
                supabase.realtime.removeChannel(channel)
            }
        }
    }
}
