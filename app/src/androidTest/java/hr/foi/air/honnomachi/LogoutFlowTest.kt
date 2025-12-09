package hr.foi.air.honnomachi

import androidx.compose.foundation.layout.PaddingValues
import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.testing.TestNavHostController
import androidx.test.ext.junit.runners.AndroidJUnit4
import hr.foi.air.honnomachi.pages.ProfilePage
import hr.foi.air.honnomachi.viewmodel.AuthViewModel
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LogoutFlowTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var navController: TestNavHostController
    private lateinit var fakeLogoutViewModel: FakeLogoutViewModel

    @Before
    fun setup() {
        fakeLogoutViewModel = FakeLogoutViewModel()

        composeRule.setContent {
            val context = LocalContext.current

            navController = TestNavHostController(context).apply {
                navigatorProvider.addNavigator(ComposeNavigator())
            }

            NavHost(navController = navController, startDestination = "profile") {
                composable("profile") {
                    ProfilePage(navController = navController, authViewModel = fakeLogoutViewModel, paddingValues = PaddingValues())
                }
                composable("auth") {
                    Text("Auth Screen (Login/Signup)")
                }
                composable("home") {
                    Text("Home Screen")
                }
            }
        }

        composeRule.waitForIdle()
    }

    @Test
    fun logout_navigates_to_auth() {
        composeRule.onNodeWithTag("logout_button").performClick()
        composeRule.waitForIdle()

        assertEquals("auth", navController.currentBackStackEntry?.destination?.route)
        assertEquals(true, fakeLogoutViewModel.logoutCalled)
    }
}

// Fake koji oponasa ponasanje stvarnog AuthViewModel-a
private class FakeLogoutViewModel : AuthViewModel() {
    var logoutCalled: Boolean = false
    override fun signOut() {
        logoutCalled = true
    }
}
