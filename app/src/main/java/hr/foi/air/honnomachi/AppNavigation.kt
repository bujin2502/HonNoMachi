import androidx.compose.runtime.Composable
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
import hr.foi.air.honnomachi.AppUtil
import hr.foi.air.honnomachi.screen.AuthScreen
import hr.foi.air.honnomachi.screen.BookDetailScreen
import hr.foi.air.honnomachi.screen.EmailVerificationScreen
import hr.foi.air.honnomachi.screen.ForgotPasswordScreen
import hr.foi.air.honnomachi.screen.HomeScreen
import hr.foi.air.honnomachi.screen.LoginScreen
import hr.foi.air.honnomachi.screen.SignupScreen
import hr.foi.air.honnomachi.viewmodel.AuthViewModel
import hr.foi.air.honnomachi.viewmodel.HomeViewModel
import hr.foi.air.honnomachi.viewmodel.ViewModelFactory

@Composable
fun AppNavigation(
    modifier: Modifier = Modifier,
    navController: NavHostController
) {
    val authViewModel: AuthViewModel = viewModel()
    val homeViewModel: HomeViewModel = viewModel(factory = ViewModelFactory())
    val context = LocalContext.current

    val currentUser = Firebase.auth.currentUser
    val startDestination = when {
        currentUser == null -> "auth"
        currentUser.isEmailVerified -> "home"
        else -> "verification"
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()

    LaunchedEffect(Unit) {
        while (true) {
            authViewModel.checkSession {
                AppUtil.showToast(context, "Session expired. Please login again.")
                navController.navigate("auth") {
                    popUpTo(0) { inclusive = true }
                }
            }
            kotlinx.coroutines.delay(5000)
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
                authViewModel = authViewModel
            )
        }

        composable("forgotPassword") {
            ForgotPasswordScreen(navController, authViewModel)
        }

        composable("home") {
            HomeScreen(modifier, navController, authViewModel, homeViewModel)
        }

        composable("changePassword") {
            hr.foi.air.honnomachi.screen.ChangePasswordScreen(navController = navController)
        }

        composable(
            "bookDetail/{bookId}",
            arguments = listOf(navArgument("bookId") { type = NavType.StringType })
        ) { backStackEntry ->
            BookDetailScreen(bookId = backStackEntry.arguments?.getString("bookId"))
        }
    }
}