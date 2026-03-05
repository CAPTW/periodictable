package com.chemtable.interactive

import android.app.Application
import com.chemtable.interactive.data.prepopulate.DatabaseSeeder
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class ChemTableApp : Application() {
    @Inject
    lateinit var seeder: DatabaseSeeder

    private val appScope: CoroutineScope by lazy {
        CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }

    override fun onCreate() {
        super.onCreate()
        appScope.launch {
            seeder.seedIfNeeded()
        }
    }
}
