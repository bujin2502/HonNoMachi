package hr.foi.air.honnomachi

import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.testing.TestNavHostController
import androidx.test.ext.junit.runners.AndroidJUnit4
import hr.foi.air.honnomachi.data.AuthRepository
import hr.foi.air.honnomachi.ui.auth.AuthUiState
import hr.foi.air.honnomachi.ui.auth.AuthViewModel
import hr.foi.air.honnomachi.ui.auth.EmailVerificationScreen
import hr.foi.air.honnomachi.ui.auth.LoginScreen
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LoginFlowTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var navController: TestNavHostController
    private lateinit var fakeLoginViewModel: FakeLoginViewModel

    // Test podaci
    private val testEmail = "test@example.com"
    private val testPassword = "password123"

    private val verifyMsg = "Please verify your email before logging in."

    @Before
    fun setup() {
        fakeLoginViewModel = FakeLoginViewModel(mockk(relaxed = true), mockk(relaxed = true))

        composeRule.setContent {
            val context = LocalContext.current

            navController =
                TestNavHostController(context).apply {
                    navigatorProvider.addNavigator(ComposeNavigator())
                }

            NavHost(navController = navController, startDestination = "auth") {
                composable("auth") {
                    LoginScreen(navController = navController, authViewModel = fakeLoginViewModel)
                }

                composable("home") {
                    Text("Home Screen")
                }
                composable("verification") {
                    EmailVerificationScreen(
                        onNavigateToLogin = { navController.navigate("auth") },
                        authViewModel = fakeLoginViewModel,
                    )
                }
                composable("forgotPassword") {
                    Text("Forgot Password Screen")
                }
            }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun login_successful_navigates_to_home() {
        fakeLoginViewModel.shouldLoginSucceed = true
        composeRule.onNodeWithTag("email_field").performTextInput(testEmail)
        composeRule.onNodeWithTag("password_field").performTextInput(testPassword)
        composeRule.onNodeWithTag("login_button").performClick()
        composeRule.waitForIdle()

        assertEquals("home", navController.currentBackStackEntry?.destination?.route)
        assertEquals(true, fakeLoginViewModel.loginCalled)
        assertEquals(testEmail, fakeLoginViewModel.capturedEmail)
        assertEquals(testPassword, fakeLoginViewModel.capturedPassword)
    }

    @Test
    fun login_failure_stays_on_auth_screen() {
        fakeLoginViewModel.shouldLoginSucceed = false
        fakeLoginViewModel.nextErrorMessage = "Invalid credentials"
        composeRule.onNodeWithTag("email_field").performTextInput("wrong@user.com")
        composeRule.onNodeWithTag("password_field").performTextInput("wrongPassword")
        composeRule.onNodeWithTag("login_button").performClick()
        composeRule.waitForIdle()

        assertEquals("auth", navController.currentBackStackEntry?.destination?.route)
        assertEquals(true, fakeLoginViewModel.loginCalled)
        fakeLoginViewModel.shouldLoginSucceed = true
    }

    @Test
    fun resend_verification_flow_navigates_and_calls_service() {
        val testResendEmail = "unverified@test.com"
        val testResendPassword = "passwordtest"

        fakeLoginViewModel.shouldLoginSucceed = false
        fakeLoginViewModel.nextErrorMessage = verifyMsg

        composeRule.onNodeWithTag("email_field").performTextInput(testResendEmail)
        composeRule.onNodeWithTag("password_field").performTextInput(testResendPassword)
        composeRule.onNodeWithTag("login_button").performClick()
        composeRule.waitForIdle()

        assertEquals("verification", navController.currentBackStackEntry?.destination?.route)
        composeRule.onNodeWithTag("verification_email_field").performTextInput(testResendEmail)
        composeRule.onNodeWithTag("verification_password_field").performTextInput(testResendPassword)
        composeRule.onNodeWithTag("resend_verification_button").performClick()
        composeRule.waitForIdle()
        assertTrue("resendVerificationEmail mora biti pozvan", fakeLoginViewModel.resendCalled)
        assertEquals(testResendEmail, fakeLoginViewModel.resendCapturedEmail)
        assertEquals(testResendPassword, fakeLoginViewModel.resendCapturedPassword)

        fakeLoginViewModel.shouldLoginSucceed = true
        fakeLoginViewModel.nextErrorMessage = null
    }
}

// Fake koji oponasa stvarni AuthViewModel-a
private class FakeLoginViewModel(
    authRepository: AuthRepository,
    firebaseAuth: com.google.firebase.auth.FirebaseAuth,
) : AuthViewModel(authRepository, firebaseAuth) {
    var loginCalled: Boolean = false
    var capturedEmail: String? = null
    var capturedPassword: String? = null
    var shouldLoginSucceed: Boolean = true

    var resendCalled: Boolean = false
    var resendCapturedEmail: String? = null
    var resendCapturedPassword: String? = null
    var nextErrorMessage: String? = null

    private val _uiState = MutableStateFlow(AuthUiState())
    override val uiState = _uiState.asStateFlow()

    override fun login(
        email: String,
        password: String,
    ) {
        loginCalled = true
        capturedEmail = email
        capturedPassword = password

        if (shouldLoginSucceed) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isUserLoggedIn = true,
                    errorMessage = null,
                )
            }
        } else {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isUserLoggedIn = false,
                    errorMessage = nextErrorMessage ?: "Login failed",
                )
            }
        }
    }

    override fun resendVerificationEmail(
        email: String,
        password: String,
        onComplete: (Boolean, String) -> Unit,
    ) {
        resendCalled = true
        resendCapturedEmail = email
        resendCapturedPassword = password

        onComplete(true, "Verification email sent.")
    }
}
