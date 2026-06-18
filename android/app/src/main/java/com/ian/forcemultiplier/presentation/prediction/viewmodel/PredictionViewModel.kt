package com.ian.forcemultiplier.presentation.prediction.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ian.forcemultiplier.data.remote.dto.PredictionDto
import com.ian.forcemultiplier.domain.repository.PredictionRepository
import com.ian.forcemultiplier.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PredictionViewModel @Inject constructor(
    private val repository: PredictionRepository
) : ViewModel() {

    private val _predictionsState = MutableStateFlow<Resource<List<PredictionDto>>>(Resource.Loading())
    val predictionsState: StateFlow<Resource<List<PredictionDto>>> = _predictionsState

    init {
        loadPredictions()
    }

    fun loadPredictions() {
        viewModelScope.launch {
            repository.getPredictions().collect { result ->
                _predictionsState.value = result
            }
        }
    }
}
