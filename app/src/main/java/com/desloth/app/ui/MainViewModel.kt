package com.desloth.app.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.desloth.app.data.datastore.BypassState
import com.desloth.app.data.local.entities.DeliverableEntity
import com.desloth.app.data.local.entities.FactVaultEntity
import com.desloth.app.data.local.entities.TargetAppEntity
import com.desloth.app.data.repository.LedgerStatus
import com.desloth.app.data.repository.StimulationLedgerState
import com.desloth.app.di.AppContainer
import com.desloth.app.ui.components.PermissionUtils
import com.desloth.app.ui.components.SystemPermissionsState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(private val appContainer: AppContainer) : ViewModel() {

    private val _ledgerState = MutableStateFlow(
        StimulationLedgerState(
            balance = 0,
            earnedUnits = 0,
            tier1Minutes = 0L,
            tier2Minutes = 0L,
            totalDrain = 0L,
            tier1AppsCount = 0,
            tier2AppsCount = 0,
            currentEpochDay = 0L,
            status = LedgerStatus.FRICTION
        )
    )
    val ledgerState: StateFlow<StimulationLedgerState> = _ledgerState.asStateFlow()

    private val _usageMap = MutableStateFlow<Map<String, Long>>(emptyMap())
    val usageMap: StateFlow<Map<String, Long>> = _usageMap.asStateFlow()

    private val _permissionsState = MutableStateFlow(
        SystemPermissionsState(
            hasUsageAccess = false,
            hasOverlayAccess = false,
            hasAccessibilityAccess = false
        )
    )
    val permissionsState: StateFlow<SystemPermissionsState> = _permissionsState.asStateFlow()

    val deliverables: StateFlow<List<DeliverableEntity>> = appContainer.deliverableDao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val targetApps: StateFlow<List<TargetAppEntity>> = appContainer.targetAppDao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val facts: StateFlow<List<FactVaultEntity>> = appContainer.factVaultDao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bypassState: StateFlow<BypassState?> = appContainer.preferencesRepository.bypassStateFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        refreshLedger()
    }

    fun checkPermissions(context: Context) {
        _permissionsState.value = PermissionUtils.checkPermissions(context)
    }

    fun refreshLedger() {
        viewModelScope.launch {
            val state = appContainer.ledgerRepository.computeCurrentState()
            _ledgerState.value = state

            val currentTargets = appContainer.targetAppDao.getAllSync()
            val packages = currentTargets.map { it.packageName }.toSet()
            _usageMap.value = appContainer.usageTracker.getForegroundMinutesTodayBatch(packages)
        }
    }

    fun toggleDeliverable(id: Long, completed: Boolean) {
        viewModelScope.launch {
            appContainer.ledgerRepository.markDeliverableCompleted(id, completed)
            refreshLedger()
        }
    }

    fun addDeliverable(title: String, unitValue: Int, isMicroTask: Boolean) {
        viewModelScope.launch {
            val epochDay = appContainer.usageTracker.getCurrentEpochDay()
            appContainer.deliverableDao.insert(
                DeliverableEntity(
                    title = title,
                    unitValue = unitValue,
                    isMicroTask = isMicroTask,
                    completedToday = false,
                    lastCompletedEpochDay = 0L
                )
            )
            refreshLedger()
        }
    }

    fun deleteDeliverable(id: Long) {
        viewModelScope.launch {
            appContainer.deliverableDao.deleteById(id)
            refreshLedger()
        }
    }

    fun addTargetApp(packageName: String, appName: String, tier: Int) {
        viewModelScope.launch {
            appContainer.targetAppDao.insert(
                TargetAppEntity(
                    packageName = packageName,
                    appName = appName,
                    tier = tier
                )
            )
            refreshLedger()
        }
    }

    fun updateTargetAppTier(packageName: String, newTier: Int) {
        viewModelScope.launch {
            val existing = appContainer.targetAppDao.getByPackage(packageName) ?: return@launch
            appContainer.targetAppDao.update(existing.copy(tier = newTier))
            refreshLedger()
        }
    }

    fun deleteTargetApp(packageName: String) {
        viewModelScope.launch {
            appContainer.targetAppDao.deleteByPackage(packageName)
            refreshLedger()
        }
    }

    fun addFact(statement: String, goalTitle: String, deadlineMillis: Long, hours: Double) {
        viewModelScope.launch {
            appContainer.factVaultDao.insert(
                FactVaultEntity(
                    statement = statement,
                    goalTitle = goalTitle,
                    deadlineEpochMillis = deadlineMillis,
                    requiredDailyHours = hours
                )
            )
        }
    }

    fun deleteFact(id: Long) {
        viewModelScope.launch {
            appContainer.factVaultDao.deleteById(id)
        }
    }

    fun seedSampleData() {
        viewModelScope.launch {
            appContainer.ledgerRepository.seedSampleData()
            refreshLedger()
        }
    }

    fun resetTodayCompletion() {
        viewModelScope.launch {
            // Force reset completion flags to test midnight behavior
            val all = appContainer.deliverableDao.getAllSync()
            all.forEach {
                appContainer.deliverableDao.markCompleted(it.id, false, 0L)
            }
            refreshLedger()
        }
    }

    companion object {
        fun provideFactory(appContainer: AppContainer): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return MainViewModel(appContainer) as T
                }
            }
    }
}
