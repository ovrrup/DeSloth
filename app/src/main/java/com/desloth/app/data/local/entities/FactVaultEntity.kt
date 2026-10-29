package com.desloth.app.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.concurrent.TimeUnit

/**
 * Represents cognitive anchor statements tied to hard deadlines and daily hour requirements.
 * Displayed in the friction overlay (-49 <= B <= 0) requiring verbatim typing to unlock a 10-minute bypass.
 *
 * @param id Unique identifier
 * @param statement The exact verbatim string the user must type to overcome cognitive inertia
 * @param goalTitle The macro goal or project this anchor safeguards
 * @param deadlineEpochMillis Epoch timestamp of the milestone or hard deadline
 * @param requiredDailyHours Dedicated hours per day committed toward the milestone
 */
@Entity(tableName = "fact_vault")
data class FactVaultEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val statement: String,
    val goalTitle: String,
    val deadlineEpochMillis: Long,
    val requiredDailyHours: Double
) {
    /**
     * Calculates days remaining until the hard deadline from current system time.
     */
    fun daysRemaining(currentEpochMillis: Long = System.currentTimeMillis()): Long {
        val diff = deadlineEpochMillis - currentEpochMillis
        return if (diff > 0) {
            TimeUnit.MILLISECONDS.toDays(diff)
        } else {
            0L
        }
    }
}
