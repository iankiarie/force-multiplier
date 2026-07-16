package com.ian.forcemultiplier.presentation.vault.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ian.forcemultiplier.domain.model.Collaborator
import com.ian.forcemultiplier.domain.model.CollaborationRole
import com.ian.forcemultiplier.domain.model.NoteComment
import com.ian.forcemultiplier.domain.repository.CollaborationRepository
import com.ian.forcemultiplier.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
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

    // Track the active note so we can reload
    private var activeNoteId: String? = null

    // ── Public actions ────────────────────────────────────────────────────────

    fun loadForNote(noteId: String) {
        activeNoteId = noteId
        viewModelScope.launch {
            // Resolve role first so UI knows what to show
            _myRole.value = repo.getMyRole(noteId)
            loadCollaborators(noteId)
            subscribeToComments(noteId)
        }
    }

    private fun loadCollaborators(noteId: String) {
        viewModelScope.launch {
            repo.getCollaborators(noteId).onEach { _collaborators.value = it }.launchIn(this)
        }
    }

    private fun subscribeToComments(noteId: String) {
        viewModelScope.launch {
            repo.subscribeToComments(noteId).collect { list ->
                _comments.value = Resource.Success(list)
            }
        }
    }

    fun inviteCollaborator(noteId: String, email: String, role: CollaborationRole) {
        if (email.isBlank()) return
        viewModelScope.launch {
            _inviteResult.value = Resource.Loading()
            repo.inviteCollaborator(noteId, email, role).collect { result ->
                _inviteResult.value = result
                if (result is Resource.Success) loadCollaborators(noteId)
            }
        }
    }

    fun updateRole(collaboratorId: String, role: CollaborationRole) {
        viewModelScope.launch {
            repo.updateCollaboratorRole(collaboratorId, role).collect {
                activeNoteId?.let { noteId -> loadCollaborators(noteId) }
            }
        }
    }

    fun removeCollaborator(collaboratorId: String) {
        viewModelScope.launch {
            repo.removeCollaborator(collaboratorId).collect {
                activeNoteId?.let { noteId -> loadCollaborators(noteId) }
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
                // refresh on resolve
                activeNoteId?.let { noteId -> subscribeToComments(noteId) }
            }
        }
    }

    fun deleteComment(commentId: String) {
        viewModelScope.launch {
            repo.deleteComment(commentId).collect {
                // remove locally immediately for snappy UX
                val current = (_comments.value as? Resource.Success)?.data ?: return@collect
                _comments.value = Resource.Success(current.filter { it.id != commentId })
            }
        }
    }

    fun clearInviteResult() { _inviteResult.value = null }
}
