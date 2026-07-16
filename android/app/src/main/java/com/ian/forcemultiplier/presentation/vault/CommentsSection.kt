package com.ian.forcemultiplier.presentation.vault

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.TextStyle
import com.ian.forcemultiplier.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ian.forcemultiplier.core.theme.FMColors
import com.ian.forcemultiplier.domain.model.CollaborationRole
import com.ian.forcemultiplier.domain.model.NoteComment
import com.ian.forcemultiplier.presentation.vault.viewmodel.CollaborationViewModel
import com.ian.forcemultiplier.util.Resource
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

// ── Palette ────────────────────────────────────────────────────────────────────
private val SurfaceDark  = FMColors.DarkSurface
private val Surface2Dark = FMColors.DarkSurface2
private val BorderDark   = FMColors.DarkOutline
private val MutedText    = FMColors.DarkMuted
private val GreenPrimary = FMColors.Primary
private val OnSurface    = FMColors.DarkOnSurface

@Composable
fun CommentsSection(
    noteId: String,
    currentUserId: String?,
    myRole: CollaborationRole?,
    viewModel: CollaborationViewModel
) {
    val commentsState by viewModel.comments.collectAsState()
    var commentInput  by remember { mutableStateOf("") }

    val canComment = myRole == null /* owner */ ||
            myRole == CollaborationRole.OWNER ||
            myRole == CollaborationRole.EDITOR ||
            myRole == CollaborationRole.COMMENTER

    Column(modifier = Modifier.fillMaxWidth()) {

        // ── Section header ────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "COMMENTS",
                color = MutedText,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.8.sp
            )
            val count = (commentsState as? Resource.Success)?.data?.size ?: 0
            if (count > 0) {
                Text("$count", color = MutedText, fontSize = 11.sp)
            }
        }

        // ── Comment list ──────────────────────────────────────────────────
        when (val state = commentsState) {
            is Resource.Loading -> {
                Box(Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = GreenPrimary, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                }
            }
            is Resource.Error -> {
                Text(state.message ?: "Could not load comments", color = FMColors.Error, fontSize = 13.sp)
            }
            is Resource.Success -> {
                val comments = state.data.orEmpty().filter { !it.resolved }
                val resolved = state.data.orEmpty().filter { it.resolved }

                if (comments.isEmpty() && resolved.isEmpty()) {
                    Text(
                        if (canComment) "No comments yet. Be the first." else "No comments.",
                        color = MutedText,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                // Active comments
                comments.forEach { comment ->
                    CommentItem(
                        comment       = comment,
                        isAuthor      = comment.userId == currentUserId,
                        isNoteOwner   = myRole == null || myRole == CollaborationRole.OWNER,
                        onResolve     = { viewModel.resolveComment(comment.id) },
                        onDelete      = { viewModel.deleteComment(comment.id) }
                    )
                    Spacer(Modifier.height(8.dp))
                }

                // Resolved comments (collapsible)
                if (resolved.isNotEmpty()) {
                    var showResolved by remember { mutableStateOf(false) }
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showResolved = !showResolved }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            if (showResolved) "- ${resolved.size} resolved" else "+ ${resolved.size} resolved",
                            color = MutedText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    AnimatedVisibility(
                        visible = showResolved,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column {
                            resolved.forEach { comment ->
                                CommentItem(
                                    comment     = comment,
                                    isAuthor    = comment.userId == currentUserId,
                                    isNoteOwner = myRole == null || myRole == CollaborationRole.OWNER,
                                    onResolve   = {},
                                    onDelete    = { viewModel.deleteComment(comment.id) },
                                    muted       = true
                                )
                                Spacer(Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }
        }

        // ── Comment input ─────────────────────────────────────────────────
        if (canComment) {
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Surface2Dark)
                    .border(1.dp, BorderDark, RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicTextField(
                    value = commentInput,
                    onValueChange = { commentInput = it },
                    modifier = Modifier.weight(1f),
                    textStyle = TextStyle(color = OnSurface, fontSize = 13.sp, lineHeight = 20.sp),
                    cursorBrush = SolidColor(GreenPrimary),
                    decorationBox = { inner ->
                        if (commentInput.isEmpty()) Text("Add a comment…", color = MutedText, fontSize = 13.sp)
                        inner()
                    }
                )
                Spacer(Modifier.width(8.dp))
                AnimatedVisibility(visible = commentInput.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(GreenPrimary)
                            .clickable {
                                viewModel.addComment(noteId, commentInput)
                                commentInput = ""
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_add),
                            contentDescription = "Send",
                            tint = Color.Black,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

// ── Single comment item ────────────────────────────────────────────────────────
@Composable
private fun CommentItem(
    comment: NoteComment,
    isAuthor: Boolean,
    isNoteOwner: Boolean,
    onResolve: () -> Unit,
    onDelete: () -> Unit,
    muted: Boolean = false
) {
    val initials = comment.userId.take(2).uppercase()
    val dateStr  = comment.createdAt?.let { formatCommentDate(it) } ?: ""
    val alpha    = if (muted) 0.5f else 1f

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(GreenPrimary.copy(0.25f * alpha), FMColors.FMGradMid.copy(0.25f * alpha))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(initials, color = OnSurface.copy(alpha), fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(initials, color = OnSurface.copy(alpha), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text(dateStr, color = MutedText.copy(alpha), fontSize = 11.sp)
            }
            Spacer(Modifier.height(3.dp))
            Text(comment.content, color = OnSurface.copy(alpha * 0.9f), fontSize = 13.sp, lineHeight = 19.sp)

            // Actions (visible for unresolved only)
            if (!muted) {
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (isNoteOwner || isAuthor) {
                        Text(
                            "Resolve",
                            color = GreenPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.clickable { onResolve() }
                        )
                    }
                    if (isAuthor || isNoteOwner) {
                        Text(
                            "Delete",
                            color = FMColors.Error.copy(0.7f),
                            fontSize = 11.sp,
                            modifier = Modifier.clickable { onDelete() }
                        )
                    }
                }
            }
        }
    }
}

private fun formatCommentDate(raw: String): String {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        val date = sdf.parse(raw.take(19)) ?: return ""
        SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(date)
    } catch (_: Exception) { "" }
}
