package com.ian.forcemultiplier.domain.repository

import com.ian.forcemultiplier.data.remote.dto.NoteDto
import com.ian.forcemultiplier.util.Resource
import kotlinx.coroutines.flow.Flow

interface NoteRepository {
    suspend fun getNotes(): Flow<Resource<List<NoteDto>>>
    suspend fun createNote(title: String, content: String, tags: String?): Flow<Resource<NoteDto>>
}
