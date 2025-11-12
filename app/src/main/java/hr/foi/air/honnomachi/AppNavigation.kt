package hr.foi.air.honnomachi

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import hr.foi.air.honnomachi.screen.AuthScreen
import hr.foi.air.honnomachi.screen.LoginScreen
import hr.foi.air.honnomachi.screen.SignupScreen

@Composable
fun AppNavigation(modifier: Modifier=Modifier) {

    val navcontroller=rememberNavController()

    NavHost(navController = navcontroller, startDestination = "auth") {

        composable("auth") {
            AuthScreen(modifier, navcontroller)
        }

        composable("login") {
            LoginScreen(modifier)
        }

        composable("signup") {
            SignupScreen(modifier)
        }
    }
}