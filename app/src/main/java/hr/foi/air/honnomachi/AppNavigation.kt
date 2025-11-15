package hr.foi.air.honnomachi

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import hr.foi.air.honnomachi.screen.AuthScreen
import hr.foi.air.honnomachi.screen.HomeScreen
import hr.foi.air.honnomachi.screen.LoginScreen
import hr.foi.air.honnomachi.screen.SignupScreen

@Composable
fun AppNavigation(modifier: Modifier=Modifier) {

    val navController=rememberNavController()

    val isLoggedIn = Firebase.auth.currentUser != null
    val startDestination = if (isLoggedIn) "home" else "auth"

    NavHost(navController = navController, startDestination = startDestination) {

        composable("auth") {
            AuthScreen(modifier, navController)
        }

        composable("login") {
            LoginScreen(modifier, navController)
        }

        composable("signup") {
            SignupScreen(modifier, navController)
        }

        composable("home") {
            HomeScreen(modifier, navController)
        }
    }
}