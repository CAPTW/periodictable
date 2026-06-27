package com.chemtable.interactive.data.prepopulate

import android.content.Context
import android.util.Log
import com.chemtable.interactive.core.util.StartupTrace
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

object AppStartupSeeder {
    private val started = AtomicBoolean(false)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun start(context: Context) {
        if (!started.compareAndSet(false, true)) {
            StartupTrace.mark("DatabaseSeeder already started")
            return
        }

        val appContext = context.applicationContext
        scope.launch {
            try {
                val seeder = StartupTrace.measure("DatabaseSeeder entry point") {
                    EntryPointAccessors.fromApplication(
                        appContext,
                        SeederEntryPoint::class.java
                    ).databaseSeeder()
                }
                StartupTrace.mark("DatabaseSeeder coroutine started")
                StartupTrace.measureSuspend("DatabaseSeeder.seedIfNeeded") {
                    seeder.seedIfNeeded()
                }
                StartupTrace.mark("DatabaseSeeder coroutine finished")
            } catch (ce: CancellationException) {
                throw ce
            } catch (t: Throwable) {
                // Defensive: a Hilt entry-point/DI failure or seeding error must not bring
                // down startup. Seeding is idempotent (count-guarded) and retried next launch.
                StartupTrace.mark("DatabaseSeeder failed: ${t.message}")
                Log.e("AppStartupSeeder", "Database seeding failed", t)
            }
        }
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface SeederEntryPoint {
    fun databaseSeeder(): DatabaseSeeder
}
