package hr.foi.air.honnomachi

interface CrashlyticsService {
    fun updateCurrentScreen(screenName: String)
    fun setUserId(userId: String?)
    fun logException(exception: Throwable)
}
