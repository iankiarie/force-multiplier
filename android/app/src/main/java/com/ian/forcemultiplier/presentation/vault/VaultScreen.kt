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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
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

// ─── Note templates ──────────────────────────────────────────────────────────
data class NoteTemplate(
    val name: String,
    val description: String,
    val titlePrefix: String,
    val body: String
)

private val noteTemplates = listOf(
    NoteTemplate(
        "Meeting Notes",
        "Agenda, decisions, action items",
        "Meeting: ",
        """## Attendees
- 

## Agenda
1. 
2. 

## Key Decisions


## Action Items
- [ ] 
- [ ] 

## Notes

"""
    ),
    NoteTemplate(
        "Daily Log",
        "What you worked on today",
        "Daily Log - ",
        """## Today's focus


## What I did


## Blockers


## Tomorrow

"""
    ),
    NoteTemplate(
        "Ideas",
        "Brain dump for new ideas",
        "Ideas: ",
        """## The idea


## Why it matters


## How it could work


## Next steps

"""
    ),
    NoteTemplate(
        "Project Plan",
        "Goals, tasks, timeline",
        "Plan: ",
        """## Objective


## Deliverables
- 
- 

## Tasks
- [ ] 
- [ ] 
- [ ] 

## Timeline


## Notes

"""
    ),
    NoteTemplate(
        "Weekly Review",
        "Wins, improvements, goals",
        "Week Review - ",
        """## Wins this week


## What could be better


## Learnings


## Goals for next week
1. 
2. 
3. 

"""
    ),
    NoteTemplate(
        "Lecture Notes",
        "Key points and takeaways",
        "Lecture: ",
        """## Topic


## Key Concepts


## Important Details


## Questions


## Summary

"""
    ),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultScreen(
    navController: NavController,
    viewModel: VaultViewModel = hiltViewModel()
) {
    val notesState     by viewModel.notesState.collectAsState()
    var searchQuery    by remember { mutableStateOf("") }
    var searchActive   by remember { mutableStateOf(false) }
    var selectedTag    by remember { mutableStateOf("All") }
    var showTemplates  by remember { mutableStateOf(false) }
    var showCreateMenu by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val sheetState     = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // ── Lifecycle: reload notes every time screen resumes ────────────────────
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.getNotes()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // ── Derived data ─────────────────────────────────────────────────────────
    val allNotes = when (val s = notesState) {
        is Resource.Success -> s.data ?: emptyList()
        else -> emptyList()
    }
    val availableTags = remember(allNotes) {
        val tags = allNotes.mapNotNull { it.tags }
            .flatMap { it.split(",") }
            .map { it.trim() }.filter { it.isNotBlank() }.distinct()
        listOf("All") + tags
    }
    val displayedNotes = remember(allNotes, searchQuery, selectedTag) {
        var list = allNotes.filter { it.parentId == null }
        if (searchQuery.isNotBlank()) list = list.filter {
            it.title.contains(searchQuery, true) ||
                    it.content?.contains(searchQuery, true) == true ||
                    it.tags?.contains(searchQuery, true) == true
        }
        if (selectedTag != "All") list = list.filter {
            it.tags?.contains(selectedTag, true) == true
        }
        list
    }
    val pinnedNotes    = remember(allNotes) { allNotes.filter { it.isBookmarked } }
    val groupedNotes   = remember(displayedNotes) { groupNotesByDate(displayedNotes) }

    // ── Templates bottom sheet ────────────────────────────────────────────────
    if (showTemplates) {
        ModalBottomSheet(
            onDismissRequest = { showTemplates = false },
            sheetState = sheetState,
            containerColor = SurfaceDark,
            dragHandle = { Box(modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)) {
                Box(modifier = Modifier.width(36.dp).height(4.dp)
                    .background(BorderDark, RoundedCornerShape(2.dp)))
            }}
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 48.dp)) {
                Text(
                    "Choose a template",
                    color = OnSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
                noteTemplates.forEach { tpl ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                showTemplates = false
                                val today = SimpleDateFormat("MMM d", Locale.getDefault()).format(Date())
                                navController.navigate(
                                    "note_detail/new?template=${
                                        java.net.URLEncoder.encode(tpl.titlePrefix + today + "|" + tpl.body, "UTF-8")
                                    }"
                                )
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(44.dp).clip(RoundedCornerShape(10.dp))
                                .background(GreenPrimary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_note),
                                contentDescription = null,
                                tint = GreenPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(tpl.name, color = OnSurface, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text(tpl.description, color = MutedText, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(BgDark)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            // ── Header ──────────────────────────────────────────────────
            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Notes", color = OnSurface, fontWeight = FontWeight.Bold, fontSize = 28.sp, letterSpacing = (-0.5).sp)
                            Text(
                                text = "${allNotes.size} page${if (allNotes.size != 1) "s" else ""}",
                                color = MutedText, fontSize = 13.sp
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            IconButton(
                                onClick = { searchActive = !searchActive; if (!searchActive) searchQuery = "" },
                                modifier = Modifier.size(38.dp).clip(CircleShape)
                                    .background(if (searchActive) GreenPrimary.copy(0.15f) else SurfaceDark)
                            ) {
                                Icon(painterResource(R.drawable.ic_search), null,
                                    tint = if (searchActive) GreenPrimary else MutedText,
                                    modifier = Modifier.size(18.dp))
                            }
                            IconButton(
                                onClick = { showCreateMenu = !showCreateMenu },
                                modifier = Modifier.size(38.dp).clip(CircleShape).background(GreenPrimary)
                            ) {
                                Icon(painterResource(R.drawable.ic_add), null, tint = Color.Black, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                    // Create menu popup
                    AnimatedVisibility(visible = showCreateMenu, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                        Column(modifier = Modifier.padding(top = 12.dp).fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp)).background(Surface2Dark)
                            .border(1.dp, BorderDark, RoundedCornerShape(14.dp))
                        ) {
                            CreateMenuRow("Blank page", "Start with nothing", R.drawable.ic_add) {
                                showCreateMenu = false
                                navController.navigate("note_detail/new")
                            }
                            HorizontalDivider(color = BorderDark, modifier = Modifier.padding(horizontal = 16.dp))
                            CreateMenuRow("From template", "Meeting notes, daily log...", R.drawable.ic_note) {
                                showCreateMenu = false
                                showTemplates = true
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // ── Search ──────────────────────────────────────────────────
            item {
                AnimatedVisibility(visible = searchActive, enter = expandVertically(tween(200)) + fadeIn(), exit = shrinkVertically(tween(200)) + fadeOut()) {
                    Box(modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp)).background(SurfaceDark)
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(painterResource(R.drawable.ic_search), null, tint = MutedText, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            BasicTextField(
                                value = searchQuery, onValueChange = { searchQuery = it },
                                modifier = Modifier.weight(1f).focusRequester(focusRequester),
                                textStyle = LocalTextStyle.current.copy(color = OnSurface, fontSize = 14.sp),
                                cursorBrush = SolidColor(GreenPrimary), singleLine = true,
                                decorationBox = { inner ->
                                    if (searchQuery.isEmpty()) Text("Search notes...", color = MutedText, fontSize = 14.sp)
                                    inner()
                                }
                            )
                        }
                    }
                }
                if (searchActive) Spacer(modifier = Modifier.height(12.dp))
            }

            // ── Tag chips ────────────────────────────────────────────────
            item {
                LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(availableTags) { tag ->
                        val sel = selectedTag == tag
                        Box(
                            modifier = Modifier.clip(RoundedCornerShape(20.dp))
                                .background(if (sel) GreenPrimary.copy(0.15f) else SurfaceDark)
                                .border(1.dp, if (sel) GreenPrimary.copy(0.4f) else BorderDark, RoundedCornerShape(20.dp))
                                .clickable { selectedTag = tag }
                                .padding(horizontal = 14.dp, vertical = 7.dp)
                        ) { Text(tag, color = if (sel) GreenPrimary else MutedText, fontSize = 13.sp, fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal) }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // ── Pinned ───────────────────────────────────────────────────
            if (pinnedNotes.isNotEmpty()) {
                item {
                    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                        SectionLabel("Pinned"); Spacer(modifier = Modifier.height(10.dp))
                    }
                }
                item {
                    LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(pinnedNotes) { note ->
                            PinnedNoteCard(note) { navController.navigate("note_detail/${note.id}") }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            // ── Note list by state ────────────────────────────────────────
            when (val state = notesState) {
                is Resource.Loading -> item {
                    Box(modifier = Modifier.fillMaxWidth().padding(top = 40.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = GreenPrimary, strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
                    }
                }
                is Resource.Error -> item {
                    Box(modifier = Modifier.fillMaxWidth().padding(top = 32.dp, start = 20.dp, end = 20.dp), contentAlignment = Alignment.Center) {
                        Text(state.message ?: "Could not load notes", color = Color(0xFFFF4757), fontSize = 14.sp)
                    }
                }
                is Resource.Success -> {
                    if (displayedNotes.isEmpty() && searchQuery.isBlank() && selectedTag == "All") {
                        item { EmptyNotesState { navController.navigate("note_detail/new") } }
                    } else if (displayedNotes.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(top = 40.dp), contentAlignment = Alignment.Center) {
                                Text("No notes match \"$searchQuery\"", color = MutedText, fontSize = 14.sp)
                            }
                        }
                    } else {
                        groupedNotes.forEach { (label, notes) ->
                            item {
                                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                                    SectionLabel(label); Spacer(modifier = Modifier.height(4.dp))
                                }
                            }
                            items(notes, key = { it.id ?: it.title }) { note ->
                                NoteListRow(note) { navController.navigate("note_detail/${note.id}") }
                            }
                            item { Spacer(modifier = Modifier.height(16.dp)) }
                        }
                    }
                }
            }
        }
    }
}

// ─── Create menu row ──────────────────────────────────────────────────────────
@Composable
private fun CreateMenuRow(title: String, subtitle: String, icon: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(painterResource(icon), null, tint = GreenPrimary, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(title, color = OnSurface, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = MutedText, fontSize = 12.sp)
        }
    }
}

// ─── Pinned card ──────────────────────────────────────────────────────────────
@Composable
private fun PinnedNoteCard(note: NoteDto, onClick: () -> Unit) {
    Box(
        modifier = Modifier.width(172.dp).height(108.dp)
            .clip(RoundedCornerShape(14.dp)).background(SurfaceDark)
            .border(1.dp, BorderDark, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick).padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxSize()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(painterResource(R.drawable.ic_pin), null, tint = GreenPrimary, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(note.tags?.split(",")?.firstOrNull()?.trim() ?: "Note", color = MutedText, fontSize = 11.sp)
            }
            Column {
                Text(note.title, color = OnSurface, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (!note.content.isNullOrBlank()) {
                    Text(note.content, color = MutedText, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp))
                }
            }
        }
    }
}

// ─── Note list row ─────────────────────────────────────────────────────────────
@Composable
private fun NoteListRow(note: NoteDto, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(Surface2Dark),
            contentAlignment = Alignment.Center
        ) {
            Icon(painterResource(R.drawable.ic_note), null, tint = MutedText, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(note.title.ifBlank { "Untitled" }, color = OnSurface, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (!note.content.isNullOrBlank()) {
                Text(note.content, color = MutedText, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 1.dp))
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(note.createdAt?.let { formatRelativeDate(it) } ?: "", color = MutedText, fontSize = 11.sp)
    }
}

// ─── Section label ─────────────────────────────────────────────────────────────
@Composable
private fun SectionLabel(label: String) {
    Text(label.uppercase(), color = MutedText, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.8.sp)
}

// ─── Empty state ───────────────────────────────────────────────────────────────
@Composable
private fun EmptyNotesState(onCreateNote: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 60.dp, start = 32.dp, end = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.size(64.dp).clip(RoundedCornerShape(18.dp)).background(SurfaceDark), contentAlignment = Alignment.Center) {
            Icon(painterResource(R.drawable.ic_note), null, tint = MutedText, modifier = Modifier.size(32.dp))
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text("No notes yet", color = OnSurface, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(6.dp))
        Text("Capture thoughts, meeting notes, ideas — anything worth remembering.", color = MutedText, fontSize = 13.sp, lineHeight = 20.sp)
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onCreateNote, colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary, contentColor = Color.Black), shape = RoundedCornerShape(12.dp)) {
            Text("Create your first note", fontWeight = FontWeight.SemiBold)
        }
    }
}

// ─── Date helpers ──────────────────────────────────────────────────────────────
private fun groupNotesByDate(notes: List<NoteDto>): List<Pair<String, List<NoteDto>>> {
    val today     = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0) }
    val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0) }
    val weekAgo   = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -7) }
    val t = mutableListOf<NoteDto>(); val y = mutableListOf<NoteDto>()
    val w = mutableListOf<NoteDto>(); val o = mutableListOf<NoteDto>()
    notes.forEach { note ->
        val cal = parseDateToCal(note.createdAt ?: "")
        when {
            cal != null && cal.after(today)     -> t.add(note)
            cal != null && cal.after(yesterday) -> y.add(note)
            cal != null && cal.after(weekAgo)   -> w.add(note)
            else -> o.add(note)
        }
    }
    return buildList {
        if (t.isNotEmpty()) add("Today"     to t)
        if (y.isNotEmpty()) add("Yesterday" to y)
        if (w.isNotEmpty()) add("This week" to w)
        if (o.isNotEmpty()) add("Older"     to o)
    }
}

private fun parseDateToCal(raw: String): Calendar? {
    listOf("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", "yyyy-MM-dd'T'HH:mm:ss'Z'", "yyyy-MM-dd").forEach { fmt ->
        try {
            val d = SimpleDateFormat(fmt, Locale.getDefault()).also { it.timeZone = TimeZone.getTimeZone("UTC") }.parse(raw)
            if (d != null) return Calendar.getInstance().apply { time = d }
        } catch (_: Exception) {}
    }
    return null
}

private fun formatRelativeDate(raw: String): String {
    val cal   = parseDateToCal(raw) ?: return ""
    val today = Calendar.getInstance()
    return when {
        today.get(Calendar.DAY_OF_YEAR) == cal.get(Calendar.DAY_OF_YEAR) && today.get(Calendar.YEAR) == cal.get(Calendar.YEAR) -> "Today"
        today.get(Calendar.DAY_OF_YEAR) - cal.get(Calendar.DAY_OF_YEAR) == 1 && today.get(Calendar.YEAR) == cal.get(Calendar.YEAR) -> "Yesterday"
        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(cal.time)
    }
}
