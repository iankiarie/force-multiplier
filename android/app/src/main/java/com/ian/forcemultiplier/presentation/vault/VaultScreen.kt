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
import androidx.compose.ui.graphics.Brush
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
import com.ian.forcemultiplier.core.theme.FMColors
import com.ian.forcemultiplier.data.remote.dto.NoteDto
import com.ian.forcemultiplier.domain.model.Folder
import com.ian.forcemultiplier.presentation.vault.viewmodel.VaultViewModel
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultScreen(
    navController: NavController,
    viewModel: VaultViewModel = hiltViewModel()
) {
    val notesState     by viewModel.notesState.collectAsState()
    val foldersState   by viewModel.foldersState.collectAsState()
    val selectedFolder by viewModel.selectedFolder.collectAsState()
    var searchQuery    by remember { mutableStateOf("") }
    var searchActive   by remember { mutableStateOf(false) }
    var selectedTag    by remember { mutableStateOf("All") }
    var activeTab      by remember { mutableStateOf(0) }  // 0=All Notes, 1=Folders
    var showCreateMenu by remember { mutableStateOf(false) }
    var showTemplates  by remember { mutableStateOf(false) }
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var renameFolderTarget by remember { mutableStateOf<Folder?>(null) }
    var deleteFolderTarget by remember { mutableStateOf<Folder?>(null) }
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
    val displayedNotes = remember(allNotes, searchQuery, selectedTag, selectedFolder) {
        var list = allNotes.filter { it.parentId == null }
        if (searchQuery.isNotBlank()) list = list.filter {
            it.title.contains(searchQuery, true) ||
            it.content?.contains(searchQuery, true) == true ||
            it.tags?.contains(searchQuery, true) == true
        }
        if (selectedTag != "All") list = list.filter { it.tags?.contains(selectedTag, true) == true }
        val folderId = selectedFolder?.id
        if (folderId != null) {
            list = list.filter { it.folderId == folderId }
        }
        list
    }
    val groupedNotes = remember(displayedNotes) { groupNotesByDate(displayedNotes) }
    val folderList = (foldersState as? Resource.Success)?.data.orEmpty()

    fun buildNewNoteRoute(
        parentId: String? = null,
        template: String? = null,
        folderId: String? = selectedFolder?.id
    ): String {
        val query = buildList {
            parentId?.let { add("parentId=$it") }
            template?.let { add("template=$it") }
            folderId?.let { add("folderId=$it") }
        }
        return buildString {
            append("note_detail/new")
            if (query.isNotEmpty()) {
                append("?")
                append(query.joinToString("&"))
            }
        }
    }

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
                                    buildNewNoteRoute(
                                        template = java.net.URLEncoder.encode(
                                            tpl.titlePrefix + today + "|" + tpl.body,
                                            "UTF-8"
                                        )
                                    )
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

    if (showCreateFolderDialog) {
        FolderNameDialog(
            title = "New folder",
            initialValue = "",
            confirmLabel = "Create",
            onDismiss = { showCreateFolderDialog = false },
            onConfirm = { name ->
                viewModel.createFolder(name = name, icon = "ic_note")
                showCreateFolderDialog = false
            }
        )
    }

    renameFolderTarget?.let { folder ->
        FolderNameDialog(
            title = "Rename folder",
            initialValue = folder.name,
            confirmLabel = "Save",
            onDismiss = { renameFolderTarget = null },
            onConfirm = { name ->
                viewModel.renameFolder(folder.id, name)
                renameFolderTarget = null
            }
        )
    }

    deleteFolderTarget?.let { folder ->
        AlertDialog(
            onDismissRequest = { deleteFolderTarget = null },
            containerColor = SurfaceDark,
            title = { Text("Delete folder", color = OnSurface) },
            text = {
                Text(
                    "Notes inside ${folder.name} will be moved to another folder so nothing is lost.",
                    color = MutedText
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteFolder(folder.id)
                        deleteFolderTarget = null
                    }
                ) {
                    Text("Delete", color = FMColors.Error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteFolderTarget = null }) {
                    Text("Cancel", color = MutedText)
                }
            }
        )
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
                FoldersGrid(
                    allNotes = allNotes,
                    folders = folderList,
                    onCreateFolder = { showCreateFolderDialog = true },
                    onRenameFolder = { renameFolderTarget = it },
                    onDeleteFolder = { deleteFolderTarget = it },
                    onFolderClick = { folder ->
                        viewModel.selectFolder(folder)
                        selectedTag = "All"
                        activeTab = 0
                    }
                )
            } else {
                // All Notes
                LazyColumn(
                    modifier = Modifier.fillMaxSize().weight(1f),
                    contentPadding = PaddingValues(bottom = 160.dp)
                ) {
                    item {
                        NotesHeroHeader(
                            folder = selectedFolder,
                            folderCount = folderList.size,
                            onClearFolder = {
                                viewModel.clearFolderSelection()
                                selectedTag = "All"
                            }
                        )
                    }

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

                    if (selectedFolder != null) {
                        item {
                            FolderFilterChip(
                                folderName = selectedFolder?.name.orEmpty(),
                                onClear = {
                                    viewModel.clearFolderSelection()
                                    selectedTag = "All"
                                }
                            )
                        }
                    }

                    when (val state = notesState) {
                        is Resource.Loading -> item {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(top = 56.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator(color = GreenPrimary, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
                                Spacer(Modifier.height(10.dp))
                                Text("Loading notes…", color = MutedText, fontSize = 13.sp)
                            }
                        }
                        is Resource.Error -> item {
                            Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Couldn’t load notes", color = OnSurface, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                                    Spacer(Modifier.height(6.dp))
                                    Text(state.message ?: "Check your connection and try again.", color = MutedText, fontSize = 13.sp)
                                    Spacer(Modifier.height(12.dp))
                                    TextButton(onClick = { viewModel.getNotes() }) {
                                        Text("Retry", color = GreenPrimary)
                                    }
                                }
                            }
                        }
                        is Resource.Success -> {
                            if (displayedNotes.isEmpty() && searchQuery.isBlank() && selectedTag == "All") {
                                item {
                                    EmptyNotesState(
                                        folderName = selectedFolder?.name,
                                        onCreateNote = { navController.navigate(buildNewNoteRoute()) },
                                        onExploreFolders = { activeTab = 1 }
                                    )
                                }
                            } else if (displayedNotes.isEmpty()) {
                                item {
                                    Box(Modifier.fillMaxWidth().padding(top = 48.dp), contentAlignment = Alignment.Center) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("No notes match your filters", color = OnSurface, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                                            Spacer(Modifier.height(6.dp))
                                            Text(
                                                if (searchQuery.isNotBlank()) "Try a different search term." else "Clear the folder or tag filter.",
                                                color = MutedText,
                                                fontSize = 13.sp
                                            )
                                        }
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
                        navController.navigate(buildNewNoteRoute())
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
    val tagList   = note.tags?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()
    val shortDate = note.createdAt?.let { formatShortDate(it) } ?: ""

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Page icon
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Surface2Dark),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_note),
                contentDescription = null,
                tint = MutedText.copy(alpha = 0.55f),
                modifier = Modifier.size(16.dp)
            )
        }

        Spacer(Modifier.width(13.dp))

        // Title + preview + tags
        Column(modifier = Modifier.weight(1f)) {
            Text(
                note.title.ifBlank { "Untitled" },
                color = OnSurface,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!note.content.isNullOrBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    note.content,
                    color = MutedText,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (tagList.isNotEmpty()) {
                Spacer(Modifier.height(5.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    tagList.take(2).forEach { tag ->
                        Text(
                            "# $tag",
                            color = GreenPrimary.copy(alpha = 0.65f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    if (tagList.size > 2) {
                        Text("+${tagList.size - 2} more", color = MutedText, fontSize = 10.sp)
                    }
                }
            }
        }

        Spacer(Modifier.width(10.dp))

        // Relative date
        Text(shortDate, color = MutedText.copy(alpha = 0.55f), fontSize = 11.sp)
    }
}

// ─── Folders grid ─────────────────────────────────────────────────────────────
@Composable
private fun FoldersGrid(
    allNotes: List<NoteDto>,
    folders: List<Folder>,
    onCreateFolder: () -> Unit,
    onRenameFolder: (Folder) -> Unit,
    onDeleteFolder: (Folder) -> Unit,
    onFolderClick: (Folder) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Folders", color = OnSurface, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text("Create, rename, and organize your note spaces.", color = MutedText, fontSize = 12.sp)
                }
                TextButton(onClick = onCreateFolder) {
                    Text("New folder", color = GreenPrimary)
                }
            }
        }

        if (folders.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No folders yet", color = MutedText, fontSize = 14.sp)
                }
            }
            return@LazyColumn
        }

        val rows = folders.chunked(2)
        items(rows) { rowItems ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                rowItems.forEach { folder ->
                    val noteCount = allNotes.count {
                        it.parentId == null && it.folderId == folder.id
                    }

                    FolderCard(
                        folder = folder,
                        noteCount = noteCount,
                        modifier = Modifier.weight(1f),
                        onRename = { onRenameFolder(folder) },
                        onDelete = { onDeleteFolder(folder) },
                        onClick = { onFolderClick(folder) }
                    )
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
private fun FolderCard(
    folder: Folder,
    noteCount: Int,
    modifier: Modifier = Modifier,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .height(112.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(listOf(SurfaceDark, Surface2Dark)))
            .border(1.dp, BorderDark.copy(alpha = 0.75f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(Surface2Dark),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(folderIconRes(folder.icon)),
                    contentDescription = null,
                    tint = MutedText,
                    modifier = Modifier.size(20.dp)
                )
            }
            Box(modifier = Modifier.align(Alignment.End)) {
                Text(
                    text = "⋯",
                    color = MutedText,
                    fontSize = 18.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { menuExpanded = true }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    containerColor = SurfaceDark
                ) {
                    DropdownMenuItem(
                        text = { Text("Rename", color = OnSurface) },
                        onClick = {
                            menuExpanded = false
                            onRename()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete", color = FMColors.Error) },
                        onClick = {
                            menuExpanded = false
                            onDelete()
                        }
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(folder.name, color = OnSurface, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text("$noteCount notes", color = MutedText, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun NotesHeroHeader(
    folder: Folder?,
    folderCount: Int,
    onClearFolder: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = folder?.name ?: "All notes",
            color = OnSurface,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
        if (folder != null) {
            TextButton(
                onClick = onClearFolder,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text("← All notes", color = GreenPrimary, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun FolderNameDialog(
    title: String,
    initialValue: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var value by remember(initialValue) { mutableStateOf(initialValue) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        title = { Text(title, color = OnSurface) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                singleLine = true,
                label = { Text("Folder name") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GreenPrimary,
                    unfocusedBorderColor = BorderDark,
                    focusedTextColor = OnSurface,
                    unfocusedTextColor = OnSurface,
                    focusedLabelColor = GreenPrimary,
                    unfocusedLabelColor = MutedText,
                    cursorColor = GreenPrimary
                )
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(value) }) {
                Text(confirmLabel, color = GreenPrimary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MutedText)
            }
        }
    )
}

@Composable
private fun StatPill(label: String, value: String) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(SurfaceDark)
            .border(1.dp, BorderDark, RoundedCornerShape(999.dp))
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(label, color = MutedText, fontSize = 11.sp)
        Text(value, color = OnSurface, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun FolderFilterChip(folderName: String, onClear: () -> Unit) {
    Row(
        modifier = Modifier
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(GreenPrimary.copy(0.12f))
            .border(1.dp, GreenPrimary.copy(0.35f), RoundedCornerShape(999.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Folder:", color = MutedText, fontSize = 12.sp)
        Text(folderName, color = GreenPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        TextButton(onClick = onClear, contentPadding = PaddingValues(0.dp), modifier = Modifier.height(20.dp)) {
            Text("Clear", color = GreenPrimary, fontSize = 12.sp)
        }
    }
}

private fun folderIconRes(iconKey: String?): Int {
    return when (iconKey) {
        "ic_add" -> R.drawable.ic_add
        "ic_bet" -> R.drawable.ic_bet
        else -> R.drawable.ic_note
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
private fun EmptyNotesState(
    folderName: String?,
    onCreateNote: () -> Unit,
    onExploreFolders: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 72.dp, start = 32.dp, end = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(76.dp).clip(RoundedCornerShape(24.dp)).background(SurfaceDark)
                .border(1.dp, BorderDark, RoundedCornerShape(22.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(painterResource(R.drawable.ic_note), null, tint = GreenPrimary, modifier = Modifier.size(32.dp))
        }
        Spacer(Modifier.height(20.dp))
        Text("No notes yet", color = OnSurface, fontWeight = FontWeight.Bold, fontSize = 17.sp)
        Spacer(Modifier.height(7.dp))
        Text(
            if (folderName != null)
                "No notes in $folderName yet. Add one to get started."
            else
                "Capture thoughts, meeting notes, ideas — anything worth remembering.",
            color = MutedText, fontSize = 13.sp, lineHeight = 20.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = onExploreFolders) {
                Text("Browse folders")
            }
            Button(onClick = onCreateNote, colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary, contentColor = Color.Black)) {
                Text("New note")
            }
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

private fun formatShortDate(raw: String): String {
    val cal = parseDateToCal(raw) ?: return ""
    val now = Calendar.getInstance()
    val diffDays = ((now.timeInMillis - cal.timeInMillis) / (1000L * 60 * 60 * 24)).toInt()
    return when {
        diffDays < 1 -> "Today"
        diffDays < 2 -> "Yesterday"
        diffDays < 7 -> "${diffDays}d ago"
        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(cal.time)
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
