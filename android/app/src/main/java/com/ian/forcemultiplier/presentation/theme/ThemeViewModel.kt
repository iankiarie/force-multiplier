package com.ian.forcemultiplier.presentation.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ian.forcemultiplier.core.theme.AppTheme
import com.ian.forcemultiplier.core.theme.ThemePreferenceManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val themePreferenceManager: ThemePreferenceManager
) : ViewModel() {

    val appTheme = themePreferenceManager.themeFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AppTheme.SYSTEM
    )

    fun setTheme(theme: AppTheme) {
        viewModelScope.launch {
            themePreferenceManager.setTheme(theme)
        }
    }
}
