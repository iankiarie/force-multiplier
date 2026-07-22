package com.ian.forcemultiplier.presentation.vault.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ian.forcemultiplier.domain.model.Collaborator
import com.ian.forcemultiplier.domain.model.CollaborationRole
import com.ian.forcemultiplier.domain.model.NoteComment
import com.ian.forcemultiplier.domain.repository.CollaborationRepository
import com.ian.forcemultiplier.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CollaborationViewModel @Inject constructor(
    private val repo: CollaborationRepository
) : ViewModel() {

    // ── Collaborators ─────────────────────────────────────────────────────────
    private val _collaborators = MutableStateFlow<Resource<List<Collaborator>>>(Resource.Loading())
    val collaborators: StateFlow<Resource<List<Collaborator>>> = _collaborators.asStateFlow()

    // ── Current user's role on the open note ──────────────────────────────────
    private val _myRole = MutableStateFlow<CollaborationRole?>(null)
    val myRole: StateFlow<CollaborationRole?> = _myRole.asStateFlow()

    // ── Comments ──────────────────────────────────────────────────────────────
    private val _comments = MutableStateFlow<Resource<List<NoteComment>>>(Resource.Loading())
    val comments: StateFlow<Resource<List<NoteComment>>> = _comments.asStateFlow()

    // ── Invite UI state ───────────────────────────────────────────────────────
    private val _inviteResult = MutableStateFlow<Resource<Unit>?>(null)
    val inviteResult: StateFlow<Resource<Unit>?> = _inviteResult.asStateFlow()

    // Track active jobs to avoid multiple parallel subscriptions/collectors
    private var activeNoteId: String? = null
    private var collabsJob: Job? = null
    private var commentsJob: Job? = null

    // ── Public actions ────────────────────────────────────────────────────────

    fun loadForNote(noteId: String) {
        if (activeNoteId == noteId) return
        activeNoteId = noteId

        // Cancel previous note's listeners
        collabsJob?.cancel()
        commentsJob?.cancel()

        viewModelScope.launch {
            // Resolve role first so UI knows what to show
            _myRole.value = repo.getMyRole(noteId)
            
            collabsJob = launch {
                repo.getCollaborators(noteId).collect { _collaborators.value = it }
            }
            
            commentsJob = launch {
                repo.subscribeToComments(noteId).collect { list ->
                    _comments.value = Resource.Success(list)
                }
            }
        }
    }

    fun inviteCollaborator(noteId: String, email: String, role: CollaborationRole) {
        if (email.isBlank()) return
        viewModelScope.launch {
            _inviteResult.value = Resource.Loading()
            repo.inviteCollaborator(noteId, email, role).collect { result ->
                _inviteResult.value = result
                if (result is Resource.Success) {
                    // Trigger a refresh of the collaborators list
                    viewModelScope.launch {
                        repo.getCollaborators(noteId).collect { _collaborators.value = it }
                    }
                }
            }
        }
    }

    fun updateRole(collaboratorId: String, role: CollaborationRole) {
        viewModelScope.launch {
            repo.updateCollaboratorRole(collaboratorId, role).collect {
                activeNoteId?.let { noteId -> loadForNote(noteId) }
            }
        }
    }

    fun removeCollaborator(collaboratorId: String) {
        viewModelScope.launch {
            repo.removeCollaborator(collaboratorId).collect {
                activeNoteId?.let { noteId -> loadForNote(noteId) }
            }
        }
    }

    fun addComment(noteId: String, content: String) {
        if (content.isBlank()) return
        viewModelScope.launch {
            // optimistic: realtime will push the final version
            repo.addComment(noteId, content).collect { /* result handled via realtime */ }
        }
    }

    fun resolveComment(commentId: String) {
        viewModelScope.launch {
            repo.resolveComment(commentId).collect {
                // Realtime will pick up the update automatically
            }
        }
    }

    fun deleteComment(commentId: String) {
        viewModelScope.launch {
            repo.deleteComment(commentId).collect {
                // Realtime will pick up the deletion automatically
            }
        }
    }

    fun clearInviteResult() { _inviteResult.value = null }
}
