package hr.foi.air.honnomachi

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
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

@Composable
fun AppNavigation(
    modifier: Modifier = Modifier,
    navController: NavHostController,
) {
    val authViewModel: AuthViewModel = hiltViewModel()
    val homeViewModel: HomeViewModel = hiltViewModel()

    val uiState by authViewModel.uiState.collectAsState()

    LaunchedEffect(uiState.isUserLoggedIn, uiState.needsVerification) {
        val route =
            when {
                uiState.isUserLoggedIn -> "home"
                uiState.needsVerification -> "verification"
                else -> "auth"
            }

        val currentRoute =
            navController.currentBackStackEntry
                ?.destination
                ?.route
                ?.substringBefore("/")
        val destinationRoute = route.substringBefore("/")
        if (destinationRoute == currentRoute) {
            return@LaunchedEffect
        }

        navController.navigate(route) {
            // Briše sve prethodne ekrane sa stacka
            popUpTo(navController.graph.startDestinationId) { inclusive = true }
            // Osigurava da se ne stvori nova instanca ako smo već na cilju
            launchSingleTop = true
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    DisposableEffect(navBackStackEntry) {
        val currentScreen = navBackStackEntry?.destination?.route ?: "Unknown"
        CrashlyticsManager.instance.updateCurrentScreen(currentScreen)
        onDispose { }
    }

    NavHost(navController = navController, startDestination = "auth") {
        // Svi vaši `composable` pozivi ostaju potpuno isti
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
            HomeScreen(navController, authViewModel, homeViewModel)
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
