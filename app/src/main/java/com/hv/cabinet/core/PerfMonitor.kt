package com.hv.cabinet.core

import android.os.SystemClock
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap

object PerfMonitor {
    private val marks = ConcurrentHashMap<String, Long>()

    fun mark(name: String) {
        val now = SystemClock.elapsedRealtime()
        marks[name] = now
        Timber.tag("Perf").i("[perf] mark=%s at=%d", name, now)
    }

    fun measure(start: String, end: String, label: String = "$start->$end"): Long? {
        val startAt = marks[start] ?: return null
        val endAt = marks[end] ?: return null
        val cost = (endAt - startAt).coerceAtLeast(0)
        Timber.tag("Perf").i("[perf] measure=%s cost_ms=%d", label, cost)
        return cost
    }
}
