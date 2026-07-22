package com.ian.forcemultiplier.presentation.vault

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ian.forcemultiplier.core.theme.FMColors
import com.ian.forcemultiplier.domain.model.Collaborator
import com.ian.forcemultiplier.domain.model.CollaborationRole
import com.ian.forcemultiplier.presentation.vault.viewmodel.CollaborationViewModel
import com.ian.forcemultiplier.util.Resource

// ── Palette (matches NoteDetailScreen) ────────────────────────────────────────
private val BgDark       = FMColors.DarkBg
private val SurfaceDark  = FMColors.DarkSurface
private val Surface2Dark = FMColors.DarkSurface2
private val BorderDark   = FMColors.DarkOutline
private val MutedText    = FMColors.DarkMuted
private val GreenPrimary = FMColors.Primary
private val OnSurface    = FMColors.DarkOnSurface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollaborationSheet(
    noteId: String,
    isOwner: Boolean,
    viewModel: CollaborationViewModel,
    onDismiss: () -> Unit
) {
    val collaboratorsState by viewModel.collaborators.collectAsState()
    val inviteResult       by viewModel.inviteResult.collectAsState()
    val sheetState         = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var email              by remember { mutableStateOf("") }
    var selectedRole       by remember { mutableStateOf(CollaborationRole.VIEWER) }
    var roleMenuExpanded   by remember { mutableStateOf(false) }
    val focusManager       = LocalFocusManager.current

    // Clear result when user types again
    LaunchedEffect(email) { viewModel.clearInviteResult() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SurfaceDark,
        dragHandle = {
            Box(Modifier.padding(top = 10.dp, bottom = 4.dp)) {
                Box(Modifier.width(36.dp).height(4.dp).background(BorderDark, RoundedCornerShape(2.dp)))
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 48.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Share", color = OnSurface, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("Invite people to this note", color = MutedText, fontSize = 13.sp)
                }
                // Global link access chip (visual only for now)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Surface2Dark)
                        .border(1.dp, BorderDark, RoundedCornerShape(20.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("Private", color = MutedText, fontSize = 12.sp)
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Invite form (owner only) ───────────────────────────────────
            if (isOwner) {
                Text("INVITE", color = MutedText, fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold, letterSpacing = 0.8.sp)
                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Email field
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        placeholder = { Text("Email address", color = MutedText, fontSize = 13.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor   = GreenPrimary,
                            unfocusedBorderColor = BorderDark,
                            focusedTextColor     = OnSurface,
                            unfocusedTextColor   = OnSurface,
                            cursorColor          = GreenPrimary,
                            focusedContainerColor   = Surface2Dark,
                            unfocusedContainerColor = Surface2Dark,
                        ),
                        shape = RoundedCornerShape(10.dp),
                        textStyle = LocalTextStyle.current.copy(fontSize = 13.sp)
                    )

                    // Role picker
                    ExposedDropdownMenuBox(
                        expanded = roleMenuExpanded,
                        onExpandedChange = { roleMenuExpanded = !roleMenuExpanded }
                    ) {
                        Box(
                            modifier = Modifier
                                .menuAnchor(type = MenuAnchorType.PrimaryNotEditable)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Surface2Dark)
                                .border(1.dp, BorderDark, RoundedCornerShape(10.dp))
                                .padding(horizontal = 12.dp, vertical = 14.dp)
                        ) {
                            Text(selectedRole.label, color = OnSurface, fontSize = 13.sp)
                        }
                        ExposedDropdownMenu(
                            expanded = roleMenuExpanded,
                            onDismissRequest = { roleMenuExpanded = false },
                            containerColor = SurfaceDark
                        ) {
                            CollaborationRole.grantable.forEach { role ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(role.label, color = OnSurface, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                            Text(role.description, color = MutedText, fontSize = 11.sp)
                                        }
                                    },
                                    onClick = {
                                        selectedRole = role
                                        roleMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Feedback message
                val result = inviteResult
                AnimatedVisibility(visible = result != null, enter = fadeIn(), exit = fadeOut()) {
                    when (result) {
                        is Resource.Loading -> Text("Sending invite…", color = MutedText, fontSize = 12.sp)
                        is Resource.Success -> Text("Invite sent!", color = GreenPrimary, fontSize = 12.sp)
                        is Resource.Error   -> Text(result.message ?: "Failed to send invite.", color = FMColors.Error, fontSize = 12.sp)
                        else -> {}
                    }
                }

                Button(
                    onClick = {
                        focusManager.clearFocus()
                        viewModel.inviteCollaborator(noteId, email, selectedRole)
                        email = ""
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GreenPrimary,
                        contentColor   = Color.Black
                    ),
                    enabled = email.contains("@")
                ) {
                    Text("Invite", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                Spacer(Modifier.height(24.dp))
            }

            // ── People with access ─────────────────────────────────────────
            Text("PEOPLE WITH ACCESS", color = MutedText, fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold, letterSpacing = 0.8.sp)
            Spacer(Modifier.height(10.dp))

            when (val state = collaboratorsState) {
                is Resource.Loading -> {
                    Box(Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = GreenPrimary, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                    }
                }
                is Resource.Error -> {
                    Text(state.message ?: "Could not load collaborators", color = FMColors.Error, fontSize = 13.sp)
                }
                is Resource.Success -> {
                    val list = state.data.orEmpty()
                    if (list.isEmpty()) {
                        Text("Only you have access.", color = MutedText, fontSize = 13.sp)
                    } else {
                        list.forEach { collab ->
                            CollaboratorRow(
                                collaborator = collab,
                                isOwner      = isOwner,
                                onRoleChange = { newRole -> viewModel.updateRole(collab.id, newRole) },
                                onRemove     = { viewModel.removeCollaborator(collab.id) }
                            )
                            Spacer(Modifier.height(4.dp))
                        }
                    }
                }
            }
        }
    }
}

// ── Single collaborator row ────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CollaboratorRow(
    collaborator: Collaborator,
    isOwner: Boolean,
    onRoleChange: (CollaborationRole) -> Unit,
    onRemove: () -> Unit
) {
    var roleMenuExpanded by remember { mutableStateOf(false) }
    val initials = collaborator.displayLabel.take(2).uppercase()
    val isPending = !collaborator.accepted

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Surface2Dark)
            .border(1.dp, BorderDark, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(GreenPrimary.copy(0.3f), FMColors.FMGradMid.copy(0.3f)))),
            contentAlignment = Alignment.Center
        ) {
            Text(initials, color = OnSurface, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    collaborator.displayLabel,
                    color = OnSurface,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
                if (isPending) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(FMColors.Accent.copy(0.15f))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text("Pending", color = FMColors.Accent, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // Role chip / picker
        if (isOwner && collaborator.role != CollaborationRole.OWNER) {
            ExposedDropdownMenuBox(
                expanded = roleMenuExpanded,
                onExpandedChange = { roleMenuExpanded = !roleMenuExpanded }
            ) {
                Box(
                    modifier = Modifier
                        .menuAnchor(type = MenuAnchorType.PrimaryNotEditable)
                        .clip(RoundedCornerShape(6.dp))
                        .background(SurfaceDark)
                        .border(1.dp, BorderDark, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(collaborator.role.label, color = MutedText, fontSize = 11.sp)
                }
                ExposedDropdownMenu(
                    expanded = roleMenuExpanded,
                    onDismissRequest = { roleMenuExpanded = false },
                    containerColor = SurfaceDark
                ) {
                    CollaborationRole.grantable.forEach { role ->
                        DropdownMenuItem(
                            text = { Text(role.label, color = OnSurface, fontSize = 13.sp) },
                            onClick = { onRoleChange(role); roleMenuExpanded = false }
                        )
                    }
                    HorizontalDivider(color = BorderDark)
                    DropdownMenuItem(
                        text = { Text("Remove", color = FMColors.Error, fontSize = 13.sp) },
                        onClick = { onRemove(); roleMenuExpanded = false }
                    )
                }
            }
        } else {
            RoleChip(collaborator.role)
        }
    }
}

@Composable
private fun RoleChip(role: CollaborationRole) {
    val (bg, fg) = when (role) {
        CollaborationRole.OWNER     -> Pair(GreenPrimary.copy(0.15f), GreenPrimary)
        CollaborationRole.EDITOR    -> Pair(FMColors.Info.copy(0.12f), FMColors.Info)
        CollaborationRole.COMMENTER -> Pair(FMColors.Accent.copy(0.12f), FMColors.Accent)
        CollaborationRole.VIEWER    -> Pair(Surface2Dark, MutedText)
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(role.label, color = fg, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}
