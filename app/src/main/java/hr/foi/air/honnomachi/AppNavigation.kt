package hr.foi.air.honnomachi

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import hr.foi.air.honnomachi.di.ViewModelFactory
import hr.foi.air.honnomachi.ui.auth.AuthScreen
import hr.foi.air.honnomachi.ui.auth.AuthViewModel
import hr.foi.air.honnomachi.ui.auth.ChangePasswordScreen
import hr.foi.air.honnomachi.ui.auth.EmailVerificationScreen
import hr.foi.air.honnomachi.ui.auth.ForgotPasswordScreen
import hr.foi.air.honnomachi.ui.auth.LoginScreen
import hr.foi.air.honnomachi.ui.auth.SignupScreen
import hr.foi.air.honnomachi.ui.book.BookDetailScreen
import hr.foi.air.honnomachi.ui.home.HomeScreen
import hr.foi.air.honnomachi.ui.home.HomeViewModel
import hr.foi.air.honnomachi.ui.policy.PrivacyPolicyScreen
import kotlinx.coroutines.delay

@Composable
fun AppNavigation(
    modifier: Modifier = Modifier,
    navController: NavHostController,
) {
    val authViewModel: AuthViewModel = viewModel()
    val homeViewModel: HomeViewModel = viewModel(factory = ViewModelFactory())
    val context = LocalContext.current

    val currentUser = Firebase.auth.currentUser
    val startDestination =
        when {
            currentUser == null -> "auth"
            currentUser.isEmailVerified -> "home"
            else -> "verification"
        }

    val navBackStackEntry by navController.currentBackStackEntryAsState()

    // automatsko pracenje naziva trenutnog ekrana za Crashlytics
    DisposableEffect(navBackStackEntry) {
        val currentScreen = navBackStackEntry?.destination?.route ?: "Unknown"
        CrashlyticsManager.instance.updateCurrentScreen(currentScreen)
        onDispose { }
    }

    LaunchedEffect(Unit) {
        while (true) {
            authViewModel.checkSession {
                AppUtil.showToast(context, "Session expired. Please login again.")
                navController.navigate("auth") {
                    popUpTo(0) { inclusive = true }
                }
            }
            delay(5000)
        }
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable("auth") {
            AuthScreen(modifier, navController, authViewModel)
        }

        composable("login") {
            LoginScreen(modifier, navController, authViewModel)
        }

        composable("signup") {
            SignupScreen(modifier, navController, authViewModel)
        }

        composable("verification") {
            EmailVerificationScreen(
                onNavigateToLogin = {
                    navController.navigate("login") {
                        popUpTo("auth") { inclusive = true }
                    }
                },
                authViewModel = authViewModel,
            )
        }

        composable("forgotPassword") {
            ForgotPasswordScreen(navController, authViewModel)
        }

        composable("home") {
            HomeScreen(modifier, navController, authViewModel, homeViewModel)
        }

        composable("changePassword") {
            ChangePasswordScreen(navController = navController)
        }

        composable("privacyPolicy") {
            PrivacyPolicyScreen(onNavigateBack = { navController.navigateUp() })
        }

        composable(
            "bookDetail/{bookId}",
            arguments = listOf(navArgument("bookId") { type = NavType.StringType }),
        ) { backStackEntry ->
            BookDetailScreen(bookId = backStackEntry.arguments?.getString("bookId"))
        }
    }
}
