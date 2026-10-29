package com.desloth.app.di

import android.content.Context
import com.desloth.app.data.datastore.AppPreferencesRepository
import com.desloth.app.data.local.DeSlothDatabase
import com.desloth.app.data.local.dao.DeliverableDao
import com.desloth.app.data.local.dao.FactVaultDao
import com.desloth.app.data.local.dao.TargetAppDao
import com.desloth.app.data.repository.StimulationLedgerRepository
import com.desloth.app.data.usage.UsageStatsHelper
import com.desloth.app.data.usage.UsageTracker
import com.desloth.app.overlay.OverlayController
import com.desloth.app.overlay.OverlayManager

/**
 * Manual Dependency Injection container contract for the DeSloth architecture.
 * Strictly avoids automated DI reflection or bytecode manipulation (e.g. Hilt/Koin).
 */
interface AppContainer {
    val database: DeSlothDatabase
    val targetAppDao: TargetAppDao
    val deliverableDao: DeliverableDao
    val factVaultDao: FactVaultDao
    val preferencesRepository: AppPreferencesRepository
    val usageTracker: UsageTracker
    val usageStatsHelper: UsageStatsHelper
    val ledgerRepository: StimulationLedgerRepository
    val overlayController: OverlayController
    val overlayManager: OverlayManager
}

class DefaultAppContainer(private val applicationContext: Context) : AppContainer {

    override val database: DeSlothDatabase by lazy {
        DeSlothDatabase.buildDatabase(applicationContext)
    }

    override val targetAppDao: TargetAppDao by lazy {
        database.targetAppDao()
    }

    override val deliverableDao: DeliverableDao by lazy {
        database.deliverableDao()
    }

    override val factVaultDao: FactVaultDao by lazy {
        database.factVaultDao()
    }

    override val preferencesRepository: AppPreferencesRepository by lazy {
        AppPreferencesRepository(applicationContext)
    }

    override val usageTracker: UsageTracker by lazy {
        UsageTracker(applicationContext)
    }

    override val usageStatsHelper: UsageStatsHelper by lazy {
        UsageStatsHelper(applicationContext)
    }

    override val ledgerRepository: StimulationLedgerRepository by lazy {
        StimulationLedgerRepository(
            targetAppDao = targetAppDao,
            deliverableDao = deliverableDao,
            factVaultDao = factVaultDao,
            usageTracker = usageTracker
        )
    }

    override val overlayController: OverlayController by lazy {
        OverlayController(applicationContext)
    }

    override val overlayManager: OverlayManager by lazy {
        OverlayManager(applicationContext)
    }
}
