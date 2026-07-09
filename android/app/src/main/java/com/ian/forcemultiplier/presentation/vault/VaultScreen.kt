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
private val BgDark       = Color(0xFF080808)
private val SurfaceDark  = Color(0xFF151B23)
private val Surface2Dark = Color(0xFF1C2128)
private val BorderDark   = Color(0xFF30363D)
private val MutedText    = Color(0xFF8B949E)
private val GreenPrimary = Color(0xFF2ED573)
private val OnSurface    = Color(0xFFE6EDF3)

// Folder definitions (UI only — backed by tags)
private data class FolderItem(val name: String, val icon: Int, val tagFilter: String? = null)
private val folders = listOf(
    FolderItem("My Notes",   R.drawable.ic_note),
    FolderItem("To-do list", R.drawable.ic_add),
    FolderItem("Journal",    R.drawable.ic_note),
    FolderItem("Projects",   R.drawable.ic_bet),
    FolderItem("Reading",    R.drawable.ic_note),
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
    var activeTab      by remember { mutableStateOf(0) }  // 0=All Notes, 1=Folders
    var showCreateMenu by remember { mutableStateOf(false) }
    var showTemplates  by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val sheetState     = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Reload on resume
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.getNotes()
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }

    val allNotes = when (val s = notesState) {
        is Resource.Success -> s.data ?: emptyList()
        else -> emptyList()
    }
    val availableTags = remember(allNotes) {
        val tags = allNotes.mapNotNull { it.tags }
            .flatMap { it.split(",") }.map { it.trim() }.filter { it.isNotBlank() }.distinct()
        listOf("All") + tags
    }
    val displayedNotes = remember(allNotes, searchQuery, selectedTag) {
        var list = allNotes.filter { it.parentId == null }
        if (searchQuery.isNotBlank()) list = list.filter {
            it.title.contains(searchQuery, true) ||
            it.content?.contains(searchQuery, true) == true ||
            it.tags?.contains(searchQuery, true) == true
        }
        if (selectedTag != "All") list = list.filter { it.tags?.contains(selectedTag, true) == true }
        list
    }
    val groupedNotes = remember(displayedNotes) { groupNotesByDate(displayedNotes) }

    // ── Templates sheet ───────────────────────────────────────────────────────
    if (showTemplates) {
        ModalBottomSheet(
            onDismissRequest = { showTemplates = false },
            sheetState = sheetState,
            containerColor = SurfaceDark,
            dragHandle = {
                Box(Modifier.padding(top = 10.dp, bottom = 4.dp)) {
                    Box(Modifier.width(36.dp).height(4.dp).background(BorderDark, RoundedCornerShape(2.dp)))
                }
            }
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 48.dp)) {
                Text("Templates", color = OnSurface, fontWeight = FontWeight.Bold, fontSize = 17.sp,
                    modifier = Modifier.padding(vertical = 16.dp))
                noteTemplates.forEach { tpl ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                            .clickable {
                                showTemplates = false
                                val today = SimpleDateFormat("MMM d", Locale.getDefault()).format(Date())
                                navController.navigate(
                                    "note_detail/new?template=${
                                        java.net.URLEncoder.encode(tpl.titlePrefix + today + "|" + tpl.body, "UTF-8")
                                    }"
                                )
                            }.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(42.dp).clip(RoundedCornerShape(10.dp))
                                .background(GreenPrimary.copy(0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(painterResource(R.drawable.ic_note), null, tint = GreenPrimary, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(14.dp))
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
        Column(modifier = Modifier.fillMaxSize()) {

            // ── App header ───────────────────────────────────────────────────
            Column(modifier = Modifier.background(BgDark)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
                        .padding(top = 52.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        // User avatar initials
                        Box(
                            modifier = Modifier.size(36.dp).clip(CircleShape)
                                .background(GreenPrimary.copy(0.15f))
                                .border(1.dp, GreenPrimary.copy(0.3f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("U", color = GreenPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Text(
                            "Notes",
                            color = OnSurface,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            letterSpacing = (-0.3).sp
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        // Search toggle
                        IconButton(
                            onClick = { searchActive = !searchActive; if (!searchActive) searchQuery = "" },
                            modifier = Modifier.size(38.dp).clip(CircleShape)
                                .background(if (searchActive) GreenPrimary.copy(0.12f) else SurfaceDark)
                                .border(1.dp, if (searchActive) GreenPrimary.copy(0.3f) else BorderDark, CircleShape)
                        ) {
                            Icon(painterResource(R.drawable.ic_search), null,
                                tint = if (searchActive) GreenPrimary else MutedText,
                                modifier = Modifier.size(17.dp))
                        }
                        // More/overflow
                        IconButton(
                            onClick = { showCreateMenu = !showCreateMenu },
                            modifier = Modifier.size(38.dp).clip(CircleShape).background(SurfaceDark)
                                .border(1.dp, BorderDark, CircleShape)
                        ) {
                            Icon(painterResource(R.drawable.ic_note), null, tint = MutedText, modifier = Modifier.size(17.dp))
                        }
                    }
                }

                // ── Search bar ───────────────────────────────────────────────
                AnimatedVisibility(visible = searchActive, enter = expandVertically(tween(180)) + fadeIn(), exit = shrinkVertically(tween(180)) + fadeOut()) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 12.dp)
                            .fillMaxWidth().height(42.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(SurfaceDark)
                            .border(1.dp, BorderDark, RoundedCornerShape(22.dp))
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(painterResource(R.drawable.ic_search), null, tint = MutedText, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(10.dp))
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

                // ── Tabs: All Notes / Folders ────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 0.dp)
                ) {
                    listOf("All Notes", "Folders").forEachIndexed { i, label ->
                        Column(
                            modifier = Modifier.clickable { activeTab = i }.padding(vertical = 8.dp).weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                label,
                                color = if (activeTab == i) OnSurface else MutedText,
                                fontSize = 14.sp,
                                fontWeight = if (activeTab == i) FontWeight.SemiBold else FontWeight.Normal
                            )
                            Spacer(Modifier.height(6.dp))
                            Box(
                                modifier = Modifier.fillMaxWidth(0.5f).height(2.dp)
                                    .clip(RoundedCornerShape(1.dp))
                                    .background(if (activeTab == i) GreenPrimary else Color.Transparent)
                            )
                        }
                    }
                }
                HorizontalDivider(color = BorderDark)
            }

            // ── Body ─────────────────────────────────────────────────────────
            if (activeTab == 1) {
                // Folders grid
                FoldersGrid(navController = navController, allNotes = allNotes)
            } else {
                // All Notes
                LazyColumn(
                    modifier = Modifier.fillMaxSize().weight(1f),
                    contentPadding = PaddingValues(bottom = 160.dp)
                ) {
                    // Tag filter chips
                    if (availableTags.size > 1) {
                        item {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(availableTags) { tag ->
                                    val sel = selectedTag == tag
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(if (sel) GreenPrimary.copy(0.12f) else SurfaceDark)
                                            .border(1.dp, if (sel) GreenPrimary.copy(0.35f) else BorderDark, RoundedCornerShape(16.dp))
                                            .clickable { selectedTag = tag }
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(tag, color = if (sel) GreenPrimary else MutedText, fontSize = 12.sp,
                                            fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal)
                                    }
                                }
                            }
                        }
                    }

                    when (val state = notesState) {
                        is Resource.Loading -> item {
                            Box(Modifier.fillMaxWidth().padding(top = 48.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = GreenPrimary, strokeWidth = 2.dp, modifier = Modifier.size(26.dp))
                            }
                        }
                        is Resource.Error -> item {
                            Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                Text(state.message ?: "Could not load notes", color = Color(0xFFFF4757), fontSize = 14.sp)
                            }
                        }
                        is Resource.Success -> {
                            if (displayedNotes.isEmpty() && searchQuery.isBlank() && selectedTag == "All") {
                                item { EmptyNotesState { navController.navigate("note_detail/new") } }
                            } else if (displayedNotes.isEmpty()) {
                                item {
                                    Box(Modifier.fillMaxWidth().padding(top = 48.dp), contentAlignment = Alignment.Center) {
                                        Text("No notes match \"$searchQuery\"", color = MutedText, fontSize = 14.sp)
                                    }
                                }
                            } else {
                                groupedNotes.forEach { (groupLabel, notes) ->
                                    item {
                                        Text(
                                            groupLabel.uppercase(),
                                            color = MutedText,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            letterSpacing = 0.7.sp,
                                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                                                .padding(top = 4.dp)
                                        )
                                    }
                                    items(notes, key = { it.id ?: it.title }) { note ->
                                        NoteListItem(note) { navController.navigate("note_detail/${note.id}") }
                                        HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), color = BorderDark.copy(alpha = 0.5f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── FAB ──────────────────────────────────────────────────────────────
        Column(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 96.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Create dropdown
            AnimatedVisibility(visible = showCreateMenu, enter = fadeIn() + expandVertically(expandFrom = Alignment.Bottom), exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Bottom)) {
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(SurfaceDark)
                        .border(1.dp, BorderDark, RoundedCornerShape(16.dp))
                        .padding(vertical = 6.dp)
                ) {
                    FabMenuItem("Blank page", R.drawable.ic_add) {
                        showCreateMenu = false
                        navController.navigate("note_detail/new")
                    }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 14.dp), color = BorderDark)
                    FabMenuItem("From template", R.drawable.ic_note) {
                        showCreateMenu = false
                        showTemplates = true
                    }
                }
                Spacer(Modifier.height(10.dp))
            }

            // Main FAB
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(28.dp))
                    .background(GreenPrimary)
                    .clickable { showCreateMenu = !showCreateMenu }
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(painterResource(R.drawable.ic_add), null, tint = Color.Black, modifier = Modifier.size(16.dp))
                    Text("Add new note", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp, letterSpacing = 0.2.sp)
                }
            }
        }
    }
}

// ─── Note list item ────────────────────────────────────────────────────────────
@Composable
private fun NoteListItem(note: NoteDto, onClick: () -> Unit) {
    val dateStr = note.createdAt?.let { formatListDate(it) } ?: ""
    val tagList = note.tags?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()

    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Date column
        Column(
            modifier = Modifier.width(44.dp).padding(top = 2.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                dateStr.take(2),   // day number
                color = MutedText,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 20.sp
            )
            Text(
                dateStr.drop(3).take(3).uppercase(), // month abbrev
                color = MutedText,
                fontSize = 10.sp,
                letterSpacing = 0.5.sp
            )
        }

        Spacer(Modifier.width(14.dp))

        // Content
        Column(modifier = Modifier.weight(1f)) {
            Text(
                note.title.ifBlank { "Untitled" },
                color = OnSurface,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!note.content.isNullOrBlank()) {
                Spacer(Modifier.height(3.dp))
                Text(
                    note.content,
                    color = MutedText,
                    fontSize = 13.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 18.sp
                )
            }
            if (tagList.isNotEmpty()) {
                Spacer(Modifier.height(7.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    val visible = tagList.take(3)
                    val extra  = tagList.size - visible.size
                    visible.forEach { tag ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(5.dp))
                                .background(Surface2Dark)
                                .border(1.dp, BorderDark, RoundedCornerShape(5.dp))
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Text(tag, color = MutedText, fontSize = 10.sp)
                        }
                    }
                    if (extra > 0) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(5.dp))
                                .background(Surface2Dark)
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Text("+$extra", color = MutedText, fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }
}

// ─── Folders grid ─────────────────────────────────────────────────────────────
@Composable
private fun FoldersGrid(navController: NavController, allNotes: List<NoteDto>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val rows = folders.chunked(2)
        items(rows) { rowItems ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                rowItems.forEach { folder ->
                    FolderCard(folder = folder, modifier = Modifier.weight(1f)) {
                        // Navigate to all notes filtered by this folder tag
                        navController.navigate("vault")
                    }
                }
                if (rowItems.size < 2) Spacer(Modifier.weight(1f))
            }
        }
        item {
            Spacer(Modifier.height(60.dp))
        }
    }
}

