package com.ian.forcemultiplier.presentation.bets

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.ian.forcemultiplier.R
import com.ian.forcemultiplier.core.theme.FMColors
import com.ian.forcemultiplier.data.remote.dto.BetDto
import com.ian.forcemultiplier.data.remote.dto.PredictionDto
import com.ian.forcemultiplier.presentation.bets.viewmodel.BetsViewModel
import com.ian.forcemultiplier.util.Resource
import java.text.SimpleDateFormat
import java.util.*

// ─── Palette ──────────────────────────────────────────────────────────────────
private val BgDark       = FMColors.DarkBg
private val SurfaceDark  = FMColors.DarkSurface
private val Surface2Dark = FMColors.DarkSurface2
private val BorderDark   = FMColors.DarkOutline
private val MutedText    = FMColors.DarkMuted
private val GreenPrimary = FMColors.Primary
private val OnSurface    = FMColors.DarkOnSurface
private val AccentBlue   = FMColors.Info
private val AccentPurple = FMColors.BadgeEpic
private val AccentGold   = Color(0xFFFFC84A)
private val ErrorRed     = FMColors.Error

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BetsScreen(
    navController: NavController,
    viewModel: BetsViewModel = hiltViewModel()
) {
    val predictionsState    by viewModel.predictionsState.collectAsState()
    val myBetsState         by viewModel.myBetsState.collectAsState()
    val myCreatedState      by viewModel.myCreatedState.collectAsState()
    val betResult           by viewModel.betResult.collectAsState()
    val isPlacingBet        by viewModel.isPlacingBet.collectAsState()
    val isCreatingPrediction by viewModel.isCreatingPrediction.collectAsState()
    val isResolvingPrediction by viewModel.isResolvingPrediction.collectAsState()
    val coinBalance         by viewModel.coinBalance.collectAsState()

    var selectedTab          by remember { mutableStateOf("Active") }
    var selectedPrediction   by remember { mutableStateOf<PredictionDto?>(null) }
    var showCreateSheet      by remember { mutableStateOf(false) }
    var resolveTarget        by remember { mutableStateOf<PredictionDto?>(null) }

    val betSheetState    = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val createSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val resolveSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(betResult) {
        betResult?.let { result ->
            if (result.success) {
                // Dismiss sheets first — null-ing these causes Compose to remove the sheets
                selectedPrediction = null
                showCreateSheet = false
                resolveTarget = null
            }
            // Then show the snackbar (suspends until it appears; sheet is already gone)
            snackbarHostState.showSnackbar(result.message)
            viewModel.clearBetResult()
        }
    }

    // Sheets
    if (selectedPrediction != null) {
        PlaceBetSheet(
            prediction = selectedPrediction!!,
            isLoading  = isPlacingBet,
            coinBalance = coinBalance ?: 0,
            onDismiss  = { selectedPrediction = null },
            onConfirm  = { optionId, amount -> viewModel.placeBet(selectedPrediction!!.id, optionId, amount) },
            sheetState = betSheetState
        )
    }
    if (showCreateSheet) {
        CreatePredictionSheet(
            isLoading  = isCreatingPrediction,
            onDismiss  = { showCreateSheet = false },
            onCreate   = { title, desc, cat, opts, endsAt ->
                viewModel.createPrediction(title, desc, cat, opts, endsAt)
            },
            sheetState = createSheetState
        )
    }
    if (resolveTarget != null) {
        ResolvePredictionSheet(
            prediction = resolveTarget!!,
            isLoading  = isResolvingPrediction,
            onDismiss  = { resolveTarget = null },
            onResolve  = { winningOption ->
                viewModel.resolvePrediction(resolveTarget!!.id, winningOption)
            },
            sheetState = resolveSheetState
        )
    }

    Scaffold(
        containerColor = BgDark,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateSheet = true },
                containerColor = GreenPrimary,
                contentColor = Color.Black,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, "Create prediction")
            }
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = SurfaceDark,
                    contentColor = OnSurface,
                    actionColor = GreenPrimary,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // ── Header ──────────────────────────────────────────────────
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Spacer(modifier = Modifier.height(28.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Bets", color = OnSurface, fontWeight = FontWeight.Bold, fontSize = 28.sp, letterSpacing = (-0.5).sp)
                        Text("Place predictions, earn coins.", color = MutedText, fontSize = 13.sp)
                    }
                    // Coin balance chip
                    CoinBalanceChip(coinBalance)
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // ── Stats row ───────────────────────────────────────────────
            val myBets = (myBetsState as? Resource.Success)?.data ?: emptyList()
            val wins   = myBets.count { it.optionId.isNotBlank() }
            val winRate = if (myBets.isNotEmpty()) (wins * 100 / myBets.size) else 0

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                BetStatChip("${myBets.size}", "Total Bets",  modifier = Modifier.weight(1f))
                BetStatChip("$wins",          "Wins",        accent = GreenPrimary, modifier = Modifier.weight(1f))
                BetStatChip("$winRate%",       "Win Rate",    accent = AccentBlue,   modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(20.dp))

            // ── Tab row ─────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(12.dp)).background(SurfaceDark).padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf("Active", "My Bets", "Created").forEach { tab ->
                    val sel = selectedTab == tab
                    Box(
                        modifier = Modifier.weight(1f)
                            .clip(RoundedCornerShape(9.dp))
                            .background(if (sel) GreenPrimary.copy(0.15f) else Color.Transparent)
                            .clickable { selectedTab = tab }.padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(tab, color = if (sel) GreenPrimary else MutedText, fontSize = 13.sp, fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            when (selectedTab) {
                "Active"  -> ActivePredictionsTab(predictionsState, onBet = { selectedPrediction = it })
                "My Bets" -> MyBetsTab(myBetsState, predictionsState)
                "Created" -> MyCreatedTab(myCreatedState, onResolve = { resolveTarget = it })
            }
        }
    }
}

