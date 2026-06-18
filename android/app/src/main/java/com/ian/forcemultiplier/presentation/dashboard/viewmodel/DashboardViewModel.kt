package com.ian.forcemultiplier.presentation.dashboard.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ian.forcemultiplier.data.remote.dto.UserDto
import com.ian.forcemultiplier.domain.repository.DashboardRepository
import com.ian.forcemultiplier.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: DashboardRepository
) : ViewModel() {

    private val _userState = MutableStateFlow<Resource<UserDto>>(Resource.Loading())
    val userState: StateFlow<Resource<UserDto>> = _userState

    init {
        loadCurrentUser()
    }

    fun loadCurrentUser() {
        viewModelScope.launch {
            repository.getCurrentUser().collect { result ->
                _userState.value = result
            }
        }
    }
}
