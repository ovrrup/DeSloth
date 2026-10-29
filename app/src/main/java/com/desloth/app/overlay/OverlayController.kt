package com.desloth.app.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.TypedValue
import android.view.ActionMode
import android.view.Gravity
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.desloth.app.data.local.entities.DeliverableEntity
import com.desloth.app.data.local.entities.FactVaultEntity
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Hardened overlay controller managing programmatic WindowManager overlays.
 * Features:
 * - Atomic double-attachment guard and parent nullity checks.
 * - Hardware Back key interception via custom FrameLayout.
 * - Strict WindowManager flags: FLAG_LAYOUT_IN_SCREEN | FLAG_WATCH_OUTSIDE_TOUCH (WITHOUT FLAG_NOT_FOCUSABLE).
 * - Complete disablement of copy, paste, cut, context action menus, and autofill.
 */
class OverlayController(private val context: Context) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val mainHandler = Handler(Looper.getMainLooper())

    private val isOverlayShowing = AtomicBoolean(false)

    @Volatile
    private var currentRootLayout: OverlayRootFrameLayout? = null

    /**
     * Root container intercepting hardware Back presses to prevent dismissal.
     */
    @SuppressLint("ViewConstructor")
    class OverlayRootFrameLayout(
        context: Context,
        private val onBackBlocked: () -> Unit
    ) : FrameLayout(context) {

        init {
            isFocusable = true
            isFocusableInTouchMode = true
        }

        override fun dispatchKeyEvent(event: KeyEvent): Boolean {
            if (event.keyCode == KeyEvent.KEYCODE_BACK) {
                if (event.action == KeyEvent.ACTION_UP) {
                    onBackBlocked()
                }
                // Intercept and consume hardware Back press strictly to block dismissal
                return true
            }
            return super.dispatchKeyEvent(event)
        }
    }

    /**
     * Subclassed EditText that rigorously suppresses copy, paste, cut, autofill, and action menus.
     */
    @SuppressLint("ViewConstructor")
    private class StrictNoPasteEditText(context: Context) : EditText(context) {

        init {
            isLongClickable = false
            setTextIsSelectable(false)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                importantForAutofill = IMPORTANT_FOR_AUTOFILL_NO
            }

            val noOpActionModeCallback = object : ActionMode.Callback {
                override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean = false
                override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean = false
                override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean = false
                override fun onDestroyActionMode(mode: ActionMode?) {}
            }

            customSelectionActionModeCallback = noOpActionModeCallback
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                customInsertionActionModeCallback = noOpActionModeCallback
            }
        }

        override fun onTextContextMenuItem(id: Int): Boolean {
            if (id == android.R.id.paste || id == android.R.id.pasteAsPlainText ||
                id == android.R.id.copy || id == android.R.id.cut
            ) {
                return false
            }
            return super.onTextContextMenuItem(id)
        }
    }

    private fun dp(dpValue: Float): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dpValue,
            context.resources.displayMetrics
        ).toInt()
    }

    private fun createRoundedCardDrawable(
        backgroundColor: Int,
        cornerRadiusDp: Float = 12f,
        strokeColor: Int? = null,
        strokeWidthDp: Float = 1f
    ): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(backgroundColor)
            cornerRadius = dp(cornerRadiusDp).toFloat()
            if (strokeColor != null) {
                setStroke(dp(strokeWidthDp), strokeColor)
            }
        }
    }

    /**
     * Synchronously removes any currently attached overlay view with atomic state checks.
     */
    private fun removeOverlaySync() {
        val view = currentRootLayout
        if (view != null) {
            try {
                if (view.isAttachedToWindow && view.parent != null) {
                    windowManager.removeView(view)
                }
            } catch (_: Exception) {
                // View detached or window destroyed
            } finally {
                currentRootLayout = null
                isOverlayShowing.set(false)
            }
        } else {
            isOverlayShowing.set(false)
        }
    }

    /**
     * Dismisses the active overlay on the main thread safely.
     */
    fun dismissOverlay() {
        mainHandler.post {
            removeOverlaySync()
        }
    }

    /**
     * Checks if an overlay is currently actively showing.
     */
    fun isShowing(): Boolean = isOverlayShowing.get()

    /**
     * Displays the Friction Overlay for balances between -49 and 0.
     * Requires exact verbatim typing of the FactVault statement to unlock a 10-minute bypass.
     */
    fun showFrictionOverlay(
        packageName: String,
        balance: Int,
        fact: FactVaultEntity,
        onBypassGranted: (pkg: String) -> Unit,
        onHomeAction: () -> Unit
    ) {
        if (!Settings.canDrawOverlays(context)) return

        mainHandler.post {
            removeOverlaySync()

            val rootLayout = OverlayRootFrameLayout(context) {
                // Hardware Back intercepted: prevent dismissal
            }.apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                setBackgroundColor(Color.parseColor("#0F172A"))
            }

            val scrollView = ScrollView(context).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
                isFillViewport = true
            }

            val contentContainer = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(24f), dp(48f), dp(24f), dp(40f))
                gravity = Gravity.CENTER_HORIZONTAL
            }

            // 1. Friction Badge
            val badge = TextView(context).apply {
                text = "FRICTION GATE ACTIVE"
                setTextColor(Color.parseColor("#F59E0B"))
                textSize = 12f
                typeface = Typeface.DEFAULT_BOLD
                setPadding(dp(12f), dp(6f), dp(12f), dp(6f))
                background = createRoundedCardDrawable(
                    backgroundColor = Color.parseColor("#261E0B"),
                    cornerRadiusDp = 16f,
                    strokeColor = Color.parseColor("#F59E0B"),
                    strokeWidthDp = 1f
                )
                gravity = Gravity.CENTER
            }
            contentContainer.addView(badge)

            // 2. Title & Balance
            val titleView = TextView(context).apply {
                text = "Stimulation Ledger Deficit"
                setTextColor(Color.WHITE)
                textSize = 22f
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
                setPadding(0, dp(16f), 0, dp(4f))
            }
            contentContainer.addView(titleView)

            val balanceView = TextView(context).apply {
                text = "Current Balance: $balance units\n(Tier Friction: Target App Intercepted)"
                setTextColor(Color.parseColor("#94A3B8"))
                textSize = 14f
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, dp(24f))
            }
            contentContainer.addView(balanceView)

            // 3. Anchor Fact Box
            val anchorCard = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                background = createRoundedCardDrawable(
                    backgroundColor = Color.parseColor("#1E293B"),
                    cornerRadiusDp = 14f,
                    strokeColor = Color.parseColor("#334155")
                )
                setPadding(dp(18f), dp(18f), dp(18f), dp(18f))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = dp(20f)
                }
            }

            val goalText = TextView(context).apply {
                text = "GOAL COMMITMENT: ${fact.goalTitle.uppercase()}"
                setTextColor(Color.parseColor("#38BDF8"))
                textSize = 12f
                typeface = Typeface.DEFAULT_BOLD
                setPadding(0, 0, 0, dp(6f))
            }
            anchorCard.addView(goalText)

            val deadlineText = TextView(context).apply {
                val daysLeft = fact.daysRemaining()
                text = "⏳ $daysLeft days left until hard deadline (${fact.requiredDailyHours}h daily requirement)"
                setTextColor(Color.parseColor("#E2E8F0"))
                textSize = 13f
                setPadding(0, 0, 0, dp(12f))
            }
            anchorCard.addView(deadlineText)

            val statementPrompt = TextView(context).apply {
                text = "Verbatim Statement Anchor:"
                setTextColor(Color.parseColor("#64748B"))
                textSize = 11f
                typeface = Typeface.DEFAULT_BOLD
                setPadding(0, 0, 0, dp(4f))
            }
            anchorCard.addView(statementPrompt)

            val statementView = TextView(context).apply {
                text = "\"${fact.statement}\""
                setTextColor(Color.parseColor("#F8FAFC"))
                textSize = 15f
                typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
                setLineSpacing(0f, 1.25f)
                background = createRoundedCardDrawable(
                    backgroundColor = Color.parseColor("#0F172A"),
                    cornerRadiusDp = 8f
                )
                setPadding(dp(12f), dp(12f), dp(12f), dp(12f))
            }
            anchorCard.addView(statementView)
            contentContainer.addView(anchorCard)

            // 4. Instructions
            val instructionView = TextView(context).apply {
                text = "Type the exact statement verbatim below to unlock a 10-minute bypass. Copy-paste is disabled."
                setTextColor(Color.parseColor("#94A3B8"))
                textSize = 13f
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, dp(12f))
            }
            contentContainer.addView(instructionView)

            // 5. Hardened EditText without paste, cut, copy, or autofill
            val inputEditText = StrictNoPasteEditText(context).apply {
                hint = "Type statement exactly as written..."
                setHintTextColor(Color.parseColor("#475569"))
                setTextColor(Color.WHITE)
                textSize = 14f
                background = createRoundedCardDrawable(
                    backgroundColor = Color.parseColor("#1E293B"),
                    cornerRadiusDp = 10f,
                    strokeColor = Color.parseColor("#475569"),
                    strokeWidthDp = 1.5f
                )
                setPadding(dp(14f), dp(14f), dp(14f), dp(14f))
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = dp(24f)
                }
            }
            contentContainer.addView(inputEditText)

            // 6. Action Buttons
            val bypassButton = Button(context).apply {
                text = "Unlock 10-Minute Bypass (Locked)"
                isEnabled = false
                setTextColor(Color.parseColor("#64748B"))
                textSize = 14f
                typeface = Typeface.DEFAULT_BOLD
                background = createRoundedCardDrawable(
                    backgroundColor = Color.parseColor("#334155"),
                    cornerRadiusDp = 10f
                )
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(52f)
                ).apply {
                    bottomMargin = dp(12f)
                }
                setOnClickListener {
                    dismissOverlay()
                    onBypassGranted(packageName)
                }
            }
            contentContainer.addView(bypassButton)

            val targetStatementTrimmed = fact.statement.trim()
            inputEditText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    val typed = s?.toString()?.trim() ?: ""
                    val isMatch = typed == targetStatementTrimmed
                    bypassButton.isEnabled = isMatch
                    if (isMatch) {
                        bypassButton.text = "✓ Unlock 10-Minute Bypass"
                        bypassButton.setTextColor(Color.WHITE)
                        bypassButton.background = createRoundedCardDrawable(
                            backgroundColor = Color.parseColor("#10B981"),
                            cornerRadiusDp = 10f
                        )
                        inputEditText.background = createRoundedCardDrawable(
                            backgroundColor = Color.parseColor("#1E293B"),
                            cornerRadiusDp = 10f,
                            strokeColor = Color.parseColor("#10B981"),
                            strokeWidthDp = 2f
                        )
                    } else {
                        bypassButton.text = "Unlock 10-Minute Bypass (Locked)"
                        bypassButton.setTextColor(Color.parseColor("#64748B"))
                        bypassButton.background = createRoundedCardDrawable(
                            backgroundColor = Color.parseColor("#334155"),
                            cornerRadiusDp = 10f
                        )
                        inputEditText.background = createRoundedCardDrawable(
                            backgroundColor = Color.parseColor("#1E293B"),
                            cornerRadiusDp = 10f,
                            strokeColor = Color.parseColor("#475569"),
                            strokeWidthDp = 1.5f
                        )
                    }
                }
            })

            val homeButton = Button(context).apply {
                text = "Return to Home Screen"
                setTextColor(Color.parseColor("#E2E8F0"))
                textSize = 14f
                background = createRoundedCardDrawable(
                    backgroundColor = Color.TRANSPARENT,
                    cornerRadiusDp = 10f,
                    strokeColor = Color.parseColor("#475569"),
                    strokeWidthDp = 1f
                )
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(48f)
                )
                setOnClickListener {
                    dismissOverlay()
                    onHomeAction()
                }
            }
            contentContainer.addView(homeButton)

            scrollView.addView(contentContainer)
            rootLayout.addView(scrollView)

            // Strict flags: FLAG_LAYOUT_IN_SCREEN | FLAG_WATCH_OUTSIDE_TOUCH (WITHOUT FLAG_NOT_FOCUSABLE)
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.CENTER
                softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
            }

            // Guard against double attachment and verify parent reference
            if (rootLayout.parent == null && isOverlayShowing.compareAndSet(false, true)) {
                try {
                    windowManager.addView(rootLayout, params)
                    currentRootLayout = rootLayout
                } catch (_: Exception) {
                    isOverlayShowing.set(false)
                    currentRootLayout = null
                }
            }
        }
    }

    /**
     * Displays the Circuit Breaker Overlay for balances <= -50.
     * Typing bypass is disabled. User must complete an emergency micro-task or return to Home.
     */
    fun showCircuitBreakerOverlay(
        balance: Int,
        microTask: DeliverableEntity?,
        onTaskCompleted: (taskId: Long) -> Unit,
        onHomeAction: () -> Unit
    ) {
        if (!Settings.canDrawOverlays(context)) return

        mainHandler.post {
            removeOverlaySync()

            val rootLayout = OverlayRootFrameLayout(context) {
                // Hardware Back intercepted: prevent dismissal
            }.apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                setBackgroundColor(Color.parseColor("#090202"))
            }

            val scrollView = ScrollView(context).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
                isFillViewport = true
            }

            val contentContainer = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(24f), dp(56f), dp(24f), dp(40f))
                gravity = Gravity.CENTER_HORIZONTAL
            }

            // 1. Alert Badge
            val alertBadge = TextView(context).apply {
                text = "⚡ CIRCUIT BREAKER TRIPPED ⚡"
                setTextColor(Color.parseColor("#EF4444"))
                textSize = 13f
                typeface = Typeface.DEFAULT_BOLD
                setPadding(dp(14f), dp(8f), dp(14f), dp(8f))
                background = createRoundedCardDrawable(
                    backgroundColor = Color.parseColor("#260808"),
                    cornerRadiusDp = 20f,
                    strokeColor = Color.parseColor("#EF4444"),
                    strokeWidthDp = 1.5f
                )
                gravity = Gravity.CENTER
            }
            contentContainer.addView(alertBadge)

            // 2. Alert Title
            val titleView = TextView(context).apply {
                text = "Emergency Lockout"
                setTextColor(Color.WHITE)
                textSize = 24f
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
                setPadding(0, dp(18f), 0, dp(6f))
            }
            contentContainer.addView(titleView)

            val deficitView = TextView(context).apply {
                text = "Severe Deficit: $balance units (Threshold: ≤ -50)\nTyping bypass is completely disabled."
                setTextColor(Color.parseColor("#FCA5A5"))
                textSize = 14f
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, dp(24f))
            }
            contentContainer.addView(deficitView)

            // 3. Task Card
            val taskCard = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                background = createRoundedCardDrawable(
                    backgroundColor = Color.parseColor("#180B0B"),
                    cornerRadiusDp = 14f,
                    strokeColor = Color.parseColor("#450A0A"),
                    strokeWidthDp = 1.5f
                )
                setPadding(dp(20f), dp(20f), dp(20f), dp(20f))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = dp(28f)
                }
            }

            val taskHeader = TextView(context).apply {
                text = "RECOVERY MICRO-TASK (5-MIN ACTION)"
                setTextColor(Color.parseColor("#F87171"))
                textSize = 12f
                typeface = Typeface.DEFAULT_BOLD
                setPadding(0, 0, 0, dp(8f))
            }
            taskCard.addView(taskHeader)

            val taskTitle = microTask?.title ?: "Emergency 5-min physical reset & hydration"
            val taskValue = microTask?.unitValue ?: 15

            val taskTitleView = TextView(context).apply {
                text = taskTitle
                setTextColor(Color.WHITE)
                textSize = 17f
                typeface = Typeface.DEFAULT_BOLD
                setPadding(0, 0, 0, dp(8f))
            }
            taskCard.addView(taskTitleView)

            val rewardBadge = TextView(context).apply {
                text = "Reward: +$taskValue Stimulation Units upon completion"
                setTextColor(Color.parseColor("#34D399"))
                textSize = 13f
            }
            taskCard.addView(rewardBadge)
            contentContainer.addView(taskCard)

            // 4. Complete Action Button
            val completeButton = Button(context).apply {
                text = "Mark Task Completed (+$taskValue Units)"
                setTextColor(Color.WHITE)
                textSize = 15f
                typeface = Typeface.DEFAULT_BOLD
                background = createRoundedCardDrawable(
                    backgroundColor = Color.parseColor("#059669"),
                    cornerRadiusDp = 10f
                )
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(54f)
                ).apply {
                    bottomMargin = dp(14f)
                }
                setOnClickListener {
                    dismissOverlay()
                    val taskId = microTask?.id ?: 0L
                    onTaskCompleted(taskId)
                }
            }
            contentContainer.addView(completeButton)

            // 5. Exit Home Button
            val exitHomeButton = Button(context).apply {
                text = "Exit to Home Screen"
                setTextColor(Color.parseColor("#E2E8F0"))
                textSize = 14f
                background = createRoundedCardDrawable(
                    backgroundColor = Color.TRANSPARENT,
                    cornerRadiusDp = 10f,
                    strokeColor = Color.parseColor("#7F1D1D"),
                    strokeWidthDp = 1f
                )
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(48f)
                )
                setOnClickListener {
                    dismissOverlay()
                    onHomeAction()
                }
            }
            contentContainer.addView(exitHomeButton)

            scrollView.addView(contentContainer)
            rootLayout.addView(scrollView)

            // Strict flags: FLAG_LAYOUT_IN_SCREEN | FLAG_WATCH_OUTSIDE_TOUCH (WITHOUT FLAG_NOT_FOCUSABLE)
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.CENTER
            }

            // Guard against double attachment and verify parent reference
            if (rootLayout.parent == null && isOverlayShowing.compareAndSet(false, true)) {
                try {
                    windowManager.addView(rootLayout, params)
                    currentRootLayout = rootLayout
                } catch (_: Exception) {
                    isOverlayShowing.set(false)
                    currentRootLayout = null
                }
            }
        }
    }
}
