package com.ian.forcemultiplier.presentation.vault.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ian.forcemultiplier.data.remote.dto.NoteDto
import com.ian.forcemultiplier.domain.model.Folder
import com.ian.forcemultiplier.domain.repository.NoteRepository
import com.ian.forcemultiplier.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    private val _foldersState = MutableStateFlow<Resource<List<Folder>>>(Resource.Loading())
    val foldersState: StateFlow<Resource<List<Folder>>> = _foldersState.asStateFlow()

    private val _selectedFolder = MutableStateFlow<Folder?>(null)
    val selectedFolder: StateFlow<Folder?> = _selectedFolder.asStateFlow()

    init {
        getFolders()
        getNotes()
    }

    fun getFolders() {
        viewModelScope.launch {
            repository.getFolders().collect {
                _foldersState.value = it
            }
        }
    }

    fun selectFolder(folder: Folder?) {
        _selectedFolder.value = folder
    }

    fun clearFolderSelection() {
        _selectedFolder.value = null
    }

    fun createFolder(name: String, icon: String? = null, tagFilter: String? = null) {
        viewModelScope.launch {
            repository.createFolder(name, icon, tagFilter).collect {
                if (it is Resource.Success) getFolders()
            }
        }
    }

    fun renameFolder(folderId: String, name: String) {
        viewModelScope.launch {
            repository.renameFolder(folderId, name).collect {
                if (it is Resource.Success) getFolders()
            }
        }
    }

    fun deleteFolder(folderId: String) {
        viewModelScope.launch {
            repository.deleteFolder(folderId).collect {
                if (it is Resource.Success) {
                    getFolders()
                    getNotes()
                    if (_selectedFolder.value?.id == folderId) _selectedFolder.value = null
                }
            }
        }
    }

    fun moveNoteToFolder(noteId: String, folderId: String?) {
        viewModelScope.launch {
            repository.moveNoteToFolder(noteId, folderId).collect {
                if (it is Resource.Success) getNotes()
            }
        }
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
