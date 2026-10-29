package com.desloth.app.data.repository

import com.desloth.app.data.local.dao.DeliverableDao
import com.desloth.app.data.local.dao.FactVaultDao
import com.desloth.app.data.local.dao.TargetAppDao
import com.desloth.app.data.local.entities.DeliverableEntity
import com.desloth.app.data.local.entities.FactVaultEntity
import com.desloth.app.data.local.entities.TargetAppEntity
import com.desloth.app.data.usage.UsageTracker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

enum class LedgerStatus {
    SURPLUS,         // B > 0
    FRICTION,        // -49 <= B <= 0
    CIRCUIT_BREAKER  // B <= -50
}

data class StimulationLedgerState(
    val balance: Int,
    val earnedUnits: Int,
    val tier1Minutes: Long,
    val tier2Minutes: Long,
    val totalDrain: Long,
    val tier1AppsCount: Int,
    val tier2AppsCount: Int,
    val currentEpochDay: Long,
    val status: LedgerStatus
)

class StimulationLedgerRepository(
    private val targetAppDao: TargetAppDao,
    private val deliverableDao: DeliverableDao,
    private val factVaultDao: FactVaultDao,
    private val usageTracker: UsageTracker
) {

    /**
     * Resets any completion statuses from previous days and computes the live stimulation state.
     * B = EarnedUnits - (3 * Tier1_Minutes + 1 * Tier2_Minutes)
     */
    suspend fun computeCurrentState(): StimulationLedgerState = withContext(Dispatchers.IO) {
        val currentEpochDay = usageTracker.getCurrentEpochDay()

        // Enforce midnight rollover: reset tasks completed on prior days
        deliverableDao.resetCompletionIfOutdated(currentEpochDay)

        // 1. Calculate EarnedUnits
        val completedToday = deliverableDao.getCompletedToday(currentEpochDay)
        val earnedUnits = completedToday.sumOf { it.unitValue }

        // 2. Query target apps and calculate foreground drain
        val targetApps = targetAppDao.getAllSync()
        val tier1Apps = targetApps.filter { it.tier == 1 }
        val tier2Apps = targetApps.filter { it.tier == 2 }

        val allPackages = targetApps.map { it.packageName }.toSet()
        val usageMap = usageTracker.getForegroundMinutesTodayBatch(allPackages)

        val tier1Minutes = tier1Apps.sumOf { usageMap[it.packageName] ?: 0L }
        val tier2Minutes = tier2Apps.sumOf { usageMap[it.packageName] ?: 0L }

        val totalDrain = (3L * tier1Minutes) + (1L * tier2Minutes)
        val balance = (earnedUnits - totalDrain).toInt()

        val status = when {
            balance > 0 -> LedgerStatus.SURPLUS
            balance >= -49 -> LedgerStatus.FRICTION
            else -> LedgerStatus.CIRCUIT_BREAKER
        }

        StimulationLedgerState(
            balance = balance,
            earnedUnits = earnedUnits,
            tier1Minutes = tier1Minutes,
            tier2Minutes = tier2Minutes,
            totalDrain = totalDrain,
            tier1AppsCount = tier1Apps.size,
            tier2AppsCount = tier2Apps.size,
            currentEpochDay = currentEpochDay,
            status = status
        )
    }

    /**
     * Combined real-time flow tracking database updates.
     */
    fun observeLedgerUpdates(): Flow<List<DeliverableEntity>> {
        return deliverableDao.getAll()
    }

    suspend fun markDeliverableCompleted(id: Long, completed: Boolean) = withContext(Dispatchers.IO) {
        val epochDay = if (completed) usageTracker.getCurrentEpochDay() else 0L
        deliverableDao.markCompleted(id, completed, epochDay)
    }

    suspend fun completeMicroTaskEmergency(id: Long): DeliverableEntity? = withContext(Dispatchers.IO) {
        val epochDay = usageTracker.getCurrentEpochDay()
        val task = deliverableDao.getById(id) ?: return@withContext null
        deliverableDao.markCompleted(id, true, epochDay)
        task.copy(completedToday = true, lastCompletedEpochDay = epochDay)
    }

    suspend fun getNextUncompletedMicroTask(): DeliverableEntity? = withContext(Dispatchers.IO) {
        deliverableDao.getFirstUncompletedMicroTask()
    }

    suspend fun getRandomFact(): FactVaultEntity? = withContext(Dispatchers.IO) {
        factVaultDao.getRandomFact()
    }

    suspend fun seedSampleData() = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val currentEpochDay = usageTracker.getCurrentEpochDay()

        // 1. Target Apps
        val sampleTargets = listOf(
            TargetAppEntity(packageName = "com.google.android.youtube", appName = "YouTube", tier = 1),
            TargetAppEntity(packageName = "com.instagram.android", appName = "Instagram", tier = 1),
            TargetAppEntity(packageName = "com.twitter.android", appName = "X (Twitter)", tier = 1),
            TargetAppEntity(packageName = "com.zhiliaoapp.musically", appName = "TikTok", tier = 1),
            TargetAppEntity(packageName = "com.reddit.frontpage", appName = "Reddit", tier = 2),
            TargetAppEntity(packageName = "com.android.chrome", appName = "Google Chrome", tier = 2)
        )
        targetAppDao.insertAll(sampleTargets)

        // 2. Deliverables & Micro-tasks
        val sampleDeliverables = listOf(
            DeliverableEntity(
                title = "Deep Work Session: Android Systems Engineering",
                unitValue = 45,
                isMicroTask = false,
                completedToday = false,
                lastCompletedEpochDay = 0L
            ),
            DeliverableEntity(
                title = "PR Review & Architecture Refactoring",
                unitValue = 30,
                isMicroTask = false,
                completedToday = false,
                lastCompletedEpochDay = 0L
            ),
            DeliverableEntity(
                title = "Write Unit Tests & Enforce Strict Code Quality",
                unitValue = 25,
                isMicroTask = false,
                completedToday = false,
                lastCompletedEpochDay = 0L
            ),
            DeliverableEntity(
                title = "5-Min Friction Break: 25 pushups & hydrate 500ml",
                unitValue = 15,
                isMicroTask = true,
                completedToday = false,
                lastCompletedEpochDay = 0L
            ),
            DeliverableEntity(
                title = "5-Min Friction Break: Clear physical desk & review 3 priorities",
                unitValue = 15,
                isMicroTask = true,
                completedToday = false,
                lastCompletedEpochDay = 0L
            ),
            DeliverableEntity(
                title = "5-Min Friction Break: 10 deep diaphragmatic breaths & posture reset",
                unitValue = 15,
                isMicroTask = true,
                completedToday = false,
                lastCompletedEpochDay = 0L
            )
        )
        deliverableDao.insertAll(sampleDeliverables)

        // 3. Fact Vault commitments
        val sampleFacts = listOf(
            FactVaultEntity(
                statement = "I am committed to deep uninterrupted engineering focus to ship world-class systems.",
                goalTitle = "Staff Systems Architect Launch",
                deadlineEpochMillis = now + TimeUnit.DAYS.toMillis(28),
                requiredDailyHours = 4.5
            ),
            FactVaultEntity(
                statement = "Discipline is sacrificing momentary comfort for long-term mastery and architectural excellence.",
                goalTitle = "Zero-Debt High-Performance Core",
                deadlineEpochMillis = now + TimeUnit.DAYS.toMillis(45),
                requiredDailyHours = 3.5
            ),
            FactVaultEntity(
                statement = "Every minute spent on algorithmic feeds directly compromises my primary delivery milestone.",
                goalTitle = "Offline Distributed Systems Delivery",
                deadlineEpochMillis = now + TimeUnit.DAYS.toMillis(14),
                requiredDailyHours = 5.0
            )
        )
        factVaultDao.insertAll(sampleFacts)
    }
}
