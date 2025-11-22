package hr.foi.air.honnomachi

import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.testing.TestNavHostController
import androidx.test.ext.junit.runners.AndroidJUnit4
import hr.foi.air.honnomachi.screen.SignupScreen
import hr.foi.air.honnomachi.viewmodel.AuthViewModel
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
        composeRule.setContent {
            val context = LocalContext.current
            navController = TestNavHostController(context).apply {
                navigatorProvider.addNavigator(ComposeNavigator())
            }
            fakeAuthViewModel = FakeAuthViewModel()

            NavHost(navController = navController, startDestination = "signup") {
                composable("signup") {
                    SignupScreen(navController = navController, authViewModel = fakeAuthViewModel)
                }
                composable("login") {
                    Text("Login Screen")
                }
            }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun shows_validation_errors_when_fields_are_empty() {
        composeRule.onNodeWithTag("signup_button").performClick()

        composeRule.onNodeWithTag("signup_email_error").assertExists()
        composeRule.onNodeWithTag("signup_name_error").assertExists()
        composeRule.onNodeWithTag("signup_password_error").assertExists()
        assertEquals(null, fakeAuthViewModel.lastSignup)
    }

    @Test
    fun successful_signup_navigates_to_login() {
        composeRule.onNodeWithTag("signup_email").performTextInput("user@example.com")
        composeRule.onNodeWithTag("signup_name").performTextInput("Test User")
        composeRule.onNodeWithTag("signup_password").performTextInput("secret123")

        composeRule.onNodeWithTag("signup_button").performClick()
        composeRule.waitForIdle()

        assertEquals("login", navController.currentBackStackEntry?.destination?.route)
        assertEquals(Triple("user@example.com", "Test User", "secret123"), fakeAuthViewModel.lastSignup)
    }
}

private class FakeAuthViewModel : AuthViewModel() {
    var lastSignup: Triple<String, String, String>? = null

    override fun signup(
        email: String,
        name: String,
        password: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        lastSignup = Triple(email, name, password)
        onResult(true, null)
    }
}
