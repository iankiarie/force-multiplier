package com.ian.forcemultiplier.data.repository

import com.ian.forcemultiplier.data.local.dao.NoteDao
import com.ian.forcemultiplier.data.remote.api.NotesApi
import com.ian.forcemultiplier.data.remote.dto.NoteDto
import com.ian.forcemultiplier.domain.repository.NoteRepository
import com.ian.forcemultiplier.util.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class NoteRepositoryImpl @Inject constructor(
    private val api: NotesApi,
    private val dao: NoteDao
) : NoteRepository {

    override suspend fun getNotes(): Flow<Resource<List<NoteDto>>> = flow {
        emit(Resource.Loading())
        try {
            val response = api.getNotes()
            if (response.isSuccessful) {
                emit(Resource.Success(response.body() ?: emptyList()))
            } else {
                emit(Resource.Error(response.message()))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Unknown error"))
        }
    }

    override suspend fun createNote(title: String, content: String, tags: String?): Flow<Resource<NoteDto>> = flow {
        emit(Resource.Loading())
        try {
            // Note: In a real app, userId should probably be handled by backend auth session
            val noteDto = NoteDto(id = 0, title = title, content = content, tags = tags, createdAt = System.currentTimeMillis(), updatedAt = null, userId = 0)
            val response = api.createNote(noteDto)
            if (response.isSuccessful) {
                response.body()?.let {
                    emit(Resource.Success(it))
                } ?: emit(Resource.Error("Empty response"))
            } else {
                emit(Resource.Error(response.message()))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Unknown error"))
        }
    }
}
