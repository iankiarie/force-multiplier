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

    // ── All active predictions (for the Active tab) ──────────
    private val _predictionsState = MutableStateFlow<Resource<List<PredictionDto>>>(Resource.Loading())
    val predictionsState: StateFlow<Resource<List<PredictionDto>>> = _predictionsState.asStateFlow()

    // ── The current user's bet placements ────────────────────
    private val _myBetsState = MutableStateFlow<Resource<List<BetDto>>>(Resource.Loading())
    val myBetsState: StateFlow<Resource<List<BetDto>>> = _myBetsState.asStateFlow()

    // ── Predictions created by the current user ───────────────
    private val _myCreatedState = MutableStateFlow<Resource<List<PredictionDto>>>(Resource.Loading())
    val myCreatedState: StateFlow<Resource<List<PredictionDto>>> = _myCreatedState.asStateFlow()

    // ── Coin balance ──────────────────────────────────────────
    private val _coinBalance = MutableStateFlow<Int?>(100)
    val coinBalance: StateFlow<Int?> = _coinBalance.asStateFlow()

    // ── Bet placement / creation / resolve feedback ───────────
    private val _betResult = MutableStateFlow<BetPlacementResult?>(null)
    val betResult: StateFlow<BetPlacementResult?> = _betResult.asStateFlow()

    private val _isPlacingBet = MutableStateFlow(false)
    val isPlacingBet: StateFlow<Boolean> = _isPlacingBet.asStateFlow()

    private val _isCreatingPrediction = MutableStateFlow(false)
    val isCreatingPrediction: StateFlow<Boolean> = _isCreatingPrediction.asStateFlow()

    private val _isResolvingPrediction = MutableStateFlow(false)
    val isResolvingPrediction: StateFlow<Boolean> = _isResolvingPrediction.asStateFlow()

    init {
        loadAll()
    }

    fun loadAll() {
        loadPredictions()
        loadMyBets()
        loadMyCreatedPredictions()
        loadCoinBalance()
    }

    // ── Load all active predictions ──────────────────────────
    fun loadPredictions() {
        viewModelScope.launch {
            predictionRepository.getPredictions().collect { result ->
                _predictionsState.value = result
            }
        }
    }

    // ── Load the user's own bet placements ───────────────────
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

    // ── Load predictions created by this user ────────────────
    fun loadMyCreatedPredictions() {
        viewModelScope.launch {
            val user = supabaseClient.auth.currentUserOrNull() ?: return@launch
            predictionRepository.getMyPredictions(user.id).collect { result ->
                _myCreatedState.value = result
            }
        }
    }

    // ── Load coin balance from user_profiles ─────────────────
    fun loadCoinBalance() {
        viewModelScope.launch {
            when (val result = predictionRepository.getCoinBalance()) {
                is Resource.Success -> _coinBalance.value = result.data
                else -> _coinBalance.value = 100 // fallback: new-user default
            }
        }
    }

    // ── Place a bet ──────────────────────────────────────────
    fun placeBet(predictionId: String, optionId: String, amount: Int) {
        viewModelScope.launch {
            _isPlacingBet.value = true
            _betResult.value = null
            try {
                val user = supabaseClient.auth.currentUserOrNull()
                    ?: run {
                        _betResult.value = BetPlacementResult(false, "Not logged in")
                        return@launch
                    }
                if (amount <= 0) {
                    _betResult.value = BetPlacementResult(false, "Stake must be greater than 0")
                    return@launch
                }
                val balance = _coinBalance.value ?: 100
                if (amount > balance) {
                    _betResult.value = BetPlacementResult(false, "Not enough coins (balance: $balance)")
                    return@launch
                }

                supabaseClient.postgrest["bets"].insert(
                    BetDto(
                        predictionId = predictionId,
                        userId       = user.id,
                        optionId     = optionId,
                        amount       = amount
                    )
                ) { select() }
                _betResult.value = BetPlacementResult(true, "🎯 Bet placed! Good luck.")
                // Refresh balance and bets list
                loadMyBets()
                loadCoinBalance()
                loadPredictions() // option stake totals updated
            } catch (e: Exception) {
                val msg = e.localizedMessage ?: "Failed to place bet"
                val friendly = when {
                    msg.contains("Insufficient coins", ignoreCase = true) ->
                        "Not enough coins to place this bet."
                    msg.contains("duplicate", ignoreCase = true) ||
                    msg.contains("unique", ignoreCase = true)   ->
                        "You already have a bet on this prediction."
                    else -> msg
                }
                _betResult.value = BetPlacementResult(false, friendly)
            } finally {
                _isPlacingBet.value = false
            }
        }
    }

    // ── Create a new prediction ──────────────────────────────
    fun createPrediction(
        title: String,
        description: String?,
        category: String?,
        options: List<String>,
        endsAt: String
    ) {
        viewModelScope.launch {
            _isCreatingPrediction.value = true
            _betResult.value = null
            when (val result = predictionRepository.createPrediction(
                title, description, category, options, endsAt
            )) {
                is Resource.Success -> {
                    _betResult.value = BetPlacementResult(true, "✅ Prediction created!")
                    loadPredictions()
                    loadMyCreatedPredictions()
                }
                is Resource.Error -> {
                    _betResult.value = BetPlacementResult(false, result.message ?: "Failed to create prediction")
                }
                else -> {}
            }
            _isCreatingPrediction.value = false
        }
    }

    // ── Resolve a prediction (creator picks winner) ──────────
    fun resolvePrediction(predictionId: String, winningOptionId: String) {
        viewModelScope.launch {
            _isResolvingPrediction.value = true
            _betResult.value = null
            when (val result = predictionRepository.resolvePrediction(predictionId, winningOptionId)) {
                is Resource.Success -> {
                    _betResult.value = BetPlacementResult(true, "🏆 Prediction resolved! Winners paid out.")
                    loadMyCreatedPredictions()
                    loadMyBets()
                    loadCoinBalance()
                    loadPredictions()
                }
                is Resource.Error -> {
                    _betResult.value = BetPlacementResult(false, result.message ?: "Failed to resolve")
                }
                else -> {}
            }
            _isResolvingPrediction.value = false
        }
    }

    fun clearBetResult() { _betResult.value = null }
}
