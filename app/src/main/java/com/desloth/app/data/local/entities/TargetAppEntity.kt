package com.desloth.app.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a blacklisted application tracked by the Stimulation Ledger.
 * Tier 1 applications incur a high drain penalty (3x units/minute).
 * Tier 2 applications incur a standard drain penalty (1x unit/minute).
 */
@Entity(tableName = "target_apps")
data class TargetAppEntity(
    @PrimaryKey
    val packageName: String,
    val appName: String,
    val tier: Int // 1 or 2
) {
    init {
        require(tier == 1 || tier == 2) { "Tier must be either 1 or 2" }
    }

    val drainRatePerMinute: Int
        get() = if (tier == 1) 3 else 1
}
