package hr.foi.air.honnomachi

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import hr.foi.air.honnomachi.ui.admin.AdminUserDetailScreen
import hr.foi.air.honnomachi.ui.admin.AdminUserListScreen
import hr.foi.air.honnomachi.ui.admin.AdminViewModel
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
import hr.foi.air.honnomachi.ui.suspended.SuspendedAccountScreen

@Composable
fun AppNavigation(
    modifier: Modifier = Modifier,
    navController: NavHostController,
) {
    @Suppress("DEPRECATION")
    val authViewModel: AuthViewModel = hiltViewModel()

    @Suppress("DEPRECATION")
    val homeViewModel: HomeViewModel = hiltViewModel()

    val uiState by authViewModel.uiState.collectAsState()

    LaunchedEffect(uiState.isUserLoggedIn, uiState.needsVerification, uiState.isSuspended) {
        val route =
            when {
                uiState.isSuspended -> "suspended"
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
            popUpTo(navController.graph.startDestinationId) { inclusive = true }
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

        composable("suspended") {
            SuspendedAccountScreen(
                reason = uiState.suspendedReason,
                onSignOut = {
                    authViewModel.consumeSuspendedState()
                    authViewModel.signOut()
                },
            )
        }

        composable("admin") {
            @Suppress("DEPRECATION")
            val adminViewModel: AdminViewModel = hiltViewModel()
            val isAdmin by adminViewModel.isAdminChecked.collectAsState()
            val context = LocalContext.current

            when (isAdmin) {
                null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
                false -> {
                    val accessDeniedMsg = stringResource(R.string.error_admin_access_denied)
                    LaunchedEffect(Unit) {
                        AppUtil.showToast(context, accessDeniedMsg)
                        navController.navigate("home") {
                            popUpTo("home") { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                }
                true -> {
                    AdminUserListScreen(
                        onNavigateBack = { navController.navigateUp() },
                        onNavigateToUserDetail = { userId ->
                            navController.navigate("admin/userDetail/$userId")
                        },
                    )
                }
            }
        }

        composable(
            "admin/userDetail/{userId}",
            arguments = listOf(navArgument("userId") { type = NavType.StringType }),
        ) {
            @Suppress("DEPRECATION")
            val adminViewModel: AdminViewModel = hiltViewModel()
            val isAdmin by adminViewModel.isAdminChecked.collectAsState()
            val context = LocalContext.current

            when (isAdmin) {
                null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
                false -> {
                    val accessDeniedMsg = stringResource(R.string.error_admin_access_denied)
                    LaunchedEffect(Unit) {
                        AppUtil.showToast(context, accessDeniedMsg)
                        navController.navigate("home") {
                            popUpTo("home") { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                }
                true -> {
                    AdminUserDetailScreen(
                        onNavigateBack = { navController.navigateUp() },
                    )
                }
            }
        }
    }
}
