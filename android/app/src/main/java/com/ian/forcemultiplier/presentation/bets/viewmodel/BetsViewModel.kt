package com.ian.forcemultiplier.presentation.bets.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ian.forcemultiplier.data.remote.dto.BetDto
import com.ian.forcemultiplier.data.remote.dto.PredictionDto
import com.ian.forcemultiplier.domain.repository.PredictionRepository
import com.ian.forcemultiplier.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BetPlacementResult(
    val success: Boolean,
    val message: String
)

@HiltViewModel
class BetsViewModel @Inject constructor(
    private val predictionRepository: PredictionRepository,
    private val supabaseClient: SupabaseClient
) : ViewModel() {

    private val _predictionsState = MutableStateFlow<Resource<List<PredictionDto>>>(Resource.Loading())
    val predictionsState: StateFlow<Resource<List<PredictionDto>>> = _predictionsState.asStateFlow()

    private val _myBetsState = MutableStateFlow<Resource<List<BetDto>>>(Resource.Loading())
    val myBetsState: StateFlow<Resource<List<BetDto>>> = _myBetsState.asStateFlow()

    private val _betResult = MutableStateFlow<BetPlacementResult?>(null)
    val betResult: StateFlow<BetPlacementResult?> = _betResult.asStateFlow()

    private val _isPlacingBet = MutableStateFlow(false)
    val isPlacingBet: StateFlow<Boolean> = _isPlacingBet.asStateFlow()

    init {
        loadPredictions()
        loadMyBets()
    }

    fun loadPredictions() {
        viewModelScope.launch {
            predictionRepository.getPredictions().collect { result ->
                _predictionsState.value = result
            }
        }
    }

    fun loadMyBets() {
        viewModelScope.launch {
            try {
                val user = supabaseClient.auth.currentUserOrNull()
                if (user != null) {
                    val bets = supabaseClient.postgrest["bets"]
                        .select { filter { eq("user_id", user.id) } }
                        .decodeList<BetDto>()
                    _myBetsState.value = Resource.Success(bets.sortedByDescending { it.createdAt })
                } else {
                    _myBetsState.value = Resource.Success(emptyList())
                }
            } catch (e: Exception) {
                _myBetsState.value = Resource.Error(e.localizedMessage ?: "Failed to load bets")
            }
        }
    }

    fun placeBet(predictionId: String, chosenOutcome: String, stake: Int) {
        viewModelScope.launch {
            _isPlacingBet.value = true
            _betResult.value = null
            try {
                val user = supabaseClient.auth.currentUserOrNull()
                    ?: run {
                        _betResult.value = BetPlacementResult(false, "Not logged in")
                        _isPlacingBet.value = false
                        return@launch
                    }
                if (stake <= 0) {
                    _betResult.value = BetPlacementResult(false, "Stake must be greater than 0")
                    _isPlacingBet.value = false
                    return@launch
                }

                val bet = mapOf(
                    "prediction_id" to predictionId,
                    "user_id" to user.id,
                    "chosen_outcome" to chosenOutcome,
                    "stake" to stake,
                    "settled" to false
                )
                supabaseClient.postgrest["bets"].insert(bet)
                _betResult.value = BetPlacementResult(true, "Bet placed! Good luck.")
                loadMyBets()
            } catch (e: Exception) {
                _betResult.value = BetPlacementResult(false, e.localizedMessage ?: "Failed to place bet")
            } finally {
                _isPlacingBet.value = false
            }
        }
    }

    fun clearBetResult() { _betResult.value = null }
}
