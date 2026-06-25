package com.ian.forcemultiplier.presentation.bets

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import com.ian.forcemultiplier.data.remote.dto.BetDto
import com.ian.forcemultiplier.data.remote.dto.PredictionDto
import com.ian.forcemultiplier.presentation.bets.viewmodel.BetsViewModel
import com.ian.forcemultiplier.util.Resource
import java.text.SimpleDateFormat
import java.util.*

// ─── Palette ──────────────────────────────────────────────────────────────────
private val BgDark       = Color(0xFF0B0F14)
private val SurfaceDark  = Color(0xFF151B23)
private val Surface2Dark = Color(0xFF1C2128)
private val BorderDark   = Color(0xFF30363D)
private val MutedText    = Color(0xFF8B949E)
private val GreenPrimary = Color(0xFF2ED573)
private val OnSurface    = Color(0xFFE6EDF3)
private val AccentBlue   = Color(0xFF3DABF5)
private val AccentPurple = Color(0xFF9C59FF)
private val ErrorRed     = Color(0xFFFF4757)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BetsScreen(
    navController: NavController,
    viewModel: BetsViewModel = hiltViewModel()
) {
    val predictionsState by viewModel.predictionsState.collectAsState()
    val myBetsState      by viewModel.myBetsState.collectAsState()
    val betResult        by viewModel.betResult.collectAsState()
    val isPlacingBet     by viewModel.isPlacingBet.collectAsState()

    var selectedTab     by remember { mutableStateOf("Active") }
    var selectedPrediction by remember { mutableStateOf<PredictionDto?>(null) }
    val sheetState      = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Show result snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(betResult) {
        betResult?.let {
            snackbarHostState.showSnackbar(it.message)
            viewModel.clearBetResult()
            selectedPrediction = null
        }
    }

    // Bet placement bottom sheet
    if (selectedPrediction != null) {
        PlaceBetSheet(
            prediction = selectedPrediction!!,
            isLoading = isPlacingBet,
            onDismiss = { selectedPrediction = null },
            onConfirm = { outcome, stake ->
                viewModel.placeBet(selectedPrediction!!.id, outcome, stake)
            },
            sheetState = sheetState
        )
    }

    Scaffold(
        containerColor = BgDark,
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
                Text("Bets", color = OnSurface, fontWeight = FontWeight.Bold, fontSize = 28.sp, letterSpacing = (-0.5).sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Place predictions, earn points.", color = MutedText, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(20.dp))
            }

            // ── Stats row ───────────────────────────────────────────────
            val myBets = (myBetsState as? Resource.Success)?.data ?: emptyList()
            val wins   = myBets.count { it.settled && it.chosenOutcome.isNotBlank() }
            val winRate = if (myBets.isNotEmpty()) (wins * 100 / myBets.size) else 0

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                BetStatChip("${myBets.size}", "Total Bets", modifier = Modifier.weight(1f))
                BetStatChip("$wins", "Wins", accent = GreenPrimary, modifier = Modifier.weight(1f))
                BetStatChip("$winRate%", "Win Rate", accent = AccentBlue, modifier = Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(20.dp))

            // ── Tab row ─────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(12.dp)).background(SurfaceDark).padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf("Active", "My Bets").forEach { tab ->
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

            // ── Content ─────────────────────────────────────────────────
            when (selectedTab) {
                "Active" -> ActivePredictionsTab(predictionsState, onBet = { selectedPrediction = it })
                "My Bets" -> MyBetsTab(myBetsState, predictionsState)
            }
        }
    }
}

