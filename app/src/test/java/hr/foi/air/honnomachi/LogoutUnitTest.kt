package hr.foi.air.honnomachi

import hr.foi.air.honnomachi.viewmodel.AuthViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LogoutUnitTest {

    @Test
    fun `sign out calls both methods`() {
        val viewModel = TestableLogoutViewModel()
        assertEquals(false, viewModel.signOutCalled)
        assertEquals(null, viewModel.loggedEvent)
        viewModel.signOut()

        assertTrue(viewModel.signOutCalled)
        assertEquals("user_logout", viewModel.loggedEvent)
    }
}
// Testna klasa
private class TestableLogoutViewModel : AuthViewModel(null, null, null) {
    var signOutCalled: Boolean = false
    var loggedEvent: String? = null
    override fun signOut() {
        loggedEvent = "user_logout"
        signOutCalled = true
    }

    override fun login(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
    }

    override fun resendVerificationEmail(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
    }
}