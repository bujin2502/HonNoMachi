package hr.foi.air.honnomachi

import com.google.firebase.Firebase
import com.google.firebase.crashlytics.crashlytics

object CrashlyticsManager {
    private var currentVisibleScreen: String = "Unknown"

    fun updateCurrentScreen(screenName: String) {
        currentVisibleScreen = screenName
    }

    fun setUserId(userId: String?) {
        Firebase.crashlytics.setUserId(userId ?: "")
    }

    // Biljezenje trenutnog ekrana na kojem je greska nastala i same greske
    fun logException(exception: Throwable) {
        Firebase.crashlytics.setCustomKey("current_screen", currentVisibleScreen)
        Firebase.crashlytics.recordException(exception)
    }
}
