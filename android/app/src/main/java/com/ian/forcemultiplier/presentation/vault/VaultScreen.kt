package com.ian.forcemultiplier.presentation.vault

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.ian.forcemultiplier.core.designsystem.FMCard
import com.ian.forcemultiplier.core.designsystem.FMChip

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultScreen(navController: NavController) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }
    val filters = listOf("All", "Ideas", "Meetings", "Learning", "Strategy")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Knowledge Vault",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Search Bar (Notion Style)
        TextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search notes, tags, ideas...") },
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp)),
            colors = TextFieldDefaults.textFieldColors(
                containerColor = MaterialTheme.colorScheme.surface,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent
            ),
            singleLine = true
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            filters.forEach { filter ->
                FMChip(
                    label = filter,
                    selected = selectedFilter == filter,
                    onClick = { selectedFilter = filter }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Notes List
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(notesData) { note ->
                NoteCard(note)
            }
        }
    }
}

@Composable
fun NoteCard(note: NoteItem) {
    FMCard(
        borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = note.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = note.date,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = note.preview,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                note.tags.forEach { tag ->
                    Box(
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "#$tag",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

data class NoteItem(
    val title: String,
    val preview: String,
    val date: String,
    val tags: List<String>
)

val notesData = listOf(
    NoteItem(
        "Product Strategy 2024",
        "Key focus areas for the upcoming year include AI integration and community-driven roadmaps...",
        "Oct 12",
        listOf("Strategy", "AI")
    ),
    NoteItem(
        "Meeting Notes: Sprint 45",
        "Discussed the new design system implementation and resolved the navigation bottleneck...",
        "Oct 10",
        listOf("Meetings", "Design")
    ),
    NoteItem(
        "Learning: Jetpack Compose",
        "Notes on Shared Element Transitions and advanced animation techniques in Compose...",
        "Oct 08",
        listOf("Learning", "Android")
    ),
    NoteItem(
        "Idea: Prediction Streak",
        "What if we rewarded users for 5 consecutive correct predictions with double points?",
        "Oct 05",
        listOf("Ideas", "Gamification")
    )
)
