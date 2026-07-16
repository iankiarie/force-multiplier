package com.ian.forcemultiplier.domain.repository

import com.ian.forcemultiplier.domain.model.Collaborator
import com.ian.forcemultiplier.domain.model.CollaborationRole
import com.ian.forcemultiplier.domain.model.NoteComment
import com.ian.forcemultiplier.util.Resource
import kotlinx.coroutines.flow.Flow

interface CollaborationRepository {

    /** Fetch current collaborators on a note (owner sees all; others see peers). */
    suspend fun getCollaborators(noteId: String): Flow<Resource<List<Collaborator>>>

    /** Invite a user by email with the given role. Creates a pending invite row. */
    suspend fun inviteCollaborator(
        noteId: String,
        email: String,
        role: CollaborationRole
    ): Flow<Resource<Unit>>

    /** Change an existing collaborator's role (owner only). */
    suspend fun updateCollaboratorRole(
        collaboratorId: String,
        role: CollaborationRole
    ): Flow<Resource<Unit>>

    /** Remove a collaborator (owner can remove anyone; user can remove themselves). */
    suspend fun removeCollaborator(collaboratorId: String): Flow<Resource<Unit>>

    /**
     * After login, claim any pending invites addressed to [email].
     * Sets user_id = current user and accepted = true.
     */
    suspend fun claimPendingInvites(email: String): Flow<Resource<Unit>>

    /**
     * Return the current user's role on [noteId].
     * Returns [CollaborationRole.OWNER] if the user owns the note,
     * the collaborator role if they are a collaborator, or null if no access.
     */
    suspend fun getMyRole(noteId: String): CollaborationRole?

    // ── Comments ──────────────────────────────────────────────────────────────

    /** One-shot fetch of all comments on a note. */
    suspend fun getComments(noteId: String): Flow<Resource<List<NoteComment>>>

    /** Post a new comment. */
    suspend fun addComment(noteId: String, content: String): Flow<Resource<NoteComment>>

    /** Soft-resolve a comment (marks it done without deleting). */
    suspend fun resolveComment(commentId: String): Flow<Resource<Unit>>

    /** Delete a comment (author or note owner). */
    suspend fun deleteComment(commentId: String): Flow<Resource<Unit>>

    /**
     * Hot flow that emits the full comment list whenever a new comment
     * is inserted via Supabase Realtime. Subscribe only when the detail
     * screen is open; cancel when leaving.
     */
    fun subscribeToComments(noteId: String): Flow<List<NoteComment>>
}
