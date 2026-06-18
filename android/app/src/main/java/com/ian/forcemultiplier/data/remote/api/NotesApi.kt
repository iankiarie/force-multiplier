package com.ian.forcemultiplier.data.remote.api

import com.ian.forcemultiplier.data.remote.dto.NoteDto
import retrofit2.Response
import retrofit2.http.*

interface NotesApi {
    @GET("notes/")
    suspend fun getNotes(): Response<List<NoteDto>>

    @GET("notes/{noteId}")
    suspend fun getNoteById(@Path("noteId") noteId: Int): Response<NoteDto>

    @POST("notes/")
    suspend fun createNote(@Body noteDto: NoteDto): Response<NoteDto>

    @DELETE("notes/{noteId}")
    suspend fun deleteNote(@Path("noteId") noteId: Int): Response<Unit>
}
