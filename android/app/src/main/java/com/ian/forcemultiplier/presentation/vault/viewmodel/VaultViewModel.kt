package com.ian.forcemultiplier.presentation.vault.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ian.forcemultiplier.data.remote.dto.NoteDto
import com.ian.forcemultiplier.domain.repository.NoteRepository
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
class VaultViewModel @Inject constructor(
    private val repository: NoteRepository
) : ViewModel() {

    private val _notesState = MutableStateFlow<Resource<List<NoteDto>>>(Resource.Loading())
    val notesState: StateFlow<Resource<List<NoteDto>>> = _notesState.asStateFlow()

    private val _currentNoteState = MutableStateFlow<Resource<NoteDto>?>(null)
    val currentNoteState: StateFlow<Resource<NoteDto>?> = _currentNoteState.asStateFlow()

    init {
        getNotes()
    }

    fun getNotes() {
        viewModelScope.launch {
            repository.getNotes().collect {
                _notesState.value = it
            }
        }
    }

    fun getNoteById(id: String) {
        viewModelScope.launch {
            repository.getNoteById(id).collect {
                _currentNoteState.value = it
            }
        }
    }

    fun createNote(note: NoteDto) {
        viewModelScope.launch {
            repository.createNote(note).collect {
                if (it is Resource.Success) {
                    getNotes()
                }
                _currentNoteState.value = it
            }
        }
    }

    fun updateNote(note: NoteDto) {
        viewModelScope.launch {
            repository.updateNote(note).collect {
                if (it is Resource.Success) {
                    getNotes()
                }
                _currentNoteState.value = it
            }
        }
    }

    fun deleteNote(id: String) {
        viewModelScope.launch {
            repository.deleteNote(id).collect {
                if (it is Resource.Success) {
                    getNotes()
                }
            }
        }
    }

    fun clearCurrentNote() {
        _currentNoteState.value = null
    }
}
