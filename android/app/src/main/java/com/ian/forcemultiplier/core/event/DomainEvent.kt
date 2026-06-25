package com.ian.forcemultiplier.core.event

import java.util.UUID

/**
 * Base interface for all domain events in the system.
 */
interface DomainEvent {
    val eventId: UUID
    val timestamp: Long
}

sealed class PredictionEvent : DomainEvent {
    override val eventId: UUID = UUID.randomUUID()
    override val timestamp: Long = System.currentTimeMillis()

    data class PredictionCreated(val predictionId: String) : PredictionEvent()
    data class PredictionResolved(val predictionId: String, val winnerOptionId: String) : PredictionEvent()
}

sealed class WalletEvent : DomainEvent {
    override val eventId: UUID = UUID.randomUUID()
    override val timestamp: Long = System.currentTimeMillis()

    data class PointsAwarded(val userId: String, val amount: Int, val reason: String) : WalletEvent()
    data class TransactionCompleted(val transactionId: String) : WalletEvent()
}

sealed class RecognitionEvent : DomainEvent {
    override val eventId: UUID = UUID.randomUUID()
    override val timestamp: Long = System.currentTimeMillis()

    data class BadgeUnlocked(val userId: String, val badgeId: String) : RecognitionEvent()
}