// ─── Coin balance chip ────────────────────────────────────────────────────────
@Composable
private fun CoinBalanceChip(balance: Int?) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.horizontalGradient(listOf(AccentGold.copy(0.18f), AccentGold.copy(0.08f)))
            )
            .border(1.dp, AccentGold.copy(0.3f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(painterResource(R.drawable.ic_points), null, tint = AccentGold, modifier = Modifier.size(16.dp))
        Text(
            text = if (balance != null) "$balance" else "—",
            color = AccentGold,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 15.sp
        )
    }
}

// ─── Active predictions tab ───────────────────────────────────────────────────
@Composable
private fun ActivePredictionsTab(
    state: Resource<List<PredictionDto>>,
    onBet: (PredictionDto) -> Unit
) {
    when (state) {
        is Resource.Loading -> CenteredLoader()
        is Resource.Error   -> CenteredError(state.message)
        is Resource.Success -> {
            val active = state.data?.filter { it.status == "ACTIVE" || it.status == null } ?: emptyList()
            if (active.isEmpty()) {
                CenteredEmpty(R.drawable.ic_bet, "No active predictions", "Check back soon.")
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(active, key = { it.id }) { prediction ->
                        PredictionBetCard(prediction, onBet = { onBet(prediction) })
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }
}

// ─── Prediction bet card ──────────────────────────────────────────────────────
@Composable
private fun PredictionBetCard(prediction: PredictionDto, onBet: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)).background(SurfaceDark)
            .border(1.dp, BorderDark, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        if (prediction.category != null) {
            Box(
                modifier = Modifier.clip(RoundedCornerShape(6.dp))
                    .background(AccentPurple.copy(0.12f))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) { Text(prediction.category, color = AccentPurple, fontSize = 10.sp, fontWeight = FontWeight.SemiBold) }
            Spacer(Modifier.height(8.dp))
        }

        Text(prediction.title, color = OnSurface, fontWeight = FontWeight.Bold, fontSize = 15.sp, lineHeight = 22.sp)
        if (prediction.description != null) {
            Text(prediction.description, color = MutedText, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
        }

        Spacer(Modifier.height(14.dp))

        // Options row with stake percentages
        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val totalStake = prediction.options.sumOf { it.totalStake }.let { if (it == 0) 1 else it }
            prediction.options.take(4).forEach { opt ->
                val pct = (opt.totalStake * 100 / totalStake)
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(8.dp))
                        .background(Surface2Dark)
                        .border(1.dp, BorderDark, RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(opt.optionText, color = OnSurface, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                        Text("$pct%", color = GreenPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(R.drawable.ic_home), null, tint = MutedText, modifier = Modifier.size(12.dp))
            Text("  Closes ${formatDeadline(prediction.endsAt)}", color = MutedText, fontSize = 11.sp, modifier = Modifier.weight(1f))
            Button(
                onClick = onBet,
                colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary, contentColor = Color.Black),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                modifier = Modifier.height(34.dp)
            ) { Text("Place Bet", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
        }
    }
}

// ─── My bets tab ──────────────────────────────────────────────────────────────
@Composable
private fun MyBetsTab(
    betsState: Resource<List<BetDto>>,
    predictionsState: Resource<List<PredictionDto>>
) {
    val predictions = (predictionsState as? Resource.Success)?.data ?: emptyList()
    when (betsState) {
        is Resource.Loading -> CenteredLoader()
        is Resource.Error   -> CenteredError(betsState.message)
        is Resource.Success -> {
            val bets = betsState.data ?: emptyList()
            if (bets.isEmpty()) {
                CenteredEmpty(R.drawable.ic_trophy, "No bets yet", "Head to Active to place your first bet.")
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(bets, key = { it.id ?: it.hashCode().toString() }) { bet ->
                        val predTitle = predictions.find { it.id == bet.predictionId }?.title ?: "Prediction"
                        MyBetRow(bet = bet, predictionTitle = predTitle)
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }
}

@Composable
private fun MyBetRow(bet: BetDto, predictionTitle: String) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(12.dp)).background(SurfaceDark)
            .border(1.dp, BorderDark, RoundedCornerShape(12.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(GreenPrimary.copy(0.12f)),
            contentAlignment = Alignment.Center
        ) { Icon(painterResource(R.drawable.ic_bet), null, tint = GreenPrimary, modifier = Modifier.size(20.dp)) }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(predictionTitle, color = OnSurface, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("Option ID: ${bet.optionId.take(8)}…", color = MutedText, fontSize = 11.sp)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("${bet.amount} 🪙", color = AccentGold, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(AccentBlue.copy(0.15f)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                Text("BET", color = AccentBlue, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp)
            }
        }
    }
}

// ─── My created tab ───────────────────────────────────────────────────────────
@Composable
private fun MyCreatedTab(
    state: Resource<List<PredictionDto>>,
    onResolve: (PredictionDto) -> Unit
) {
    when (state) {
        is Resource.Loading -> CenteredLoader()
        is Resource.Error   -> CenteredError(state.message)
        is Resource.Success -> {
            val preds = state.data ?: emptyList()
            if (preds.isEmpty()) {
                CenteredEmpty(R.drawable.ic_bet, "No predictions created", "Tap + to create your first prediction.")
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(preds, key = { it.id }) { pred ->
                        CreatedPredictionCard(pred, onResolve = { onResolve(pred) })
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }
}

@Composable
private fun CreatedPredictionCard(prediction: PredictionDto, onResolve: () -> Unit) {
    val isActive   = prediction.status == "ACTIVE" || prediction.status == null
    val statusColor = when (prediction.status) {
        "RESOLVED"  -> GreenPrimary
        "CANCELLED" -> ErrorRed
        else         -> AccentBlue
    }

    Column(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)).background(SurfaceDark)
            .border(1.dp, BorderDark, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(prediction.title, color = OnSurface, fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.weight(1f), lineHeight = 22.sp)
            Box(
                modifier = Modifier.clip(RoundedCornerShape(6.dp))
                    .background(statusColor.copy(0.12f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(prediction.status ?: "ACTIVE", color = statusColor, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp)
            }
        }

        Spacer(Modifier.height(10.dp))

        // Options summary
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            prediction.options.forEach { opt ->
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(6.dp))
                        .background(Surface2Dark)
                        .border(1.dp, BorderDark, RoundedCornerShape(6.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(opt.optionText, color = MutedText, fontSize = 11.sp, maxLines = 1)
                        Text("${opt.totalStake} 🪙", color = AccentGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        val totalPool = prediction.options.sumOf { it.totalStake }
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Pool: $totalPool 🪙", color = MutedText, fontSize = 12.sp, modifier = Modifier.weight(1f))
            if (isActive) {
                Button(
                    onClick = onResolve,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGold, contentColor = Color.Black),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    modifier = Modifier.height(34.dp)
                ) { Text("Resolve 🏆", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
            } else {
                prediction.options.find { it.id == prediction.winningOptionId }?.let { winner ->
                    Text("Winner: ${winner.optionText}", color = GreenPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

// ─── Place bet bottom sheet ───────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaceBetSheet(
    prediction: PredictionDto,
    isLoading: Boolean,
    coinBalance: Int,
    onDismiss: () -> Unit,
    onConfirm: (optionId: String, amount: Int) -> Unit,
    sheetState: SheetState
) {
    var selectedOptionId by remember { mutableStateOf("") }
    var stakeText       by remember { mutableStateOf(TextFieldValue("50")) }
    val stake = stakeText.text.toIntOrNull() ?: 0
    val insufficient = stake > coinBalance

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SurfaceDark,
        dragHandle = {
            Box(modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)) {
                Box(modifier = Modifier.width(36.dp).height(4.dp).background(BorderDark, RoundedCornerShape(2.dp)))
            }
        }
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 48.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Place Bet", color = OnSurface, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Text(prediction.title, color = MutedText, fontSize = 13.sp, maxLines = 2, lineHeight = 18.sp)
                }
                CoinBalanceChip(coinBalance)
            }

            Spacer(Modifier.height(20.dp))
            Text("CHOOSE OUTCOME", color = MutedText, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.8.sp)
            Spacer(Modifier.height(10.dp))

            prediction.options.forEach { opt ->
                val sel = selectedOptionId == opt.id
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (sel) GreenPrimary.copy(0.12f) else Surface2Dark)
                        .border(if (sel) 1.dp else 0.dp, if (sel) GreenPrimary.copy(0.4f) else Color.Transparent, RoundedCornerShape(12.dp))
                        .clickable { selectedOptionId = opt.id }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = sel,
                        onClick = { selectedOptionId = opt.id },
                        colors = RadioButtonDefaults.colors(selectedColor = GreenPrimary, unselectedColor = MutedText)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(opt.optionText, color = if (sel) OnSurface else MutedText, fontSize = 14.sp, fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal, modifier = Modifier.weight(1f))
                    Text("${opt.totalStake} 🪙", color = MutedText, fontSize = 11.sp)
                }
                Spacer(Modifier.height(8.dp))
            }

            Spacer(Modifier.height(12.dp))
            Text("STAKE (COINS)", color = MutedText, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.8.sp)
            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                    .background(Surface2Dark)
                    .border(1.dp, if (insufficient) ErrorRed else BorderDark, RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(painterResource(R.drawable.ic_points), null, tint = if (insufficient) ErrorRed else AccentGold, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                BasicTextField(
                    value = stakeText,
                    onValueChange = { stakeText = it },
                    textStyle = LocalTextStyle.current.copy(color = if (insufficient) ErrorRed else OnSurface, fontSize = 16.sp, fontWeight = FontWeight.Bold),
                    cursorBrush = SolidColor(GreenPrimary),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    decorationBox = { inner -> if (stakeText.text.isEmpty()) Text("0", color = MutedText, fontSize = 16.sp); inner() }
                )
                listOf(25, 50, 100).forEach { amt ->
                    TextButton(onClick = { stakeText = TextFieldValue(amt.toString()) }, contentPadding = PaddingValues(4.dp)) {
                        Text("+$amt", color = GreenPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            if (insufficient) {
                Text("Insufficient coins (balance: $coinBalance)", color = ErrorRed, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp, start = 4.dp))
            }

            Spacer(Modifier.height(24.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, BorderDark),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MutedText)
                ) { Text("Cancel") }

                Button(
                    onClick = {
                        if (selectedOptionId.isNotBlank() && stake > 0 && !insufficient) {
                            onConfirm(selectedOptionId, stake)
                        }
                    },
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary, contentColor = Color.Black),
                    enabled = selectedOptionId.isNotBlank() && stake > 0 && !insufficient && !isLoading
                ) {
                    if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.Black, strokeWidth = 2.dp)
                    else Text("Confirm Bet", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ─── Create prediction bottom sheet ──────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreatePredictionSheet(
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onCreate: (title: String, desc: String?, cat: String?, options: List<String>, endsAt: String) -> Unit,
    sheetState: SheetState
) {
    var title       by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category    by remember { mutableStateOf("") }
    var option1     by remember { mutableStateOf("") }
    var option2     by remember { mutableStateOf("") }
    var option3     by remember { mutableStateOf("") }
    var option4     by remember { mutableStateOf("") }
    // Simple deadline: days from today
    var daysAhead   by remember { mutableIntStateOf(7) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SurfaceDark,
        dragHandle = {
            Box(modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)) {
                Box(modifier = Modifier.width(36.dp).height(4.dp).background(BorderDark, RoundedCornerShape(2.dp)))
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 48.dp)
        ) {
            Text("Create Prediction", color = OnSurface, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Text("Others will bet coins on your prediction.", color = MutedText, fontSize = 13.sp)
            Spacer(Modifier.height(20.dp))

            SheetTextField(value = title, onValueChange = { title = it }, placeholder = "Prediction title (required)", label = "TITLE")
            Spacer(Modifier.height(12.dp))
            SheetTextField(value = description, onValueChange = { description = it }, placeholder = "Optional description", label = "DESCRIPTION")
            Spacer(Modifier.height(12.dp))
            SheetTextField(value = category, onValueChange = { category = it }, placeholder = "e.g. Sports, Finance, Tech", label = "CATEGORY (OPTIONAL)")

            Spacer(Modifier.height(20.dp))
            Text("OPTIONS (minimum 2)", color = MutedText, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.8.sp)
            Spacer(Modifier.height(8.dp))
            SheetTextField(value = option1, onValueChange = { option1 = it }, placeholder = "Option 1 (required)")
            Spacer(Modifier.height(8.dp))
            SheetTextField(value = option2, onValueChange = { option2 = it }, placeholder = "Option 2 (required)")
            Spacer(Modifier.height(8.dp))
            SheetTextField(value = option3, onValueChange = { option3 = it }, placeholder = "Option 3 (optional)")
            Spacer(Modifier.height(8.dp))
            SheetTextField(value = option4, onValueChange = { option4 = it }, placeholder = "Option 4 (optional)")

            Spacer(Modifier.height(20.dp))
            Text("CLOSES IN", color = MutedText, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.8.sp)
            Spacer(Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(1, 3, 7, 14, 30).forEach { days ->
                    val sel = daysAhead == days
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (sel) GreenPrimary.copy(0.15f) else Surface2Dark)
                            .border(1.dp, if (sel) GreenPrimary.copy(0.4f) else BorderDark, RoundedCornerShape(8.dp))
                            .clickable { daysAhead = days }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text("${days}d", color = if (sel) GreenPrimary else MutedText, fontSize = 13.sp, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            val validOptions = listOf(option1, option2, option3, option4).filter { it.isNotBlank() }
            val canCreate = title.isNotBlank() && validOptions.size >= 2

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, BorderDark),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MutedText)
                ) { Text("Cancel") }

                Button(
                    onClick = {
                        if (canCreate) {
                            val endsAt = buildEndsAt(daysAhead)
                            onCreate(
                                title,
                                description.ifBlank { null },
                                category.ifBlank { null },
                                validOptions,
                                endsAt
                            )
                        }
                    },
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary, contentColor = Color.Black),
                    enabled = canCreate && !isLoading
                ) {
                    if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.Black, strokeWidth = 2.dp)
                    else Text("Create", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ─── Resolve prediction sheet ─────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ResolvePredictionSheet(
    prediction: PredictionDto,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onResolve: (winningOptionId: String) -> Unit,
    sheetState: SheetState
) {
    var selectedOptionId   by remember { mutableStateOf("") }
    var selectedOptionText by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SurfaceDark,
        dragHandle = {
            Box(modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)) {
                Box(modifier = Modifier.width(36.dp).height(4.dp).background(BorderDark, RoundedCornerShape(2.dp)))
            }
        }
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 48.dp)) {
            Text("Resolve Prediction 🏆", color = OnSurface, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Spacer(Modifier.height(4.dp))
            Text(prediction.title, color = MutedText, fontSize = 13.sp, maxLines = 2)
            Spacer(Modifier.height(6.dp))

            val totalPool = prediction.options.sumOf { it.totalStake }
            Text("Total pool: $totalPool 🪙 — winners share proportionally", color = AccentGold, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)

            Spacer(Modifier.height(20.dp))
            Text("SELECT WINNING OPTION", color = MutedText, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.8.sp)
            Spacer(Modifier.height(10.dp))

            prediction.options.forEach { opt ->
                val sel = selectedOptionId == opt.id
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (sel) AccentGold.copy(0.12f) else Surface2Dark)
                        .border(if (sel) 1.dp else 0.dp, if (sel) AccentGold.copy(0.5f) else Color.Transparent, RoundedCornerShape(12.dp))
                        .clickable { selectedOptionId = opt.id; selectedOptionText = opt.optionText }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = sel,
                        onClick = { selectedOptionId = opt.id; selectedOptionText = opt.optionText },
                        colors = RadioButtonDefaults.colors(selectedColor = AccentGold, unselectedColor = MutedText)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(opt.optionText, color = if (sel) OnSurface else MutedText, fontSize = 14.sp, fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal, modifier = Modifier.weight(1f))
                    Text("${opt.totalStake} 🪙", color = AccentGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(8.dp))
            }

            Spacer(Modifier.height(24.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, BorderDark),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MutedText)
                ) { Text("Cancel") }

                Button(
                    onClick = { if (selectedOptionId.isNotBlank()) onResolve(selectedOptionId) },
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGold, contentColor = Color.Black),
                    enabled = selectedOptionId.isNotBlank() && !isLoading
                ) {
                    if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.Black, strokeWidth = 2.dp)
                    else Text("Confirm Winner", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ─── Shared composable helpers ────────────────────────────────────────────────
@Composable
private fun SheetTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    label: String? = null
) {
    if (label != null) {
        Text(label, color = MutedText, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.8.sp)
        Spacer(Modifier.height(6.dp))
    }
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = LocalTextStyle.current.copy(color = OnSurface, fontSize = 14.sp),
        cursorBrush = SolidColor(GreenPrimary),
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Surface2Dark)
            .border(1.dp, BorderDark, RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        decorationBox = { inner ->
            if (value.isEmpty()) Text(placeholder, color = MutedText, fontSize = 14.sp)
            inner()
        }
    )
}

@Composable
private fun CenteredLoader() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = GreenPrimary, strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
    }
}

@Composable
private fun CenteredError(message: String?) {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(message ?: "Error", color = ErrorRed, fontSize = 14.sp)
    }
}

@Composable
private fun CenteredEmpty(iconRes: Int, title: String, subtitle: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(painterResource(iconRes), null, tint = MutedText, modifier = Modifier.size(40.dp))
            Spacer(Modifier.height(12.dp))
            Text(title, color = MutedText, fontSize = 14.sp)
            Text(subtitle, color = MutedText.copy(0.6f), fontSize = 12.sp)
        }
    }
}

// ─── Stat chip ────────────────────────────────────────────────────────────────
@Composable
private fun BetStatChip(value: String, label: String, modifier: Modifier = Modifier, accent: Color = OnSurface) {
    Box(modifier = modifier.clip(RoundedCornerShape(12.dp)).background(SurfaceDark).border(1.dp, BorderDark, RoundedCornerShape(12.dp)).padding(12.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text(value, color = accent, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
            Text(label, color = MutedText, fontSize = 10.sp, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

private fun formatDeadline(raw: String): String {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
            .also { it.timeZone = TimeZone.getTimeZone("UTC") }
        val date = sdf.parse(raw) ?: return raw.take(10)
        SimpleDateFormat("MMM d", Locale.getDefault()).format(date)
    } catch (_: Exception) { raw.take(10) }
}

private fun buildEndsAt(daysAhead: Int): String {
    val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
    cal.add(Calendar.DAY_OF_YEAR, daysAhead)
    return SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
        .also { it.timeZone = TimeZone.getTimeZone("UTC") }
        .format(cal.time)
}
