package com.ian.forcemultiplier.core.featureflag

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class Feature(val key: String) {
    AI_INSIGHTS("enable_ai_insights"),
    REALTIME_UPDATES("enable_realtime"),
    REWARDS_SYSTEM("enable_rewards"),
    ADVANCED_ANALYTICS("enable_analytics")
}

@Singleton
class FeatureManager @Inject constructor() {
    private val _flags = MutableStateFlow<Map<Feature, Boolean>>(
        Feature.entries.associateWith { false }
    )
    val flags = _flags.asStateFlow()

    fun isEnabled(feature: Feature): Boolean {
        return _flags.value[feature] ?: false
    }

    fun updateFlags(newFlags: Map<Feature, Boolean>) {
        _flags.value = _flags.value + newFlags
    }
}
