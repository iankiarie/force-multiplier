package com.ian.forcemultiplier.domain.repository

import com.ian.forcemultiplier.domain.model.User

/**
 * Repository for user data operations.
 */
interface UserRepository {
    suspend fun getUserById(userId: Int): User?
    suspend fun getUserByEmail(email: String): User?
    suspend fun getUserByUsername(username: String): User?
    suspend fun getAllUsers(): List<User>
    suspend fun createUser(user: User): User
    suspend fun updateUser(user: User): User
    suspend fun deleteUser(userId: Int): Boolean
}