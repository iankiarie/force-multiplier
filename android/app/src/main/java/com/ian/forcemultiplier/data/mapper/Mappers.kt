package com.ian.forcemultiplier.data.mapper

import com.ian.forcemultiplier.data.local.entity.NoteEntity
import com.ian.forcemultiplier.data.local.entity.FolderEntity
import com.ian.forcemultiplier.data.local.entity.UserEntity
import com.ian.forcemultiplier.data.remote.dto.NoteDto
import com.ian.forcemultiplier.data.remote.dto.UserDto
import com.ian.forcemultiplier.domain.model.Folder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

fun UserDto.toUserEntity(): UserEntity {
    return UserEntity(
        id = id,
        email = email,
        username = username,
        fullName = fullName,
        isActive = isActive,
        role = role ?: "user",
        points = points,
        streak = streak,
        accuracy = accuracy,
        dailyPoints = dailyPoints,
        rank = rank,
        location = location
        // createdAt and updatedAt can be mapped if needed, but they are Long in Entity
    )
}

fun UserEntity.toUserDto(): UserDto {
    return UserDto(
        id = id,
        email = email,
        username = username,
        fullName = fullName,
        isActive = isActive,
        role = role,
        points = points,
        streak = streak,
        accuracy = accuracy,
        dailyPoints = dailyPoints,
        rank = rank,
        location = location
    )
}

fun NoteDto.toNoteEntity(): NoteEntity {
    return NoteEntity(
        // Never persist an empty PK; empty IDs caused new notes to overwrite old ones.
        id = id ?: java.util.UUID.randomUUID().toString(),
        title = title,
        content = content,
        tags = tags,
        preview = preview,
        parentId = parentId,
        icon = icon,
        coverUrl = coverUrl,
        isBookmarked = isBookmarked,
        folderId = folderId,
        createdAt = createdAt?.let { parseIsoTimestamp(it) } ?: System.currentTimeMillis(),
        updatedAt = updatedAt?.let { parseIsoTimestamp(it) },
        userId = userId
    )
}

fun NoteEntity.toNoteDto(): NoteDto {
    return NoteDto(
        id = id,
        title = title,
        content = content,
        tags = tags,
        preview = preview,
        parentId = parentId,
        icon = icon,
        coverUrl = coverUrl,
        isBookmarked = isBookmarked,
        folderId = folderId,
        createdAt = formatIsoTimestamp(createdAt),
        updatedAt = updatedAt?.let { formatIsoTimestamp(it) },
        userId = userId
    )
}

fun FolderEntity.toFolder(): Folder {
    return Folder(
        id = id,
        name = name,
        icon = icon,
        tagFilter = tagFilter
    )
}

private fun parseIsoTimestamp(raw: String): Long {
    val formats = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
        "yyyy-MM-dd"
    )

    for (pattern in formats) {
        try {
            val parser = SimpleDateFormat(pattern, Locale.getDefault()).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val parsed = parser.parse(raw)
            if (parsed != null) return parsed.time
        } catch (_: Exception) {
            // Try the next format.
        }
    }

    return System.currentTimeMillis()
}

private fun formatIsoTimestamp(timestamp: Long): String {
    return SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }.format(Date(timestamp))
}

