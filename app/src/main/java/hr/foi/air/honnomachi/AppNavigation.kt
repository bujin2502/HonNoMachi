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
import hr.foi.air.honnomachi.ui.cart.CheckoutCancelScreen
import hr.foi.air.honnomachi.ui.cart.CheckoutSuccessScreen
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
                uiState.isUserLoggedIn -> ROUTE_HOME
                uiState.needsVerification -> ROUTE_VERIFICATION
                else -> ROUTE_AUTH
            }

        val currentRoute =
            navController.currentBackStackEntry
                ?.destination
                ?.route
                ?.substringBefore("?")

        if (currentRoute == ROUTE_CHECKOUT_SUCCESS || currentRoute == ROUTE_CHECKOUT_CANCEL) {
            return@LaunchedEffect
        }

        if (route == currentRoute) {
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

    NavHost(navController = navController, startDestination = ROUTE_AUTH) {
        composable(ROUTE_AUTH) {
            AuthScreen(modifier, navController, authViewModel)
        }

        composable(ROUTE_LOGIN) {
            LoginScreen(modifier, navController, authViewModel)
        }

        composable(ROUTE_SIGNUP) {
            SignupScreen(modifier, navController, authViewModel)
        }

        composable(ROUTE_VERIFICATION) {
            EmailVerificationScreen(
                onNavigateToLogin = {
                    navController.navigate(ROUTE_LOGIN) {
                        popUpTo(ROUTE_AUTH) { inclusive = true }
                    }
                },
                authViewModel = authViewModel,
            )
        }

        composable(ROUTE_FORGOT_PASSWORD) {
            ForgotPasswordScreen(navController, authViewModel)
        }

        composable(ROUTE_HOME) {
            HomeScreen(navController, authViewModel, homeViewModel)
        }

        composable(ROUTE_CHANGE_PASSWORD) {
            ChangePasswordScreen(navController = navController)
        }

        composable(ROUTE_PRIVACY_POLICY) {
            PrivacyPolicyScreen(onNavigateBack = { navController.navigateUp() })
        }

        composable(route = ROUTE_CHECKOUT_SUCCESS) {
            CheckoutSuccessScreen(
                onNavigateHome = {
                    navController.navigate(ROUTE_HOME) {
                        popUpTo(navController.graph.startDestinationId) { inclusive = true }
                        launchSingleTop = true
                    }
                },
            )
        }

        composable(route = ROUTE_CHECKOUT_CANCEL) {
            CheckoutCancelScreen(
                onNavigateHome = {
                    navController.navigate(ROUTE_HOME) {
                        popUpTo(navController.graph.startDestinationId) { inclusive = true }
                        launchSingleTop = true
                    }
                },
            )
        }

        composable(
            "bookDetail/{bookId}",
            arguments = listOf(navArgument("bookId") { type = NavType.StringType }),
        ) { backStackEntry ->
            BookDetailScreen(bookId = backStackEntry.arguments?.getString("bookId"))
        }

        // Admin ruta - lista korisnika (HNM-289)
        // Guard logika: provjerava admin status prije prikaza ekrana
        composable(ROUTE_ADMIN) {
            val adminViewModel: AdminViewModel = hiltViewModel()
            val isAdmin by adminViewModel.isAdminChecked.collectAsState()
            val context = LocalContext.current

            when (isAdmin) {
                // Provjera u tijeku - prikaži loading indikator
                null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
                // Korisnik nije admin - preusmjeri na početni ekran
                false -> {
                    val accessDeniedMsg = stringResource(R.string.error_admin_access_denied)
                    LaunchedEffect(Unit) {
                        AppUtil.showToast(context, accessDeniedMsg)
                        navController.navigate(ROUTE_HOME) {
                            popUpTo(ROUTE_HOME) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                }
                // Korisnik je admin - prikaži admin ekran
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

        // Admin ruta - detalji korisnika (HNM-289)
        // Prima userId kao navigacijski argument
        composable(
            "admin/userDetail/{userId}",
            arguments = listOf(navArgument("userId") { type = NavType.StringType }),
        ) { backStackEntry ->
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
                        navController.navigate(ROUTE_HOME) {
                            popUpTo(ROUTE_HOME) { inclusive = true }
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

private const val ROUTE_AUTH = "auth"
private const val ROUTE_LOGIN = "login"
private const val ROUTE_SIGNUP = "signup"
private const val ROUTE_VERIFICATION = "verification"
private const val ROUTE_FORGOT_PASSWORD = "forgotPassword"
private const val ROUTE_HOME = "home"
private const val ROUTE_CHANGE_PASSWORD = "changePassword"
private const val ROUTE_PRIVACY_POLICY = "privacyPolicy"
private const val ROUTE_ADMIN = "admin"
private const val ROUTE_CHECKOUT_SUCCESS = "checkout/success"
private const val ROUTE_CHECKOUT_CANCEL = "checkout/cancel"
