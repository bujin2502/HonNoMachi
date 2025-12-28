package hr.foi.air.honnomachi

import com.google.firebase.Firebase
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.crashlytics.crashlytics

class CrashlyticsManager(
    private val crashlytics: FirebaseCrashlytics = Firebase.crashlytics,
) : CrashlyticsService {
    private var currentVisibleScreen: String = "Unknown"

    override fun updateCurrentScreen(screenName: String) {
        currentVisibleScreen = screenName
    }

    override fun setUserId(userId: String?) {
        crashlytics.setUserId(userId ?: "")
    }

    // Biljezenje trenutnog ekrana na kojem je greska nastala i same greske
    override fun logException(exception: Throwable) {
        crashlytics.setCustomKey("current_screen", currentVisibleScreen)
        crashlytics.recordException(exception)
    }

    companion object {
        val instance: CrashlyticsService by lazy { CrashlyticsManager() }
    }
}
