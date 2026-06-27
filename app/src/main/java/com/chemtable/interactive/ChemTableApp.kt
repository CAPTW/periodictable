package com.chemtable.interactive

import android.app.Application
import com.chemtable.interactive.core.util.StartupTrace
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ChemTableApp : Application() {
    override fun onCreate() {
        super.onCreate()
        StartupTrace.configure(this)
        StartupTrace.mark("ChemTableApp.onCreate")
    }
}
