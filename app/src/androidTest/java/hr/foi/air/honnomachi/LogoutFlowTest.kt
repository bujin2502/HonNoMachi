package hr.foi.air.honnomachi

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.PaddingValues
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
import hr.foi.air.honnomachi.data.AuthRepository
import hr.foi.air.honnomachi.ui.auth.AuthViewModel
import hr.foi.air.honnomachi.ui.profile.ProfilePage
import io.mockk.mockk
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
        fakeLogoutViewModel = FakeLogoutViewModel(mockk(relaxed = true), mockk(relaxed = true))

        composeRule.setContent {
            val context = LocalContext.current

            navController =
                TestNavHostController(context).apply {
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
private class FakeLogoutViewModel(
    authRepository: AuthRepository,
    firebaseAuth: com.google.firebase.auth.FirebaseAuth,
) : AuthViewModel(authRepository, firebaseAuth) {
    var logoutCalled: Boolean = false

    override fun signOut() {
        logoutCalled = true
    }
}
