package com.ian.forcemultiplier.core.event

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EventBus @Inject constructor() {
    private val _events = MutableSharedFlow<DomainEvent>()
    val events = _events.asSharedFlow()

    suspend fun publish(event: DomainEvent) {
        _events.emit(event)
    }
}
