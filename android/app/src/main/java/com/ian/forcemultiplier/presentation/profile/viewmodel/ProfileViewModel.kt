package com.ian.forcemultiplier.presentation.profile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ian.forcemultiplier.data.remote.dto.UserDto
import com.ian.forcemultiplier.domain.repository.DashboardRepository
import com.ian.forcemultiplier.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repository: DashboardRepository,
    private val supabaseClient: SupabaseClient
) : ViewModel() {

    private val _userState = MutableStateFlow<Resource<UserDto>>(Resource.Loading())
    val userState: StateFlow<Resource<UserDto>> = _userState

    /** Map of "yyyy-MM-dd" -> contribution count (notes created that day) */
    private val _contributionState = MutableStateFlow<Resource<Map<String, Int>>>(Resource.Loading())
    val contributionState: StateFlow<Resource<Map<String, Int>>> = _contributionState

    init {
        loadProfile()
        loadContributions()
    }

    fun loadProfile() {
        viewModelScope.launch {
            repository.getCurrentUser().collect { result ->
                _userState.value = result
            }
        }
    }

    private fun loadContributions() {
        viewModelScope.launch {
            repository.getContributions().collect { result ->
                _contributionState.value = result
            }
        }
    }

    fun logout(onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                supabaseClient.auth.signOut()
                onSuccess()
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
}
