package com.ian.forcemultiplier.presentation.vault

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.ian.forcemultiplier.R
import com.ian.forcemultiplier.data.remote.dto.NoteDto
import com.ian.forcemultiplier.presentation.vault.viewmodel.VaultViewModel
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

@Composable
fun VaultScreen(
    navController: NavController,
    viewModel: VaultViewModel = hiltViewModel()
) {
    val notesState by viewModel.notesState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var searchActive by remember { mutableStateOf(false) }
    var selectedTag by remember { mutableStateOf("All") }
    val focusRequester = remember { FocusRequester() }

    val allNotes = when (val s = notesState) {
        is Resource.Success -> s.data ?: emptyList()
        else -> emptyList()
    }

    // Derive dynamic tag chips from actual note tags
    val availableTags = remember(allNotes) {
        val tags = allNotes
            .mapNotNull { it.tags }
            .flatMap { it.split(",") }
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
        listOf("All") + tags
    }

    val displayedNotes = remember(allNotes, searchQuery, selectedTag) {
        var list = allNotes.filter { it.parentId == null }
        if (searchQuery.isNotBlank()) {
            list = list.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                        it.content?.contains(searchQuery, ignoreCase = true) == true ||
                        it.tags?.contains(searchQuery, ignoreCase = true) == true
            }
        }
        if (selectedTag != "All") {
            list = list.filter { it.tags?.contains(selectedTag, ignoreCase = true) == true }
        }
        list
    }

    val pinnedNotes = remember(allNotes) { allNotes.filter { it.isBookmarked } }

    val groupedNotes = remember(displayedNotes) { groupNotesByDate(displayedNotes) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            // ── Header ─────────────────────────────────────────────────
            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Notes",
                            color = OnSurface,
                            fontWeight = FontWeight.Bold,
                            fontSize = 28.sp,
                            letterSpacing = (-0.5).sp
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(
                                onClick = {
                                    searchActive = !searchActive
                                    if (!searchActive) searchQuery = ""
                                },
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (searchActive) GreenPrimary.copy(alpha = 0.15f)
                                        else SurfaceDark
                                    )
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_search),
                                    contentDescription = "Search",
                                    tint = if (searchActive) GreenPrimary else MutedText,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(
                                onClick = { navController.navigate("note_detail/new") },
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(GreenPrimary)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_add),
                                    contentDescription = "New note",
                                    tint = Color.Black,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // ── Search Bar ─────────────────────────────────────────────
            item {
                AnimatedVisibility(
                    visible = searchActive,
                    enter = expandVertically(tween(200)) + fadeIn(tween(200)),
                    exit = shrinkVertically(tween(200)) + fadeOut(tween(200))
                ) {
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 20.dp)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(SurfaceDark)
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(R.drawable.ic_search),
                                contentDescription = null,
                                tint = MutedText,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                modifier = Modifier
                                    .weight(1f)
                                    .focusRequester(focusRequester),
                                textStyle = LocalTextStyle.current.copy(
                                    color = OnSurface,
                                    fontSize = 14.sp
                                ),
                                cursorBrush = SolidColor(GreenPrimary),
                                singleLine = true,
                                decorationBox = { inner ->
                                    if (searchQuery.isEmpty()) {
                                        Text(
                                            "Search notes...",
                                            color = MutedText,
                                            fontSize = 14.sp
                                        )
                                    }
                                    inner()
                                }
                            )
                            if (searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { searchQuery = "" },
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Text("x", color = MutedText, fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }
                if (searchActive) Spacer(modifier = Modifier.height(12.dp))
            }

            // ── Tag Chips ──────────────────────────────────────────────
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(availableTags) { tag ->
                        val selected = selectedTag == tag
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    if (selected) GreenPrimary.copy(alpha = 0.15f)
                                    else SurfaceDark
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (selected) GreenPrimary.copy(alpha = 0.4f) else BorderDark,
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .clickable { selectedTag = tag }
                                .padding(horizontal = 14.dp, vertical = 7.dp)
                        ) {
                            Text(
                                text = tag,
                                color = if (selected) GreenPrimary else MutedText,
                                fontSize = 13.sp,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // ── Pinned ─────────────────────────────────────────────────
            if (pinnedNotes.isNotEmpty()) {
                item {
                    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                        SectionLabel("Pinned")
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(pinnedNotes) { note ->
                            PinnedNoteCard(note) {
                                navController.navigate("note_detail/${note.id}")
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            // ── Loading / Error / Empty ────────────────────────────────
            when (val state = notesState) {
                is Resource.Loading -> item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = GreenPrimary,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                is Resource.Error -> item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 32.dp, start = 20.dp, end = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = state.message ?: "Could not load notes",
                            color = Color(0xFFFF4757),
                            fontSize = 14.sp
                        )
                    }
                }

                is Resource.Success -> {
                    if (displayedNotes.isEmpty() && searchQuery.isBlank() && selectedTag == "All") {
                        item {
                            EmptyNotesState(
                                onCreateNote = { navController.navigate("note_detail/new") }
                            )
                        }
                    } else if (displayedNotes.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No notes match \"$searchQuery\"",
                                    color = MutedText,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    } else {
                        // Grouped note list
                        groupedNotes.forEach { (label, notes) ->
                            item {
                                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                                    SectionLabel(label)
                                    Spacer(modifier = Modifier.height(4.dp))
                                }
                            }
                            items(notes, key = { it.id ?: it.title }) { note ->
                                NoteListRow(note) {
                                    navController.navigate("note_detail/${note.id}")
                                }
                            }
                            item { Spacer(modifier = Modifier.height(16.dp)) }
                        }
                    }
                }
            }
        }
    }
}

