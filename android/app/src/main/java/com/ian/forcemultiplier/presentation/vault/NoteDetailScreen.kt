@file:OptIn(kotlinx.coroutines.FlowPreview::class)

package com.ian.forcemultiplier.presentation.vault

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.ian.forcemultiplier.R
import com.ian.forcemultiplier.core.theme.FMColors
import com.ian.forcemultiplier.data.remote.dto.NoteDto
import com.ian.forcemultiplier.presentation.vault.viewmodel.VaultViewModel
import com.ian.forcemultiplier.util.Resource
import kotlinx.coroutines.flow.debounce
import java.text.SimpleDateFormat
import java.util.*
import kotlin.time.Duration.Companion.milliseconds

// ─── Palette ──────────────────────────────────────────────────────────────────
private val BgDark       = FMColors.DarkBg
private val SurfaceDark  = FMColors.DarkSurface
private val Surface2Dark = FMColors.DarkSurface2
private val BorderDark   = FMColors.DarkOutline
private val MutedText    = FMColors.DarkMuted
private val GreenPrimary = FMColors.Primary
private val OnSurface    = FMColors.DarkOnSurface

private enum class SaveStatus { Idle, Saving, Saved, Error }

private data class NoteDraftSnapshot(
    val title: String,
    val content: String,
    val tags: String,
    val folderId: String?
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteDetailScreen(
    navController: NavController,
    noteId: String? = null,
    parentId: String? = null,
    template: String? = null,
    initialFolderId: String? = null,
    viewModel: VaultViewModel = hiltViewModel()
) {
    // ── State ─────────────────────────────────────────────────────────────────
    var title          by remember { mutableStateOf("") }
    var contentState   by remember { mutableStateOf(TextFieldValue("")) }
    var tags           by remember { mutableStateOf("") }
    var newTagInput    by remember { mutableStateOf("") }
    var showTagInput   by remember { mutableStateOf(false) }
    var selectedFolderId by remember { mutableStateOf<String?>(null) }
    var folderMenuExpanded by remember { mutableStateOf(false) }
    var savedId        by rememberSaveable { mutableStateOf<String?>(if (noteId != "new" && noteId != null) noteId else null) }
    var saveStatus     by remember { mutableStateOf(SaveStatus.Idle) }
    var pendingBack    by remember { mutableStateOf(false) }
    var isInitialLoad  by remember { mutableStateOf(true) }

    val currentNoteState by viewModel.currentNoteState.collectAsState()
    val allNotesState    by viewModel.notesState.collectAsState()
    val foldersState     by viewModel.foldersState.collectAsState()
    val activeFolderFilter by viewModel.selectedFolder.collectAsState()
    val scope            = rememberCoroutineScope()
    val scrollState      = rememberScrollState()

    val folders = (foldersState as? Resource.Success)?.data.orEmpty()
    val selectedFolder = folders.firstOrNull { it.id == selectedFolderId }
    val shouldSyncVaultFolderFilter = activeFolderFilter != null

    fun syncVaultFolderFilter(folderId: String?) {
        if (!shouldSyncVaultFolderFilter) return
        viewModel.selectFolder(folders.firstOrNull { it.id == folderId })
    }

    fun buildNewNoteRoute(parentNoteId: String?): String {
        val query = buildList {
            parentNoteId?.let { add("parentId=$it") }
            selectedFolderId?.let { add("folderId=$it") }
        }
        return buildString {
            append("note_detail/new")
            if (query.isNotEmpty()) {
                append("?")
                append(query.joinToString("&"))
            }
        }
    }

    LaunchedEffect(noteId, initialFolderId, activeFolderFilter?.id) {
        if ((noteId == null || noteId == "new") && selectedFolderId == null) {
            selectedFolderId = initialFolderId ?: activeFolderFilter?.id
        }
    }

    val subNotes = remember(allNotesState, savedId) {
        if (allNotesState is Resource.Success) allNotesState.data?.filter { it.parentId == savedId } ?: emptyList()
        else emptyList()
    }

    // ── Load existing note ────────────────────────────────────────────────────
    LaunchedEffect(noteId) {
        if (noteId != null && noteId != "new") {
            viewModel.getNoteById(noteId)
        } else {
            viewModel.clearCurrentNote()
            if (template != null) {
                val decoded = try { java.net.URLDecoder.decode(template, "UTF-8") } catch (_: Exception) { template }
                val parts = decoded.split("|", limit = 2)
                if (parts.isNotEmpty()) title = parts[0]
                if (parts.size > 1) contentState = TextFieldValue(parts[1])
            }
            isInitialLoad = false
        }
    }

    LaunchedEffect(currentNoteState) {
        val state = currentNoteState
        if (state is Resource.Success && state.data != null) {
            if (isInitialLoad) {
                title = state.data.title
                contentState = TextFieldValue(state.data.content ?: "")
                tags = state.data.tags ?: ""
                selectedFolderId = state.data.folderId
                isInitialLoad = false
            } else {
                if (state.data.id != null) savedId = state.data.id
                selectedFolderId = state.data.folderId ?: selectedFolderId
                saveStatus = SaveStatus.Saved
            }
        } else if (state is Resource.Error && !isInitialLoad) {
            saveStatus = SaveStatus.Error
        }
    }

    // ── Auto-save ─────────────────────────────────────────────────────────────
    LaunchedEffect(Unit) {
        snapshotFlow {
            NoteDraftSnapshot(
                title = title,
                content = contentState.text,
                tags = tags,
                folderId = selectedFolderId
            )
        }
            .debounce(1500.milliseconds)
            .collect { draft ->
                if (!isInitialLoad && (savedId != null || draft.title.isNotBlank() || draft.content.isNotBlank())) {
                    saveStatus = SaveStatus.Saving
                    val dto = NoteDto(id = savedId, title = draft.title.ifBlank { "Untitled" },
                        content = draft.content.takeIf { it.isNotBlank() },
                        tags = draft.tags.takeIf { it.isNotBlank() },
                        parentId = parentId, folderId = draft.folderId, userId = null)
                    if (savedId == null) viewModel.createNote(dto)
                    else viewModel.updateNote(dto)
                }
            }
    }

    // ── Save-then-back ────────────────────────────────────────────────────────
    LaunchedEffect(pendingBack, saveStatus) {
        if (pendingBack && saveStatus in listOf(SaveStatus.Saved, SaveStatus.Error, SaveStatus.Idle)) {
            navController.popBackStack()
        }
    }

    fun triggerBack() {
        if (!isInitialLoad && (savedId != null || title.isNotBlank() || contentState.text.isNotBlank())) {
            val dto = NoteDto(id = savedId, title = title.ifBlank { "Untitled" },
                content = contentState.text.takeIf { it.isNotBlank() },
                tags = tags.takeIf { it.isNotBlank() },
                parentId = parentId, folderId = selectedFolderId, userId = null)
            saveStatus = SaveStatus.Saving
            pendingBack = true
            if (savedId == null) viewModel.createNote(dto) else viewModel.updateNote(dto)
        } else {
            navController.popBackStack()
        }
    }

    BackHandler { triggerBack() }

    // ── Formatting helpers ────────────────────────────────────────────────────
    fun wrapSelection(wrapper: String) {
        val sel  = contentState.selection; val text = contentState.text
        val (newText, newCursor) = if (sel.collapsed) {
            val nt = text.substring(0, sel.start) + wrapper + wrapper + text.substring(sel.start)
            nt to sel.start + wrapper.length
        } else {
            val selected = text.substring(sel.start, sel.end)
            val nt = text.substring(0, sel.start) + wrapper + selected + wrapper + text.substring(sel.end)
            nt to sel.end + wrapper.length * 2
        }
        contentState = contentState.copy(text = newText, selection = TextRange(newCursor))
    }

    fun insertLinePrefix(prefix: String) {
        val text = contentState.text; val cursor = contentState.selection.start
        val lineStart = text.lastIndexOf('\n', cursor - 1) + 1
        val newText = text.substring(0, lineStart) + prefix + text.substring(lineStart)
        contentState = contentState.copy(text = newText, selection = TextRange(cursor + prefix.length))
    }

    // ── UI ────────────────────────────────────────────────────────────────────
    Box(modifier = Modifier.fillMaxSize().background(BgDark)) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Top nav bar ──────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp)
                    .padding(top = 36.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back button — "< All Notes" style
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { triggerBack() }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_home),
                        contentDescription = "Back",
                        tint = GreenPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(5.dp))
                    Text("All Notes", color = GreenPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }

                Spacer(Modifier.weight(1f))

                // Save status pill
                AnimatedVisibility(visible = saveStatus != SaveStatus.Idle, enter = fadeIn(), exit = fadeOut()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                when (saveStatus) {
                                    SaveStatus.Saving -> Color(0xFF1C2128)
                                    SaveStatus.Saved  -> GreenPrimary.copy(0.12f)
                                    SaveStatus.Error  -> Color(0xFFFF4757).copy(0.12f)
                                    else              -> Color.Transparent
                                }
                            )
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = when (saveStatus) {
                                SaveStatus.Saving -> "Saving..."
                                SaveStatus.Saved  -> "Saved"
                                SaveStatus.Error  -> "Error saving"
                                else              -> ""
                            },
                            color = when (saveStatus) {
                                SaveStatus.Saved  -> GreenPrimary
                                SaveStatus.Error  -> Color(0xFFFF4757)
                                else              -> MutedText
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(Modifier.width(8.dp))

                // Delete button
                if (savedId != null) {
                    IconButton(
                        onClick = { scope.let { viewModel.deleteNote(savedId!!); navController.popBackStack() } },
                        modifier = Modifier.size(36.dp).clip(CircleShape).background(Surface2Dark)
                    ) {
                        Icon(painterResource(R.drawable.ic_visibility_off), null,
                            tint = Color(0xFFFF4757).copy(0.7f), modifier = Modifier.size(16.dp))
                    }
                }
            }

            // ── Scrollable body ───────────────────────────────────────────────
            Column(
                modifier = Modifier.weight(1f).verticalScroll(scrollState)
                    .padding(horizontal = 24.dp)
            ) {
                Spacer(Modifier.height(8.dp))

                // Title
                BasicTextField(
                    value = title,
                    onValueChange = { title = it },
                    textStyle = TextStyle(
                        color = OnSurface,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 26.sp,
                        lineHeight = 34.sp,
                        letterSpacing = (-0.8).sp
                    ),
                    cursorBrush = SolidColor(GreenPrimary),
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    decorationBox = { inner ->
                        if (title.isEmpty()) Text("Untitled",
                            style = TextStyle(color = MutedText.copy(0.35f), fontWeight = FontWeight.ExtraBold, fontSize = 26.sp))
                        inner()
                    }
                )

                Spacer(Modifier.height(14.dp))

                // ── Metadata rows ────────────────────────────────────────────
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceDark)
                        .border(1.dp, BorderDark, RoundedCornerShape(12.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Created by
                    MetaRow("Created by") {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier.size(20.dp).clip(CircleShape)
                                    .background(Brush.linearGradient(listOf(GreenPrimary, Color(0xFF17C3B2)))),
                                contentAlignment = Alignment.Center
                            ) { Text("U", color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Bold) }
                            Spacer(Modifier.width(6.dp))
                            Text("You", color = OnSurface, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    // Last modified
                    MetaRow("Last Modified") {
                        Text(
                            SimpleDateFormat("d MMMM yyyy, HH:mm", Locale.getDefault()).format(Date()),
                            color = MutedText, fontSize = 13.sp
                        )
                    }
                    // Folder
                    MetaRow("Folder") {
                        ExposedDropdownMenuBox(
                            expanded = folderMenuExpanded,
                            onExpandedChange = { folderMenuExpanded = !folderMenuExpanded }
                        ) {
                            OutlinedTextField(
                                value = selectedFolder?.name ?: if (folders.isEmpty()) "No folders" else "Choose folder",
                                onValueChange = {},
                                readOnly = true,
                                singleLine = true,
                                modifier = Modifier.menuAnchor(type = MenuAnchorType.PrimaryNotEditable).widthIn(min = 170.dp),
                                textStyle = TextStyle(color = OnSurface, fontSize = 13.sp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = GreenPrimary,
                                    unfocusedBorderColor = BorderDark,
                                    focusedTextColor = OnSurface,
                                    unfocusedTextColor = OnSurface
                                )
                            )
                            ExposedDropdownMenu(
                                expanded = folderMenuExpanded,
                                onDismissRequest = { folderMenuExpanded = false },
                                containerColor = SurfaceDark
                            ) {
                                folders.forEach { folder ->
                                    DropdownMenuItem(
                                        text = { Text(folder.name, color = OnSurface) },
                                        onClick = {
                                            selectedFolderId = folder.id
                                            syncVaultFolderFilter(folder.id)
                                            folderMenuExpanded = false
                                        }
                                    )
                                }
                                if (folders.isNotEmpty()) {
                                    HorizontalDivider(color = BorderDark)
                                    DropdownMenuItem(
                                        text = { Text("Clear folder", color = MutedText) },
                                        onClick = {
                                            selectedFolderId = null
                                            syncVaultFolderFilter(null)
                                            folderMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                    // Tags
                    MetaRow("Tags") {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val tagList = tags.split(",").map { it.trim() }.filter { it.isNotBlank() }
                            tagList.forEach { tag ->
                                Box(
                                    modifier = Modifier.clip(RoundedCornerShape(6.dp))
                                        .background(GreenPrimary.copy(0.1f))
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) { Text(tag, color = GreenPrimary, fontSize = 11.sp) }
                            }
                            if (showTagInput) {
                                BasicTextField(
                                    value = newTagInput,
                                    onValueChange = { newTagInput = it },
                                    textStyle = TextStyle(color = OnSurface, fontSize = 12.sp),
                                    cursorBrush = SolidColor(GreenPrimary), singleLine = true,
                                    modifier = Modifier.width(70.dp),
                                    decorationBox = { inner ->
                                        if (newTagInput.isEmpty()) Text("tag...", color = MutedText, fontSize = 12.sp)
                                        inner()
                                    }
                                )
                                TextButton(
                                    onClick = {
                                        if (newTagInput.isNotBlank()) tags = if (tags.isBlank()) newTagInput.trim() else "$tags, ${newTagInput.trim()}"
                                        newTagInput = ""; showTagInput = false
                                    },
                                    contentPadding = PaddingValues(horizontal = 4.dp), modifier = Modifier.height(24.dp)
                                ) { Text("Add", color = GreenPrimary, fontSize = 12.sp) }
                            } else {
                                Box(
                                    modifier = Modifier.size(22.dp).clip(CircleShape)
                                        .background(Surface2Dark).border(1.dp, BorderDark, CircleShape)
                                        .clickable { showTagInput = true },
                                    contentAlignment = Alignment.Center
                                ) { Text("+", color = MutedText, fontSize = 14.sp) }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                // ── Content editor ─────────────────────────────────────────────
                BasicTextField(
                    value = contentState,
                    onValueChange = { contentState = it },
                    textStyle = TextStyle(color = OnSurface, fontSize = 15.sp, lineHeight = 25.sp),
                    cursorBrush = SolidColor(GreenPrimary),
                    modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 280.dp),
                    decorationBox = { inner ->
                        if (contentState.text.isEmpty()) Text("Start writing...",
                            style = TextStyle(color = MutedText.copy(0.4f), fontSize = 15.sp, lineHeight = 25.sp))
                        inner()
                    }
                )

                // ── Sub-pages ──────────────────────────────────────────────────
                if (subNotes.isNotEmpty()) {
                    Spacer(Modifier.height(36.dp))
                    HorizontalDivider(color = BorderDark)
                    Spacer(Modifier.height(14.dp))
                    Text("Sub-pages", color = MutedText, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.7.sp)
                    Spacer(Modifier.height(8.dp))
                    subNotes.forEach { sub ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                                .clickable { navController.navigate("note_detail/${sub.id}") }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(painterResource(R.drawable.ic_note), null, tint = MutedText, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(10.dp))
                            Text(sub.title.ifBlank { "Untitled" }, color = OnSurface, fontSize = 14.sp,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }

                // Add sub-page
                Spacer(Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                        .clickable { navController.navigate(buildNewNoteRoute(savedId)) }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(painterResource(R.drawable.ic_add), null, tint = MutedText, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Add sub-page", color = MutedText, fontSize = 14.sp)
                }

                Spacer(Modifier.height(80.dp))
            }

            // ── Format toolbar ──────────────────────────────────────────────
            Column {
                HorizontalDivider(color = BorderDark)
                Row(
                    modifier = Modifier.fillMaxWidth().background(SurfaceDark)
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FmFormatBtn("B", fontWeight = FontWeight.Bold)   { wrapSelection("**") }
                    FmFormatBtn("I", fontStyle = FontStyle.Italic)   { wrapSelection("_") }
                    FmToolDivider()
                    FmFormatBtn("H1") { insertLinePrefix("# ") }
                    FmFormatBtn("H2") { insertLinePrefix("## ") }
                    FmFormatBtn("H3") { insertLinePrefix("### ") }
                    FmToolDivider()
                    FmFormatBtn(">")  { insertLinePrefix("> ") }
                    FmFormatBtn("•")  { insertLinePrefix("- ") }
                    FmFormatBtn("1.") { insertLinePrefix("1. ") }
                    FmFormatBtn("☐")  { insertLinePrefix("- [ ] ") }
                    FmToolDivider()
                    FmFormatBtn("</>") { wrapSelection("`") }
                    FmFormatBtn("—") {
                        contentState = contentState.copy(
                            text = contentState.text.substring(0, contentState.selection.start) +
                                    "\n---\n" + contentState.text.substring(contentState.selection.start),
                            selection = TextRange(contentState.selection.start + 5)
                        )
                    }
                }
            }
        }
    }
}

// ─── Metadata row layout ────────────────────────────────────────────────────
@Composable
private fun MetaRow(label: String, value: @Composable () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = MutedText, fontSize = 12.sp, modifier = Modifier.width(100.dp))
        value()
    }
}

// ─── Format toolbar buttons ─────────────────────────────────────────────────
@Composable
private fun FmFormatBtn(
    label: String,
    fontWeight: FontWeight = FontWeight.Normal,
    fontStyle: FontStyle = FontStyle.Normal,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = MutedText, fontSize = if (label.length > 2) 10.sp else 13.sp,
            fontWeight = fontWeight, fontStyle = fontStyle)
    }
}

@Composable
private fun FmToolDivider() {
    Box(modifier = Modifier.height(18.dp).width(1.dp).background(BorderDark))
}
