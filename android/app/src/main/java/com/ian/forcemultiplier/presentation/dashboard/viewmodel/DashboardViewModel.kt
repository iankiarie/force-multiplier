package com.ian.forcemultiplier.presentation.dashboard.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ian.forcemultiplier.data.remote.dto.ActivityDto
import com.ian.forcemultiplier.data.remote.dto.UserDto
import com.ian.forcemultiplier.domain.repository.DashboardRepository
import com.ian.forcemultiplier.domain.repository.LeaderboardRepository
import com.ian.forcemultiplier.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: DashboardRepository,
    private val leaderboardRepository: LeaderboardRepository
) : ViewModel() {

    private val _userState = MutableStateFlow<Resource<UserDto>>(Resource.Loading())
    val userState: StateFlow<Resource<UserDto>> = _userState.asStateFlow()

    private val _activityState = MutableStateFlow<Resource<List<ActivityDto>>>(Resource.Loading())
    val activityState: StateFlow<Resource<List<ActivityDto>>> = _activityState.asStateFlow()

    // The #1 user on the leaderboard — shown in the hero card on the dashboard
    private val _leaderState = MutableStateFlow<Resource<UserDto?>>(Resource.Loading())
    val leaderState: StateFlow<Resource<UserDto?>> = _leaderState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        loadCurrentUser()
        loadTeamActivity()
        loadLeader()
    }

    private fun loadCurrentUser() {
        viewModelScope.launch {
            repository.getCurrentUser().collect { result ->
                _userState.value = result
            }
        }
    }

    private fun loadTeamActivity() {
        viewModelScope.launch {
            repository.getTeamActivity().collect { result ->
                _activityState.value = result
            }
        }
    }

    private fun loadLeader() {
        viewModelScope.launch {
            leaderboardRepository.getLeaderboard("all").collect { result ->
                _leaderState.value = when (result) {
                    is Resource.Success -> Resource.Success(result.data?.firstOrNull())
                    is Resource.Error   -> Resource.Success(null) // non-fatal; hero card handles null
                    is Resource.Loading -> Resource.Loading()
                }
            }
        }
    }
}
