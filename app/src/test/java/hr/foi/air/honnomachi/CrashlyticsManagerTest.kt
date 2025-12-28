package hr.foi.air.honnomachi

import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test

class CrashlyticsManagerTest {
    private lateinit var crashlytics: FirebaseCrashlytics
    private lateinit var crashlyticsManager: CrashlyticsService

    @Before
    fun setUp() {
        crashlytics = mockk(relaxed = true)
        crashlyticsManager = CrashlyticsManager(crashlytics)
    }

    @Test
    fun `setUserId calls crashlytics with correct userId`() {
        val userId = "test-user-id"
        crashlyticsManager.setUserId(userId)
        verify { crashlytics.setUserId(userId) }
    }

    @Test
    fun `setUserId with null calls crashlytics with empty string`() {
        crashlyticsManager.setUserId(null)
        verify { crashlytics.setUserId("") }
    }

    @Test
    fun `logException records exception and sets custom key for screen`() {
        val exception = RuntimeException("Test exception")
        val screenName = "TestScreen"

        crashlyticsManager.updateCurrentScreen(screenName)
        crashlyticsManager.logException(exception)

        verify { crashlytics.setCustomKey("current_screen", screenName) }
        verify { crashlytics.recordException(exception) }
    }

    @Test
    fun `logException uses Unknown screen if not updated`() {
        val exception = RuntimeException("Another test exception")

        crashlyticsManager.logException(exception)

        verify { crashlytics.setCustomKey("current_screen", "Unknown") }
        verify { crashlytics.recordException(exception) }
    }
}
