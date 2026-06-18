package com.ian.forcemultiplier

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.ian.forcemultiplier.core.theme.AppTheme
import com.ian.forcemultiplier.core.theme.ForceMultiplierTheme
import com.ian.forcemultiplier.presentation.navigation.AppNavHost
import com.ian.forcemultiplier.presentation.theme.ThemeViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val themeViewModel: ThemeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val appTheme by themeViewModel.appTheme.collectAsState(initial = AppTheme.SYSTEM)

            ForceMultiplierTheme(appTheme = appTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavHost(onThemeChange = { themeViewModel.setTheme(it) })
                }
            }
        }
    }
}
