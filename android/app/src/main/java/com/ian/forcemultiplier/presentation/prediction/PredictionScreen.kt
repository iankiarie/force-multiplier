package com.ian.forcemultiplier.presentation.prediction

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.ian.forcemultiplier.core.designsystem.FMButton
import com.ian.forcemultiplier.core.designsystem.FMCard
import com.ian.forcemultiplier.core.designsystem.FMChip
import com.ian.forcemultiplier.data.remote.dto.PredictionDto
import com.ian.forcemultiplier.presentation.prediction.viewmodel.PredictionViewModel
import com.ian.forcemultiplier.util.Resource
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun PredictionScreen(
    navController: NavController,
    viewModel: PredictionViewModel = hiltViewModel()
) {
    var selectedTab by remember { mutableStateOf("Active") }
    val tabs = listOf("Active", "Resolved", "My Bets")
    val predictionsState by viewModel.predictionsState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Predictions",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            tabs.forEach { tab ->
                FMChip(
                    label = tab,
                    selected = selectedTab == tab,
                    onClick = { selectedTab = tab }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        when (val state = predictionsState) {
            is Resource.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is Resource.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = state.message ?: "Unknown error", color = MaterialTheme.colorScheme.error)
                }
            }
            is Resource.Success -> {
                val list = state.data ?: emptyList()
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(list) { prediction ->
                        PredictionCard(prediction)
                    }
                }
            }
        }
    }
}

@Composable
fun PredictionCard(prediction: PredictionDto) {
    var expanded by remember { mutableStateOf(false) }
    var selectedOptionId by remember { mutableStateOf<String?>(null) }

    FMCard(
        onClick = { expanded = !expanded },
        borderColor = if (expanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .animateContentSize()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = prediction.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = formatDeadline(prediction.endsAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            
            prediction.description?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            AnimatedVisibility(visible = expanded) {
                Column {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Choose Outcome",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        prediction.options.forEach { option ->
                            FMButton(
                                onClick = { selectedOptionId = option.id },
                                modifier = Modifier.weight(1f),
                                containerColor = if (selectedOptionId == option.id) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (selectedOptionId == option.id) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            ) {
                                Text(option.optionText)
                            }
                        }
                    }
                    
                    if (selectedOptionId != null) {
                        Spacer(modifier = Modifier.height(24.dp))
                        FMButton(
                            onClick = { /* TODO: Confirm bet via API */ },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Confirm Prediction")
                        }
                    }
                }
            }
        }
    }
}

fun formatDeadline(deadline: String): String {
    return try {
        // Supabase returns ISO strings. Simple substring for display
        deadline.replace("T", " ").substringBefore(".")
    } catch (e: Exception) {
        deadline
    }
}
