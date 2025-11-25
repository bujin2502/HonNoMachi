package hr.foi.air.honnomachi

import hr.foi.air.honnomachi.viewmodel.AuthViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LoginUnitTest {

    private val TEST_EMAIL = "test@example.com"
    private val TEST_PASSWORD = "password123"

    @Test
    fun `login successful executes success callback`() {
        val viewModel = TestableLoginViewModel()
        var callbackSuccess: Boolean? = null
        viewModel.login(TEST_EMAIL, TEST_PASSWORD) { success, _ ->
            callbackSuccess = success
        }
        assertTrue(viewModel.loginCalled)
        assertEquals(TEST_EMAIL, viewModel.capturedEmail)
        assertEquals(TEST_PASSWORD, viewModel.capturedPassword)

        assertEquals(true, callbackSuccess)
    }

    @Test
    fun `login failure executes failure callback`() {
        val viewModel = TestableLoginViewModel()
        viewModel.nextResultSuccess = false
        var callbackSuccess: Boolean? = null
        viewModel.login(TEST_EMAIL, TEST_PASSWORD) { success, _ ->
            callbackSuccess = success
        }
        assertTrue(viewModel.loginCalled)

        assertEquals(false, callbackSuccess)
    }
}

// Klasa za testiranje
private class TestableLoginViewModel : AuthViewModel(null, null,null) {
    var loginCalled: Boolean = false
    var capturedEmail: String? = null
    var capturedPassword: String? = null
    var nextResultSuccess: Boolean = true

    override fun login(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        loginCalled = true
        capturedEmail = email
        capturedPassword = password

        if (nextResultSuccess) {
            onResult(true, null)
        } else {
            onResult(false, "Simulated login failure")
        }
    }

    override fun resendVerificationEmail(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
    }

    override fun signOut() {
    }
}