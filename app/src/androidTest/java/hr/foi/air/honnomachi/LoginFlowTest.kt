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
import hr.foi.air.honnomachi.screen.LoginScreen
import hr.foi.air.honnomachi.viewmodel.AuthViewModel
import org.junit.Assert.assertEquals
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
    private val TEST_EMAIL = "test@example.com"
    private val TEST_PASSWORD = "password123"


    @Before
    fun setup() {
        fakeLoginViewModel = FakeLoginViewModel()

        composeRule.setContent {
            val context = LocalContext.current

            navController = TestNavHostController(context).apply {
                navigatorProvider.addNavigator(ComposeNavigator())
            }

            NavHost(navController = navController, startDestination = "auth") {

                composable("auth") {
                    LoginScreen(navController = navController, authViewModel = fakeLoginViewModel)
                }

                composable("home") {
                    Text("Home Screen")
                }
            }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun login_successful_navigates_to_home() {
        composeRule.onNodeWithTag("email_field").performTextInput(TEST_EMAIL)
        composeRule.onNodeWithTag("password_field").performTextInput(TEST_PASSWORD)
        composeRule.onNodeWithTag("login_button").performClick()
        composeRule.waitForIdle()

        assertEquals("home", navController.currentBackStackEntry?.destination?.route)
        assertEquals(true, fakeLoginViewModel.loginCalled)
        assertEquals(TEST_EMAIL, fakeLoginViewModel.capturedEmail)
        assertEquals(TEST_PASSWORD, fakeLoginViewModel.capturedPassword)
    }
    @Test
    fun login_failure_stays_on_auth_screen() {
        fakeLoginViewModel.shouldLoginSucceed = false
        composeRule.onNodeWithTag("email_field").performTextInput("wrong@user.com")
        composeRule.onNodeWithTag("password_field").performTextInput("wrongPassword")
        composeRule.onNodeWithTag("login_button").performClick()
        composeRule.waitForIdle()

        assertEquals("auth", navController.currentBackStackEntry?.destination?.route)
        assertEquals(true, fakeLoginViewModel.loginCalled)
        fakeLoginViewModel.shouldLoginSucceed = true
    }
}

// Fake koji oponasa stvarni AuthViewModel-a
private class FakeLoginViewModel : AuthViewModel() {
    var loginCalled: Boolean = false
    var capturedEmail: String? = null
    var capturedPassword: String? = null
    var shouldLoginSucceed: Boolean = true

    override fun login(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        loginCalled = true
        capturedEmail = email
        capturedPassword = password

        if (shouldLoginSucceed) {
            onResult(true, null)
        } else {
            onResult(false, "Invalid credentials")
        }
    }

    override fun resendVerificationEmail(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
    }
}