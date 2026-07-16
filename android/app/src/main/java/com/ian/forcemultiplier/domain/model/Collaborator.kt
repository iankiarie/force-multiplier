package com.ian.forcemultiplier.domain.model

enum class CollaborationRole(
    val key: String,
    val label: String,
    val description: String
) {
    OWNER    ("owner",     "Owner",     "Full access — created this note"),
    EDITOR   ("editor",    "Editor",    "Can read and edit the note"),
    COMMENTER("commenter", "Commenter", "Can read and add comments"),
    VIEWER   ("viewer",    "Viewer",    "Can read only");

    companion object {
        fun fromKey(key: String): CollaborationRole =
            entries.firstOrNull { it.key == key } ?: VIEWER

        /** Roles that can be assigned when inviting (owner is not grantable) */
        val grantable: List<CollaborationRole> = listOf(EDITOR, COMMENTER, VIEWER)
    }
}

data class Collaborator(
    val id: String,
    val noteId: String,
    val userId: String?,
    val invitedEmail: String?,
    val role: CollaborationRole,
    val invitedBy: String?,
    val accepted: Boolean,
    val createdAt: String?
) {
    val displayLabel: String get() = invitedEmail ?: userId?.take(8) ?: "Unknown"
}
