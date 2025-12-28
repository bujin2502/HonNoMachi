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
import hr.foi.air.honnomachi.ui.auth.AuthViewModel
import hr.foi.air.honnomachi.ui.auth.SignupScreen
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SignupFlowTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var navController: TestNavHostController
    private lateinit var fakeAuthViewModel: FakeAuthViewModel

    @Before
    fun setup() {
        fakeAuthViewModel = FakeAuthViewModel()
        composeRule.setContent {
            val context = LocalContext.current
            navController =
                TestNavHostController(context).apply {
                    navigatorProvider.addNavigator(ComposeNavigator())
                }

            NavHost(navController = navController, startDestination = "signup") {
                composable("signup") {
                    SignupScreen(navController = navController, authViewModel = fakeAuthViewModel)
                }
                composable("login") {
                    Text("Login Screen")
                }
                composable("verification") {
                    Text("Verification Screen")
                }
            }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun shows_validation_errors_when_fields_are_empty() {
        composeRule.onNodeWithTag("signup_button").performClick()

        composeRule.onNodeWithTag("signup_email_error", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithTag("signup_name_error", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithTag("signup_password_error", useUnmergedTree = true).assertExists()
        assertEquals(null, fakeAuthViewModel.lastSignup)
    }

    @Test
    fun successful_signup_navigates_to_verification() {
        composeRule.onNodeWithTag("signup_email").performTextInput("user@example.com")
        composeRule.onNodeWithTag("signup_name").performTextInput("Test User")
        composeRule.onNodeWithTag("signup_password").performTextInput("secret123")

        composeRule.onNodeWithTag("signup_button").performClick()
        composeRule.waitForIdle()

        assertEquals("verification", navController.currentBackStackEntry?.destination?.route)
        assertEquals(Triple("user@example.com", "Test User", "secret123"), fakeAuthViewModel.lastSignup)
    }

    @Test
    fun email_already_exists_stays_on_signup() {
        fakeAuthViewModel.nextResult = false
        fakeAuthViewModel.nextErrorMessage = "Email already exists"

        composeRule.onNodeWithTag("signup_email").performTextInput("exists@example.com")
        composeRule.onNodeWithTag("signup_name").performTextInput("Existing User")
        composeRule.onNodeWithTag("signup_password").performTextInput("secret123")

        composeRule.onNodeWithTag("signup_button").performClick()
        composeRule.waitForIdle()

        assertEquals("signup", navController.currentBackStackEntry?.destination?.route)
        assertEquals(Triple("exists@example.com", "Existing User", "secret123"), fakeAuthViewModel.lastSignup)
    }

    @Test
    fun weak_password_shows_validation_error() {
        composeRule.onNodeWithTag("signup_email").performTextInput("user@example.com")
        composeRule.onNodeWithTag("signup_name").performTextInput("Test User")
        composeRule.onNodeWithTag("signup_password").performTextInput("123")

        composeRule.onNodeWithTag("signup_button").performClick()

        composeRule.onNodeWithTag("signup_password_error", useUnmergedTree = true).assertExists()
        assertEquals(null, fakeAuthViewModel.lastSignup)
        assertEquals("signup", navController.currentBackStackEntry?.destination?.route)
    }

    @Test
    fun invalid_email_format_shows_validation_error() {
        composeRule.onNodeWithTag("signup_email").performTextInput("userexample.com")
        composeRule.onNodeWithTag("signup_name").performTextInput("Test User")
        composeRule.onNodeWithTag("signup_password").performTextInput("secret123")

        composeRule.onNodeWithTag("signup_button").performClick()

        composeRule.onNodeWithTag("signup_email_error", useUnmergedTree = true).assertExists()
        assertEquals(null, fakeAuthViewModel.lastSignup)
        assertEquals("signup", navController.currentBackStackEntry?.destination?.route)
    }
}

private class FakeAuthViewModel : AuthViewModel() {
    var lastSignup: Triple<String, String, String>? = null
    var nextResult: Boolean = true
    var nextErrorMessage: String? = null

    override fun signup(
        email: String,
        name: String,
        password: String,
        onResult: (Boolean, String?) -> Unit,
    ) {
        lastSignup = Triple(email, name, password)
        onResult(nextResult, nextErrorMessage)
    }
}
