package hr.foi.air.honnomachi

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import hr.foi.air.honnomachi.ui.theme.HonNoMachiTheme
import kotlinx.coroutines.flow.MutableSharedFlow

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val deepLinkIntents = MutableSharedFlow<Intent>(extraBufferCapacity = 1)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val navController = rememberNavController()

            LaunchedEffect(navController) {
                navController.currentBackStackEntryFlow.collect { backStackEntry ->
                    requestedOrientation =
                        when (backStackEntry.destination.route) {
                            "home", "changePassword" -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                            else -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                        }
                }
            }

            LaunchedEffect(navController) {
                deepLinkIntents.collect { incomingIntent ->
                    navController.handleDeepLink(incomingIntent)
                }
            }

            HonNoMachiTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    AppNavigation(
                        modifier = Modifier.padding(innerPadding),
                        navController = navController,
                    )
                }
            }
        }

        intent?.let { launchIntent ->
            deepLinkIntents.tryEmit(launchIntent)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        deepLinkIntents.tryEmit(intent)
    }
}
