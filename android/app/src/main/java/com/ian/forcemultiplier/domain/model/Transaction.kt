package com.ian.forcemultiplier.domain.model

/**
 * Transaction domain model representing a transaction in the wallet.
 */
data class Transaction(
    val id: Int,
    val amount: Int, // Can be negative or positive
    val type: String, // bet_placed, bet_win, bonus, adjustment
    val description: String?,
    val createdAt: Long, // Timestamp in milliseconds
    val userId: Int
)