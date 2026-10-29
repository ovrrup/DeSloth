package com.desloth.app.data.usage

import android.content.Context

/**
 * Backward compatibility alias delegating to the hardened UsageTracker.
 */
class UsageStatsHelper(context: Context) {
    private val delegate = UsageTracker(context)

    fun hasUsageStatsPermission(): Boolean = delegate.hasUsageStatsPermission()
    fun getStartOfTodayEpochMillis(): Long = delegate.getStartOfTodayEpochMillis()
    fun getCurrentEpochDay(): Long = delegate.getCurrentEpochDay()
    fun getForegroundMinutesToday(packageName: String): Long = delegate.getForegroundMinutesToday(packageName)
    fun getForegroundMinutesTodayBatch(packages: Set<String>): Map<String, Long> = delegate.getForegroundMinutesTodayBatch(packages)
    fun getExactForegroundMsTodayBatch(packages: Set<String>): Map<String, Long> = delegate.getExactForegroundMsTodayBatch(packages)
}