// ─── Pinned card ──────────────────────────────────────────────────────────────
@Composable
private fun PinnedNoteCard(note: NoteDto, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .width(172.dp)
            .height(108.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceDark)
            .border(1.dp, BorderDark, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxSize()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(R.drawable.ic_pin),
                    contentDescription = null,
                    tint = GreenPrimary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = note.tags?.split(",")?.firstOrNull()?.trim() ?: "Note",
                    color = MutedText,
                    fontSize = 11.sp
                )
            }
            Column {
                Text(
                    text = note.title,
                    color = OnSurface,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (!note.content.isNullOrBlank()) {
                    Text(
                        text = note.content,
                        color = MutedText,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}

// ─── Note list row ────────────────────────────────────────────────────────────
@Composable
private fun NoteListRow(note: NoteDto, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Surface2Dark),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_note),
                contentDescription = null,
                tint = MutedText,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = note.title.ifBlank { "Untitled" },
                color = OnSurface,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!note.content.isNullOrBlank()) {
                Text(
                    text = note.content,
                    color = MutedText,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 1.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = note.createdAt?.let { formatRelativeDate(it) } ?: "",
            color = MutedText,
            fontSize = 11.sp
        )
    }
}

// ─── Section label ────────────────────────────────────────────────────────────
@Composable
private fun SectionLabel(label: String) {
    Text(
        text = label.uppercase(),
        color = MutedText,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.8.sp
    )
}

// ─── Empty state ──────────────────────────────────────────────────────────────
@Composable
private fun EmptyNotesState(onCreateNote: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 60.dp, start = 32.dp, end = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(SurfaceDark),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_note),
                contentDescription = null,
                tint = MutedText,
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No notes yet",
            color = OnSurface,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Capture thoughts, meeting notes, ideas — anything you want to remember.",
            color = MutedText,
            fontSize = 13.sp,
            lineHeight = 20.sp,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onCreateNote,
            colors = ButtonDefaults.buttonColors(
                containerColor = GreenPrimary,
                contentColor = Color.Black
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Create your first note", fontWeight = FontWeight.SemiBold)
        }
    }
}

// ─── Helpers ──────────────────────────────────────────────────────────────────
private fun groupNotesByDate(notes: List<NoteDto>): List<Pair<String, List<NoteDto>>> {
    val now = Calendar.getInstance()
    val today = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0)
    }
    val yesterday = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, -1)
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0)
    }
    val weekAgo = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -7) }

    val todayNotes    = mutableListOf<NoteDto>()
    val yestNotes     = mutableListOf<NoteDto>()
    val weekNotes     = mutableListOf<NoteDto>()
    val olderNotes    = mutableListOf<NoteDto>()

    notes.forEach { note ->
        val raw = note.createdAt ?: ""
        val cal = parseDateToCal(raw)
        when {
            cal != null && cal.after(today)      -> todayNotes.add(note)
            cal != null && cal.after(yesterday)  -> yestNotes.add(note)
            cal != null && cal.after(weekAgo)    -> weekNotes.add(note)
            else                                 -> olderNotes.add(note)
        }
    }

    val result = mutableListOf<Pair<String, List<NoteDto>>>()
    if (todayNotes.isNotEmpty())  result += "Today"     to todayNotes
    if (yestNotes.isNotEmpty())   result += "Yesterday" to yestNotes
    if (weekNotes.isNotEmpty())   result += "This week" to weekNotes
    if (olderNotes.isNotEmpty())  result += "Older"     to olderNotes
    return result
}

private fun parseDateToCal(raw: String): Calendar? {
    val fmts = listOf("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", "yyyy-MM-dd'T'HH:mm:ss'Z'", "yyyy-MM-dd")
    for (fmt in fmts) {
        try {
            val d = SimpleDateFormat(fmt, Locale.getDefault()).also { it.timeZone = TimeZone.getTimeZone("UTC") }.parse(raw)
            if (d != null) return Calendar.getInstance().apply { time = d }
        } catch (_: Exception) {}
    }
    return null
}

private fun formatRelativeDate(raw: String): String {
    val cal = parseDateToCal(raw) ?: return ""
    val today = Calendar.getInstance()
    return when {
        today.get(Calendar.DAY_OF_YEAR) == cal.get(Calendar.DAY_OF_YEAR) &&
                today.get(Calendar.YEAR) == cal.get(Calendar.YEAR) -> "Today"
        today.get(Calendar.DAY_OF_YEAR) - cal.get(Calendar.DAY_OF_YEAR) == 1 &&
                today.get(Calendar.YEAR) == cal.get(Calendar.YEAR) -> "Yesterday"
        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(cal.time)
    }
}
