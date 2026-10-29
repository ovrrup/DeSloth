package com.desloth.app.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import com.desloth.app.DeSlothApp
import com.desloth.app.data.local.entities.DeliverableEntity
import com.desloth.app.data.local.entities.FactVaultEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/**
 * Hardened Accessibility Service intercepting foreground window state transitions.
 * Features:
 * - Anti-tamper protection blocking com.android.settings during Warning (-49..0) or Circuit Breaker (<= -50) states.
 * - Dynamic 1-minute Coroutine ticker active ONLY while a blacklisted app is foregrounded under Surplus or Bypass.
 * - Automatic overlay drop upon balance exhaustion or grace window expiration without waiting for window events.
 */
class DeSlothInterceptorService : AccessibilityService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val mainHandler = Handler(Looper.getMainLooper())

    private val appContainer by lazy {
        (applicationContext as DeSlothApp).appContainer
    }

    @Volatile
    private var currentForegroundTargetPackage: String? = null
    private var tickerJob: Job? = null

    private var lastInterceptedPackage: String? = null
    private var lastInterceptTimestamp: Long = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.DEFAULT
            notificationTimeout = 100
            canRetrieveWindowContent = false
        }
        serviceInfo = info
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            return
        }

        val packageName = event.packageName?.toString() ?: return

        // 1. Anti-Tamper: Block com.android.settings during Warning or Circuit Breaker state
        if (isSettingsPackage(packageName)) {
            serviceScope.launch {
                val state = appContainer.ledgerRepository.computeCurrentState()
                val balance = state.balance
                if (balance <= 0) {
                    stopForegroundTicker()
                    currentForegroundTargetPackage = null
                    mainHandler.post {
                        performGlobalAction(GLOBAL_ACTION_HOME)
                        Toast.makeText(
                            applicationContext,
                            "DeSloth Anti-Tamper: System Settings blocked during stimulation deficit",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    triggerFrictionOrLockout(packageName = packageName, balance = balance)
                }
            }
            return
        }

        // 2. Ticker Management: Cancel ticker if moving away from current blacklisted app
        if (packageName != currentForegroundTargetPackage) {
            stopForegroundTicker()
            currentForegroundTargetPackage = null
        }

        // 3. Filter out system UI, keyboards, launchers, and DeSloth itself
        if (shouldIgnorePackage(packageName)) {
            return
        }

        // Debounce rapid window changes for the exact same package within 800ms
        val now = System.currentTimeMillis()
        if (packageName == lastInterceptedPackage && (now - lastInterceptTimestamp) < 800L) {
            return
        }
        lastInterceptedPackage = packageName
        lastInterceptTimestamp = now

        serviceScope.launch {
            handlePackageTransition(packageName)
        }
    }

    private suspend fun handlePackageTransition(packageName: String) {
        // Check if package is in TargetAppEntity
        val targetApp = appContainer.targetAppDao.getByPackage(packageName)
        if (targetApp == null) {
            stopForegroundTicker()
            currentForegroundTargetPackage = null
            return
        }

        currentForegroundTargetPackage = packageName

        // Check 10-minute grace bypass
        val isBypassed = appContainer.preferencesRepository.isBypassActive(packageName)
        val state = appContainer.ledgerRepository.computeCurrentState()
        val balance = state.balance

        if (isBypassed) {
            // Under active grace bypass: allow app and maintain 1-minute ticker
            startForegroundTicker(packageName)
            return
        }

        when {
            // STATE 1: Surplus (B > 0)
            balance > 0 -> {
                val drainRate = targetApp.drainRatePerMinute
                mainHandler.post {
                    Toast.makeText(
                        applicationContext,
                        "DeSloth: Access Allowed | Balance: +$balance | Drain: $drainRate unit/min (Tier ${targetApp.tier})",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                // Maintain ticker while in Surplus so that running to 0 immediately drops the overlay
                startForegroundTicker(packageName)
            }

            // STATE 2: Warning/Friction (-49 <= B <= 0) or STATE 3: Circuit Breaker (B <= -50)
            else -> {
                stopForegroundTicker()
                triggerFrictionOrLockout(packageName, balance)
            }
        }
    }

    /**
     * Maintains an active 1-minute Coroutine ticker ONLY while a blacklisted app is in the foreground
     * under a 10-minute grace bypass or Surplus state.
     * Recalculates Balance B every 60 seconds; if Surplus drains to <= 0 or bypass expires,
     * immediately drops the overlay without waiting for a new window event.
     */
    private fun startForegroundTicker(packageName: String) {
        tickerJob?.cancel()
        tickerJob = serviceScope.launch {
            while (isActive) {
                delay(60_000L) // 1-minute dynamic tick

                // Verify target app is still foregrounded
                if (currentForegroundTargetPackage != packageName) {
                    break
                }

                val isBypassed = appContainer.preferencesRepository.isBypassActive(packageName)
                val state = appContainer.ledgerRepository.computeCurrentState()
                val balance = state.balance

                if (!isBypassed && balance <= 0) {
                    // Exact moment Surplus exhausted or 10-min bypass expired while scrolling
                    triggerFrictionOrLockout(packageName, balance)
                    break
                }
            }
        }
    }

    private fun stopForegroundTicker() {
        tickerJob?.cancel()
        tickerJob = null
    }

    /**
     * Drops either the Friction Overlay or Circuit Breaker overlay based on Balance B.
     */
    private suspend fun triggerFrictionOrLockout(packageName: String, balance: Int) {
        if (balance >= -49) {
            // Warning/Friction Gate
            val fact = appContainer.ledgerRepository.getRandomFact()
                ?: FactVaultEntity(
                    statement = "I am committed to deep engineering execution over reflexive digital consumption.",
                    goalTitle = "Core System Focus",
                    deadlineEpochMillis = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(30),
                    requiredDailyHours = 4.0
                )

            appContainer.overlayController.showFrictionOverlay(
                packageName = packageName,
                balance = balance,
                fact = fact,
                onBypassGranted = { pkg ->
                    serviceScope.launch {
                        appContainer.preferencesRepository.setBypass(pkg, durationMinutes = 10L)
                        mainHandler.post {
                            Toast.makeText(
                                applicationContext,
                                "10-Minute Bypass Unlocked for $pkg",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        // Start 1-minute ticker for the newly unlocked bypass window
                        startForegroundTicker(pkg)
                    }
                },
                onHomeAction = {
                    stopForegroundTicker()
                    performGlobalAction(GLOBAL_ACTION_HOME)
                }
            )
        } else {
            // Circuit Breaker Lockout
            val uncompletedMicroTask = appContainer.ledgerRepository.getNextUncompletedMicroTask()

            appContainer.overlayController.showCircuitBreakerOverlay(
                balance = balance,
                microTask = uncompletedMicroTask,
                onTaskCompleted = { taskId ->
                    serviceScope.launch {
                        if (taskId != 0L) {
                            appContainer.ledgerRepository.markDeliverableCompleted(taskId, true)
                        } else {
                            val epochDay = appContainer.usageTracker.getCurrentEpochDay()
                            appContainer.deliverableDao.insert(
                                DeliverableEntity(
                                    title = "Emergency 5-min physical reset & hydration",
                                    unitValue = 15,
                                    isMicroTask = true,
                                    completedToday = true,
                                    lastCompletedEpochDay = epochDay
                                )
                            )
                        }
                        mainHandler.post {
                            Toast.makeText(
                                applicationContext,
                                "Micro-Task Recorded (+15 Units). Recalculating ledger...",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                        // Check if recovery achieved surplus
                        val updatedState = appContainer.ledgerRepository.computeCurrentState()
                        if (updatedState.balance > 0) {
                            startForegroundTicker(packageName)
                        }
                    }
                },
                onHomeAction = {
                    stopForegroundTicker()
                    performGlobalAction(GLOBAL_ACTION_HOME)
                }
            )
        }
    }

    /**
     * Identifies Android Settings applications for anti-tamper enforcement.
     */
    private fun isSettingsPackage(packageName: String): Boolean {
        return packageName == "com.android.settings" ||
                packageName == "com.google.android.settings.intelligence" ||
                packageName.startsWith("com.android.settings.")
    }

    /**
     * Determines whether a package should be ignored during normal window state monitoring:
     * - DeSloth itself
     * - Android OS Core / System UI
     * - Keyboards (Input Method Editors)
     * - Default Home Launcher
     */
    private fun shouldIgnorePackage(packageName: String): Boolean {
        if (packageName == applicationContext.packageName) return true
        if (packageName == "com.android.systemui" || packageName == "android") return true

        // Check active IMEs (keyboards)
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        val imePackages = imm?.enabledInputMethodList?.map { it.packageName }?.toSet() ?: emptySet()
        if (imePackages.contains(packageName)) return true

        // Check default home launcher
        val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val resolveInfo = packageManager.resolveActivity(homeIntent, PackageManager.MATCH_DEFAULT_ONLY)
        val launcherPackage = resolveInfo?.activityInfo?.packageName
        if (packageName == launcherPackage) return true

        return false
    }

    override fun onInterrupt() {
        stopForegroundTicker()
        appContainer.overlayController.dismissOverlay()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopForegroundTicker()
        appContainer.overlayController.dismissOverlay()
        serviceScope.cancel()
    }
}
