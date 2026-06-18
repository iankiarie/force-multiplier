package com.ian.forcemultiplier.presentation.leaderboard.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ian.forcemultiplier.data.remote.dto.UserDto
import com.ian.forcemultiplier.domain.repository.LeaderboardRepository
import com.ian.forcemultiplier.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LeaderboardViewModel @Inject constructor(
    private val repository: LeaderboardRepository
) : ViewModel() {

    private val _leaderboardState = MutableStateFlow<Resource<List<UserDto>>>(Resource.Loading())
    val leaderboardState: StateFlow<Resource<List<UserDto>>> = _leaderboardState

    init {
        loadLeaderboard("monthly")
    }

    fun loadLeaderboard(period: String) {
        viewModelScope.launch {
            repository.getLeaderboard(period).collect { result ->
                _leaderboardState.value = result
            }
        }
    }
}
