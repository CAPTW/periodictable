package com.chemtable.interactive

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import com.chemtable.interactive.core.designsystem.theme.ChemTableTheme
import com.chemtable.interactive.core.util.StartupTrace
import com.chemtable.interactive.data.prepopulate.AppStartupSeeder
import com.chemtable.interactive.navigation.ChemTableNavHost
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        StartupTrace.mark("MainActivity.onCreate started")
        StartupTrace.measure("MainActivity.super.onCreate") {
            super.onCreate(savedInstanceState)
        }
        StartupTrace.measure("MainActivity.setContent") {
            setContent {
                LaunchedEffect(Unit) {
                    StartupTrace.mark("MainActivity first composition committed")
                    AppStartupSeeder.start(applicationContext)
                }
                ChemTableTheme {
                    ChemTableNavHost()
                }
            }
        }
        StartupTrace.mark("MainActivity.onCreate finished")
    }
}
