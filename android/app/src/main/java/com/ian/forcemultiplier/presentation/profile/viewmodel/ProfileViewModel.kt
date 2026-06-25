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

    init {
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            repository.getCurrentUser().collect { result ->
                _userState.value = result
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