// ─── Active predictions ───────────────────────────────────────────────────────
@Composable
private fun ActivePredictionsTab(
    state: Resource<List<PredictionDto>>,
    onBet: (PredictionDto) -> Unit
) {
    when (state) {
        is Resource.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = GreenPrimary, strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
        }
        is Resource.Error -> Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
            Text(state.message ?: "Error", color = ErrorRed, fontSize = 14.sp)
        }
        is Resource.Success -> {
            val active = state.data?.filter { it.status == "ACTIVE" || it.status == null } ?: emptyList()
            if (active.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(painterResource(R.drawable.ic_bet), null, tint = MutedText, modifier = Modifier.size(40.dp))
                        Spacer(Modifier.height(12.dp))
                        Text("No active predictions", color = MutedText, fontSize = 14.sp)
                        Text("Check back soon.", color = MutedText.copy(0.6f), fontSize = 12.sp)
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(active) { prediction ->
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
        // Category chip
        if (prediction.category != null) {
            Box(
                modifier = Modifier.clip(RoundedCornerShape(6.dp))
                    .background(AccentPurple.copy(0.12f))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) { Text(prediction.category, color = AccentPurple, fontSize = 10.sp, fontWeight = FontWeight.SemiBold) }
            Spacer(Modifier.height(8.dp))
        }

        // Title
        Text(prediction.title, color = OnSurface, fontWeight = FontWeight.Bold, fontSize = 15.sp, lineHeight = 22.sp)
        if (prediction.description != null) {
            Text(prediction.description, color = MutedText, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
        }

        Spacer(Modifier.height(14.dp))

        // Options row
        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            prediction.options.take(4).forEach { opt ->
                val totalStake = prediction.options.sumOf { it.totalStake }.let { if (it == 0) 1 else it }
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

        // Footer
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
        is Resource.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = GreenPrimary, strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
        }
        is Resource.Error -> Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
            Text(betsState.message ?: "Error", color = ErrorRed, fontSize = 14.sp)
        }
        is Resource.Success -> {
            val bets = betsState.data ?: emptyList()
            if (bets.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(painterResource(R.drawable.ic_trophy), null, tint = MutedText, modifier = Modifier.size(40.dp))
                        Spacer(Modifier.height(12.dp))
                        Text("No bets yet", color = MutedText, fontSize = 14.sp)
                        Text("Head to Active to place your first bet.", color = MutedText.copy(0.6f), fontSize = 12.sp)
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(bets) { bet ->
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
    val statusColor = when {
        !bet.settled -> AccentBlue
        bet.chosenOutcome.isNotBlank() -> GreenPrimary
        else -> ErrorRed
    }
    val statusLabel = when {
        !bet.settled -> "PENDING"
        else -> "SETTLED"
    }

    Row(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(12.dp)).background(SurfaceDark)
            .border(1.dp, BorderDark, RoundedCornerShape(12.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp))
                .background(GreenPrimary.copy(0.12f)),
            contentAlignment = Alignment.Center
        ) { Icon(painterResource(R.drawable.ic_bet), null, tint = GreenPrimary, modifier = Modifier.size(20.dp)) }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(predictionTitle, color = OnSurface, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(bet.chosenOutcome, color = MutedText, fontSize = 11.sp)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("${bet.stake} pts", color = OnSurface, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(statusColor.copy(0.15f)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                Text(statusLabel, color = statusColor, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp)
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
    onDismiss: () -> Unit,
    onConfirm: (outcome: String, stake: Int) -> Unit,
    sheetState: SheetState
) {
    var selectedOutcome by remember { mutableStateOf("") }
    var stakeText by remember { mutableStateOf(TextFieldValue("50")) }

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
            Text("Place Bet", color = OnSurface, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Spacer(Modifier.height(4.dp))
            Text(prediction.title, color = MutedText, fontSize = 13.sp, maxLines = 2, lineHeight = 18.sp)

            Spacer(Modifier.height(20.dp))
            Text("CHOOSE OUTCOME", color = MutedText, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.8.sp)
            Spacer(Modifier.height(10.dp))

            prediction.options.forEach { opt ->
                val sel = selectedOutcome == opt.optionText
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (sel) GreenPrimary.copy(0.12f) else Surface2Dark)
                        .border(if (sel) 1.dp else 0.dp, if (sel) GreenPrimary.copy(0.4f) else Color.Transparent, RoundedCornerShape(12.dp))
                        .clickable { selectedOutcome = opt.optionText }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = sel,
                        onClick = { selectedOutcome = opt.optionText },
                        colors = RadioButtonDefaults.colors(selectedColor = GreenPrimary, unselectedColor = MutedText)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(opt.optionText, color = if (sel) OnSurface else MutedText, fontSize = 14.sp, fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal, modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
            }

            Spacer(Modifier.height(12.dp))
            Text("STAKE (PTS)", color = MutedText, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.8.sp)
            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                    .background(Surface2Dark).border(1.dp, BorderDark, RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(painterResource(R.drawable.ic_points), null, tint = GreenPrimary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                BasicTextField(
                    value = stakeText,
                    onValueChange = { stakeText = it },
                    textStyle = LocalTextStyle.current.copy(color = OnSurface, fontSize = 16.sp, fontWeight = FontWeight.Bold),
                    cursorBrush = SolidColor(GreenPrimary),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    decorationBox = { inner -> if (stakeText.text.isEmpty()) Text("0", color = MutedText, fontSize = 16.sp); inner() }
                )
                // Quick stake buttons
                listOf(25, 50, 100).forEach { amt ->
                    TextButton(onClick = { stakeText = TextFieldValue(amt.toString()) }, contentPadding = PaddingValues(4.dp)) {
                        Text("+$amt", color = GreenPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
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
                        val stake = stakeText.text.toIntOrNull() ?: 0
                        if (selectedOutcome.isNotBlank() && stake > 0) onConfirm(selectedOutcome, stake)
                    },
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary, contentColor = Color.Black),
                    enabled = selectedOutcome.isNotBlank() && (stakeText.text.toIntOrNull() ?: 0) > 0 && !isLoading
                ) {
                    if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.Black, strokeWidth = 2.dp)
                    else Text("Confirm Bet", fontWeight = FontWeight.Bold)
                }
            }
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
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).also { it.timeZone = TimeZone.getTimeZone("UTC") }
        val date = sdf.parse(raw) ?: return raw.take(10)
        SimpleDateFormat("MMM d", Locale.getDefault()).format(date)
    } catch (_: Exception) { raw.take(10) }
}
