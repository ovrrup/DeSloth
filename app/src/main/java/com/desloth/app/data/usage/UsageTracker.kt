package com.desloth.app.data.usage

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.TimeUnit

/**
 * High-precision foreground usage engine using UsageStatsManager.queryEvents.
 * Accurately pairs ACTIVITY_RESUMED and ACTIVITY_PAUSED events from local midnight,
 * caps running sessions on SCREEN_NON_INTERACTIVE, and extends active apps to System.currentTimeMillis().
 */
class UsageTracker(private val context: Context) {

    private val usageStatsManager =
        context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager

    /**
     * Determines whether PACKAGE_USAGE_STATS permission has been granted.
     */
    fun hasUsageStatsPermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager
            ?: return false
        val mode = appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /**
     * Calculates start of the current day (midnight local time) in epoch milliseconds.
     */
    fun getStartOfTodayEpochMillis(): Long {
        return LocalDate.now()
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }

    /**
     * Calculates today's epoch day number (days since 1970-01-01).
     */
    fun getCurrentEpochDay(): Long {
        return LocalDate.now().toEpochDay()
    }

    /**
     * Queries UsageEvents from midnight to System.currentTimeMillis() and calculates exact
     * foreground duration in milliseconds.
     */
    fun getExactForegroundMsTodayBatch(packages: Set<String>): Map<String, Long> {
        val resultMap = packages.associateWith { 0L }.toMutableMap()
        if (packages.isEmpty() || !hasUsageStatsPermission() || usageStatsManager == null) {
            return resultMap
        }

        val startTime = getStartOfTodayEpochMillis()
        val endTime = System.currentTimeMillis()
        if (endTime <= startTime) {
            return resultMap
        }

        val usageEvents = usageStatsManager.queryEvents(startTime, endTime) ?: return resultMap
        val event = UsageEvents.Event()

        // Tracks packageName -> timestamp when resumed
        val activeResumedPackages = mutableMapOf<String, Long>()
        var screenInteractive = true

        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event)
            val pkg = event.packageName ?: continue
            val time = event.timeStamp.coerceIn(startTime, endTime)

            when (event.eventType) {
                // ACTIVITY_RESUMED (1) / MOVE_TO_FOREGROUND (1)
                UsageEvents.Event.ACTIVITY_RESUMED,
                UsageEvents.Event.MOVE_TO_FOREGROUND -> {
                    if (screenInteractive) {
                        // Close any other active package since only one app is foreground
                        val otherActive = activeResumedPackages.keys.filter { it != pkg }
                        for (otherPkg in otherActive) {
                            val start = activeResumedPackages.remove(otherPkg) ?: continue
                            val duration = maxOf(0L, time - start)
                            if (packages.contains(otherPkg)) {
                                resultMap[otherPkg] = (resultMap[otherPkg] ?: 0L) + duration
                            }
                        }

                        if (!activeResumedPackages.containsKey(pkg)) {
                            activeResumedPackages[pkg] = time
                        }
                    }
                }

                // ACTIVITY_PAUSED (2) / MOVE_TO_BACKGROUND (2)
                UsageEvents.Event.ACTIVITY_PAUSED,
                UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                    val resumedSince = activeResumedPackages.remove(pkg)
                    if (resumedSince != null) {
                        val duration = maxOf(0L, time - resumedSince)
                        if (packages.contains(pkg)) {
                            resultMap[pkg] = (resultMap[pkg] ?: 0L) + duration
                        }
                    } else if (packages.contains(pkg) && (resultMap[pkg] ?: 0L) == 0L) {
                        // Package was running across midnight before being paused
                        val duration = maxOf(0L, time - startTime)
                        resultMap[pkg] = duration
                    }
                }

                // SCREEN_NON_INTERACTIVE (16) - Screen turned off or device locked
                UsageEvents.Event.SCREEN_NON_INTERACTIVE -> {
                    screenInteractive = false
                    for ((activePkg, start) in activeResumedPackages) {
                        val duration = maxOf(0L, time - start)
                        if (packages.contains(activePkg)) {
                            resultMap[activePkg] = (resultMap[activePkg] ?: 0L) + duration
                        }
                    }
                    activeResumedPackages.clear()
                }

                // SCREEN_INTERACTIVE (15) - Screen unlocked/interactive
                UsageEvents.Event.SCREEN_INTERACTIVE -> {
                    screenInteractive = true
                }
            }
        }

        // Account for currently open applications up to current timestamp
        if (screenInteractive && endTime > startTime) {
            for ((activePkg, start) in activeResumedPackages) {
                val duration = maxOf(0L, endTime - start)
                if (packages.contains(activePkg)) {
                    resultMap[activePkg] = (resultMap[activePkg] ?: 0L) + duration
                }
            }
        }

        return resultMap
    }

    /**
     * Aggregates foreground milliseconds into whole minutes for requested packages.
     */
    fun getForegroundMinutesTodayBatch(packages: Set<String>): Map<String, Long> {
        val msMap = getExactForegroundMsTodayBatch(packages)
        return msMap.mapValues { TimeUnit.MILLISECONDS.toMinutes(it.value) }
    }

    /**
     * Queries foreground usage minutes today for a single package.
     */
    fun getForegroundMinutesToday(packageName: String): Long {
        return getForegroundMinutesTodayBatch(setOf(packageName))[packageName] ?: 0L
    }
}
