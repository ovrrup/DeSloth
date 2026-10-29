package com.desloth.app.overlay

import android.content.Context
import com.desloth.app.data.local.entities.DeliverableEntity
import com.desloth.app.data.local.entities.FactVaultEntity

/**
 * Backward compatibility alias delegating to the hardened OverlayController.
 */
class OverlayManager(context: Context) {
    private val delegate = OverlayController(context)

    fun dismissOverlay() = delegate.dismissOverlay()

    fun showFrictionOverlay(
        packageName: String,
        balance: Int,
        fact: FactVaultEntity,
        onBypassGranted: (pkg: String) -> Unit,
        onHomeAction: () -> Unit
    ) = delegate.showFrictionOverlay(packageName, balance, fact, onBypassGranted, onHomeAction)

    fun showCircuitBreakerOverlay(
        balance: Int,
        microTask: DeliverableEntity?,
        onTaskCompleted: (taskId: Long) -> Unit,
        onHomeAction: () -> Unit
    ) = delegate.showCircuitBreakerOverlay(balance, microTask, onTaskCompleted, onHomeAction)
}
