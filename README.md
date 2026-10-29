# DeSloth 🦥⚡

**DeSloth** is an offline-first, kinetic friction Android application prototype designed to eliminate compulsive digital distraction through the **Stimulation Ledger** mechanic.

---

## 🏛 Architecture & Engineering Pillars

- **Application ID:** `com.desloth.app`
- **Target SDK:** 34 | **Min SDK:** 26 (Android 8.0+)
- **100% Offline-First:** Zero `INTERNET` permissions requested; zero network libraries included.
- **No AI / Gamification / Study Timers:** Pure deterministic systems engineering.
- **Manual Dependency Injection:** Pure singleton `AppContainer` pattern (no Hilt, no Koin).
- **Dual UI Layering:**
  - Modern **Jetpack Compose Material 3** dashboard for configuration, permissions, and task management.
  - Raw **Programmatic WindowManager Views** (`TYPE_APPLICATION_OVERLAY`) for friction and circuit breaker gates.
- **Local Persistence & Fast State:**
  - **Room via KSP** for relational local persistence.
  - **Jetpack Preferences DataStore** for non-blocking state (e.g., 10-minute bypass timers).

---

## ⚡ The Stimulation Ledger Mechanics

The core mechanic enforces a dynamic balance $B$ that resets naturally at midnight:

$$B = \text{EarnedUnits} - (3 \times \text{Tier1\_Minutes} + 1 \times \text{Tier2\_Minutes})$$

- **EarnedUnits:** Sum of unit values from completed daily deliverables and micro-tasks.
- **Tier 1 Minutes (3× Drain):** High-dopamine blacklisted apps (e.g. YouTube, TikTok, Instagram).
- **Tier 2 Minutes (1× Drain):** Moderate distraction blacklisted apps (e.g. Reddit, Chrome).
- **Usage Tracking:** Calculated via `UsageStatsManager.queryEvents` pairing `ACTIVITY_RESUMED` / `ACTIVITY_PAUSED`, capping on `SCREEN_NON_INTERACTIVE`, and extending open apps to live timestamp.

### Gate States

| Balance $B$ | State | System Behavior |
| :--- | :--- | :--- |
| **$B > 0$** | **Surplus** | App allowed. Toast shows live balance and drain rate. 1-minute ticker active. |
| **$-49 \le B \le 0$** | **Friction Gate** | Full-screen `TYPE_APPLICATION_OVERLAY`. Displays Fact Vault anchor with days to deadline. Verbatim typing with copy/paste disabled to unlock a 10-minute bypass, or exit to Home. |
| **$B \le -50$** | **Circuit Breaker** | Emergency lockout. Typing bypass disabled. User must complete an emergency 5-minute micro-task (+15 units) or return to Home screen. |

---

## 🛡 Systems Hardening

1. **Window Lifecycle Protection:** Atomic state guards (`AtomicBoolean`) and parent nullity checks prevent double-attachment crashes.
2. **Keyboard Focus & Back Interception:** Overlay uses `FLAG_LAYOUT_IN_SCREEN` and `FLAG_WATCH_OUTSIDE_TOUCH` (omits `FLAG_NOT_FOCUSABLE`), allowing software keyboard input while consuming hardware Back presses via custom `FrameLayout` key dispatching.
3. **Copy-Paste & Autofill Suppression:** Subclassed `EditText` overrides context menus, disables text selection, suppresses clipboard events, and disables autofill.
4. **Active 1-Minute Foreground Ticker:** Dynamically recalculates $B$ every 60 seconds while inside a blacklisted app under Surplus or Bypass. Drops the overlay immediately when balance drains to $\le 0$ or bypass expires during continuous scrolling.
5. **Anti-Tamper Protection:** Intercepts `com.android.settings` during deficit states ($B \le 0$) to block force-stops or permission revocation, instantly dispatching `GLOBAL_ACTION_HOME` and locking the screen.

---

## 🛠 Building the Project

### Using GitHub Actions
The repository includes a GitHub Actions workflow in `.github/workflows/build-apk.yml`. Every push or pull request automatically compiles the project and generates downloadable debug APK artifacts.

### Local Build via Gradle Wrapper
```bash
./gradlew assembleDebug
```
The output APK will be generated at:
```
app/build/outputs/apk/debug/app-debug.apk
```

---

## 📄 License
This project is open-source under the Apache 2.0 License.
