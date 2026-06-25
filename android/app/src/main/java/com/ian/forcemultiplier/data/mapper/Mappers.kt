package com.ian.forcemultiplier.data.mapper

import com.ian.forcemultiplier.data.local.entity.NoteEntity
import com.ian.forcemultiplier.data.local.entity.UserEntity
import com.ian.forcemultiplier.data.remote.dto.NoteDto
import com.ian.forcemultiplier.data.remote.dto.UserDto

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
        id = id ?: "",
        title = title,
        content = content,
        tags = tags,
        preview = preview,
        parentId = parentId,
        icon = icon,
        coverUrl = coverUrl,
        isBookmarked = isBookmarked,
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
        userId = userId
    )
}
