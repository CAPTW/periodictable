package com.chemtable.interactive.core.util

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.SystemClock
import android.os.Trace
import android.util.Log

object StartupTrace {
    private const val TAG = "StartupTrace"
    private const val MAX_TRACE_LABEL_LENGTH = 127

    @Volatile
    private var enabled = false

    private val processStartMillis = SystemClock.elapsedRealtime()

    fun configure(context: Context) {
        enabled = context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
    }

    fun mark(label: String) {
        if (!enabled) return
        val elapsed = SystemClock.elapsedRealtime() - processStartMillis
        Log.d(TAG, "+${elapsed}ms $label")
    }

    fun <T> measure(label: String, block: () -> T): T {
        if (!enabled) return block()

        val start = SystemClock.elapsedRealtime()
        Trace.beginSection(label.take(MAX_TRACE_LABEL_LENGTH))
        try {
            return block()
        } finally {
            Trace.endSection()
            Log.d(TAG, "$label took ${SystemClock.elapsedRealtime() - start}ms")
        }
    }

    suspend fun <T> measureSuspend(label: String, block: suspend () -> T): T {
        if (!enabled) return block()

        val start = SystemClock.elapsedRealtime()
        try {
            return block()
        } finally {
            Log.d(TAG, "$label took ${SystemClock.elapsedRealtime() - start}ms")
        }
    }
}
