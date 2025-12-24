package hr.foi.air.honnomachi

import com.google.firebase.Firebase
import com.google.firebase.crashlytics.crashlytics

object CrashlyticsManager {

    fun logException(exception: Throwable) {
        Firebase.crashlytics.recordException(exception)
    }
}