@Composable
private fun FolderCard(folder: FolderItem, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .height(100.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceDark)
            .border(1.dp, BorderDark, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(Surface2Dark),
                contentAlignment = Alignment.Center
            ) {
                Icon(painterResource(folder.icon), null, tint = MutedText, modifier = Modifier.size(20.dp))
            }
            Text(folder.name, color = OnSurface, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        }
    }
}

// ─── FAB menu item ─────────────────────────────────────────────────────────────
@Composable
private fun FabMenuItem(label: String, icon: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(painterResource(icon), null, tint = GreenPrimary, modifier = Modifier.size(17.dp))
        Spacer(Modifier.width(12.dp))
        Text(label, color = OnSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

// ─── Empty state ───────────────────────────────────────────────────────────────
@Composable
private fun EmptyNotesState(onCreateNote: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 72.dp, start = 40.dp, end = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(72.dp).clip(RoundedCornerShape(22.dp)).background(SurfaceDark)
                .border(1.dp, BorderDark, RoundedCornerShape(22.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(painterResource(R.drawable.ic_note), null, tint = MutedText, modifier = Modifier.size(32.dp))
        }
        Spacer(Modifier.height(18.dp))
        Text("No notes yet", color = OnSurface, fontWeight = FontWeight.Bold, fontSize = 17.sp)
        Spacer(Modifier.height(7.dp))
        Text(
            "Capture thoughts, meeting notes, ideas — anything worth remembering.",
            color = MutedText, fontSize = 13.sp, lineHeight = 20.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
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

// Returns "20 APR" format
private fun formatListDate(raw: String): String {
    val cal = parseDateToCal(raw) ?: return ""
    return SimpleDateFormat("dd MMM", Locale.getDefault()).format(cal.time).uppercase()
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

// ─── Note templates (kept here for template sheet) ─────────────────────────────
data class NoteTemplate(val name: String, val description: String, val titlePrefix: String, val body: String)

val noteTemplates = listOf(
    NoteTemplate("Meeting Notes", "Agenda, decisions, action items", "Meeting: ",
        "## Attendees\n- \n\n## Agenda\n1. \n2. \n\n## Key Decisions\n\n\n## Action Items\n- [ ] \n- [ ] \n\n## Notes\n\n"),
    NoteTemplate("Daily Log", "What you worked on today", "Daily Log - ",
        "## Today's focus\n\n\n## What I did\n\n\n## Blockers\n\n\n## Tomorrow\n\n"),
    NoteTemplate("Ideas", "Brain dump for new ideas", "Ideas: ",
        "## The idea\n\n\n## Why it matters\n\n\n## How it could work\n\n\n## Next steps\n\n"),
    NoteTemplate("Project Plan", "Goals, tasks, timeline", "Plan: ",
        "## Objective\n\n\n## Deliverables\n- \n- \n\n## Tasks\n- [ ] \n- [ ] \n- [ ] \n\n## Timeline\n\n\n## Notes\n\n"),
    NoteTemplate("Weekly Review", "Wins, improvements, goals", "Week Review - ",
        "## Wins this week\n\n\n## What could be better\n\n\n## Learnings\n\n\n## Goals for next week\n1. \n2. \n3. \n\n"),
    NoteTemplate("Lecture Notes", "Key points and takeaways", "Lecture: ",
        "## Topic\n\n\n## Key Concepts\n\n\n## Important Details\n\n\n## Questions\n\n\n## Summary\n\n"),
)
