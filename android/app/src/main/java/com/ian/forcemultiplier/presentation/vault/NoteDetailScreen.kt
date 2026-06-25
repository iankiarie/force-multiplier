package com.ian.forcemultiplier.presentation.vault

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.ian.forcemultiplier.R
import com.ian.forcemultiplier.data.remote.dto.NoteDto
import com.ian.forcemultiplier.presentation.vault.viewmodel.VaultViewModel
import com.ian.forcemultiplier.util.Resource
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
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

private enum class SaveStatus { Idle, Saving, Saved, Error }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteDetailScreen(
    navController: NavController,
    noteId: String? = null,
    parentId: String? = null,
    template: String? = null,
    viewModel: VaultViewModel = hiltViewModel()
) {
    // ─── State ──────────────────────────────────────────────────────────────
    var title by remember { mutableStateOf("") }
    var contentState by remember { mutableStateOf(TextFieldValue("")) }
    var tags by remember { mutableStateOf("") }
    var newTagInput by remember { mutableStateOf("") }
    var showTagInput by remember { mutableStateOf(false) }

    var savedId by rememberSaveable { mutableStateOf<String?>(if (noteId != "new" && noteId != null) noteId else null) }
    var saveStatus by remember { mutableStateOf(SaveStatus.Idle) }
    var isInitialLoad by remember { mutableStateOf(true) }

    val currentNoteState by viewModel.currentNoteState.collectAsState()
    val allNotesState by viewModel.notesState.collectAsState()

    val subNotes = remember(allNotesState, savedId) {
        if (allNotesState is Resource.Success) {
            allNotesState.data?.filter { it.parentId == savedId } ?: emptyList()
        } else emptyList()
    }

    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    // ─── Load existing note ─────────────────────────────────────────────────
    LaunchedEffect(noteId) {
        if (noteId != null && noteId != "new") {
            viewModel.getNoteById(noteId)
        } else {
            viewModel.clearCurrentNote()
            // Pre-fill from template if provided
            if (template != null) {
                val decoded = try { java.net.URLDecoder.decode(template, "UTF-8") } catch (_: Exception) { template }
                val parts = decoded.split("|", limit = 2)
                if (parts.isNotEmpty()) title = parts[0]
                if (parts.size > 1) contentState = TextFieldValue(parts[1])
            }
            isInitialLoad = false
        }
    }

    // ─── Populate fields from loaded note + watch save results ───────────────
    LaunchedEffect(currentNoteState) {
        val state = currentNoteState
        if (state is Resource.Success && state.data != null) {
            if (isInitialLoad) {
                title = state.data.title
                contentState = TextFieldValue(state.data.content ?: "")
                tags = state.data.tags ?: ""
                isInitialLoad = false
            } else {
                // This is a save result — update our savedId
                if (state.data.id != null) savedId = state.data.id
                saveStatus = SaveStatus.Saved
            }
        } else if (state is Resource.Error && !isInitialLoad) {
            saveStatus = SaveStatus.Error
        }
    }

    // ─── Auto-save with debounce ─────────────────────────────────────────────
    LaunchedEffect(Unit) {
        snapshotFlow { Triple(title, contentState.text, tags) }
            .debounce(1500L)
            .collect { (t, c, tgs) ->
                if (!isInitialLoad && (t.isNotBlank() || c.isNotBlank())) {
                    saveStatus = SaveStatus.Saving
                    val dto = NoteDto(
                        id = savedId,
                        title = t.ifBlank { "Untitled" },
                        content = c.takeIf { it.isNotBlank() },
                        tags = tgs.takeIf { it.isNotBlank() },
                        parentId = parentId,
                        userId = null
                    )
                    if (savedId == null) {
                        viewModel.createNote(dto)
                    } else {
                        viewModel.updateNote(dto)
                    }
                }
            }
    }

    // ─── Formatting helpers ──────────────────────────────────────────────────
    fun wrapSelection(wrapper: String) {
        val sel = contentState.selection
        val text = contentState.text
        val newText: String
        val newCursor: Int
        if (sel.collapsed) {
            newText = text.substring(0, sel.start) + wrapper + wrapper + text.substring(sel.start)
            newCursor = sel.start + wrapper.length
        } else {
            val selected = text.substring(sel.start, sel.end)
            newText = text.substring(0, sel.start) + wrapper + selected + wrapper + text.substring(sel.end)
            newCursor = sel.end + wrapper.length * 2
        }
        contentState = contentState.copy(text = newText, selection = TextRange(newCursor))
    }

    fun insertLinePrefix(prefix: String) {
        val text = contentState.text
        val cursor = contentState.selection.start
        val lineStart = text.lastIndexOf('\n', cursor - 1) + 1
        val newText = text.substring(0, lineStart) + prefix + text.substring(lineStart)
        contentState = contentState.copy(
            text = newText,
            selection = TextRange(cursor + prefix.length)
        )
    }

    // ─── UI ─────────────────────────────────────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ── Top bar ────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_home), // back arrow
                        contentDescription = "Back",
                        tint = MutedText,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Save status indicator
                AnimatedVisibility(
                    visible = saveStatus != SaveStatus.Idle,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Text(
                        text = when (saveStatus) {
                            SaveStatus.Saving -> "Saving..."
                            SaveStatus.Saved  -> "Saved"
                            SaveStatus.Error  -> "Error saving"
                            SaveStatus.Idle   -> ""
                        },
                        color = when (saveStatus) {
                            SaveStatus.Saved  -> GreenPrimary
                            SaveStatus.Error  -> Color(0xFFFF4757)
                            else              -> MutedText
                        },
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }

                // Delete button (only for existing notes)
                if (savedId != null) {
                    IconButton(onClick = {
                        scope.launch {
                            viewModel.deleteNote(savedId!!)
                            navController.popBackStack()
                        }
                    }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_visibility_off),
                            contentDescription = "Delete",
                            tint = Color(0xFFFF4757).copy(alpha = 0.7f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // ── Scrollable body ────────────────────────────────────────
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 24.dp)
            ) {
                // Title
                BasicTextField(
                    value = title,
                    onValueChange = { title = it },
                    textStyle = TextStyle(
                        color = OnSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp,
                        lineHeight = 36.sp,
                        letterSpacing = (-0.5).sp
                    ),
                    cursorBrush = SolidColor(GreenPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    decorationBox = { inner ->
                        if (title.isEmpty()) {
                            Text(
                                "Untitled",
                                style = TextStyle(
                                    color = MutedText.copy(alpha = 0.4f),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 28.sp
                                )
                            )
                        }
                        inner()
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Tags row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val tagList = tags.split(",").map { it.trim() }.filter { it.isNotBlank() }
                    tagList.forEach { tag ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(GreenPrimary.copy(alpha = 0.12f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(tag, color = GreenPrimary, fontSize = 11.sp)
                        }
                    }
                    // Add tag
                    if (showTagInput) {
                        BasicTextField(
                            value = newTagInput,
                            onValueChange = { newTagInput = it },
                            textStyle = TextStyle(color = OnSurface, fontSize = 12.sp),
                            cursorBrush = SolidColor(GreenPrimary),
                            singleLine = true,
                            modifier = Modifier.width(80.dp),
                            decorationBox = { inner ->
                                if (newTagInput.isEmpty()) Text("tag...", color = MutedText, fontSize = 12.sp)
                                inner()
                            }
                        )
                        TextButton(
                            onClick = {
                                if (newTagInput.isNotBlank()) {
                                    tags = if (tags.isBlank()) newTagInput.trim()
                                    else "$tags, ${newTagInput.trim()}"
                                }
                                newTagInput = ""
                                showTagInput = false
                            },
                            contentPadding = PaddingValues(horizontal = 4.dp),
                            modifier = Modifier.height(24.dp)
                        ) { Text("Add", color = GreenPrimary, fontSize = 12.sp) }
                    } else {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(SurfaceDark)
                                .clickable { showTagInput = true }
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text("+ tag", color = MutedText, fontSize = 11.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Metadata row
                Text(
                    text = buildString {
                        append(SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date()))
                        if (saveStatus == SaveStatus.Saved) append("  ·  Edited just now")
                    },
                    color = MutedText,
                    fontSize = 12.sp
                )

                // Divider
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 16.dp),
                    color = BorderDark
                )

                // Content editor
                BasicTextField(
                    value = contentState,
                    onValueChange = { contentState = it },
                    textStyle = TextStyle(
                        color = OnSurface,
                        fontSize = 15.sp,
                        lineHeight = 24.sp
                    ),
                    cursorBrush = SolidColor(GreenPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 240.dp),
                    decorationBox = { inner ->
                        if (contentState.text.isEmpty()) {
                            Text(
                                "Start writing...",
                                style = TextStyle(
                                    color = MutedText.copy(alpha = 0.4f),
                                    fontSize = 15.sp,
                                    lineHeight = 24.sp
                                )
                            )
                        }
                        inner()
                    }
                )

                // Sub-pages section
                if (subNotes.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(40.dp))
                    HorizontalDivider(color = BorderDark)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Sub-pages",
                        color = MutedText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.8.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    subNotes.forEach { sub ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { navController.navigate("note_detail/${sub.id}") }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_note),
                                contentDescription = null,
                                tint = MutedText,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                sub.title.ifBlank { "Untitled" },
                                color = OnSurface,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                // Add sub-page
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { navController.navigate("note_detail/new?parentId=$savedId") }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_add),
                        contentDescription = null,
                        tint = MutedText,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Add sub-page", color = MutedText, fontSize = 14.sp)
                }

                Spacer(modifier = Modifier.height(80.dp))
            }

            // ── Format toolbar (pinned at bottom) ─────────────────────
            Column {
                HorizontalDivider(color = BorderDark)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceDark)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FormatButton("B", fontWeight = FontWeight.Bold)    { wrapSelection("**") }
                    FormatButton("I", fontStyle = FontStyle.Italic)    { wrapSelection("_") }
                    FormatButton("H1")  { insertLinePrefix("# ") }
                    FormatButton("H2")  { insertLinePrefix("## ") }
                    FormatDivider()
                    FormatButton(">")   { insertLinePrefix("> ") }
                    FormatButton("-")   { insertLinePrefix("- ") }
                    FormatButton("1.")  { insertLinePrefix("1. ") }
                    FormatDivider()
                    FormatButton("</>") { wrapSelection("`") }
                    FormatButton("---") { contentState = contentState.copy(
                        text = contentState.text.substring(0, contentState.selection.start) +
                                "\n---\n" + contentState.text.substring(contentState.selection.start),
                        selection = TextRange(contentState.selection.start + 5)
                    )}
                }
            }
        }
    }
}

@Composable
private fun FormatButton(
    label: String,
    fontWeight: FontWeight = FontWeight.Normal,
    fontStyle: FontStyle = FontStyle.Normal,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = MutedText,
            fontSize = if (label.length > 2) 10.sp else 13.sp,
            fontWeight = fontWeight,
            fontStyle = fontStyle
        )
    }
}

@Composable
private fun FormatDivider() {
    Box(
        modifier = Modifier
            .height(20.dp)
            .width(1.dp)
            .background(BorderDark)
    )
}
