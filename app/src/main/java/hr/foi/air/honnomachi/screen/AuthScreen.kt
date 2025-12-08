package hr.foi.air.honnomachi.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import hr.foi.air.honnomachi.AppUtil
import hr.foi.air.honnomachi.R
import hr.foi.air.honnomachi.ui.components.OrDivider
import hr.foi.air.honnomachi.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    authViewModel: AuthViewModel = viewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    var isGoogleLoading by remember { mutableStateOf(false) }

    // Hoist the string resource out of the coroutine launch block
    val serverClientId = stringResource(id = R.string.default_web_client_id)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(painterResource(id = R.drawable.hnm_logo),
            contentDescription = "slika",
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            stringResource(R.string.welcome),
            style = TextStyle(
                fontSize = 30.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace
            ))

        Spacer(modifier = Modifier.height(15.dp))

        Text(
            stringResource(R.string.the_best_platform_to_buy_books),
            style = TextStyle(
                textAlign = TextAlign.Center,
            ))

        Spacer(modifier = Modifier.height(50.dp))

        Button(onClick = {
            navController.navigate("login")
        },
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)) {
            Text(stringResource(R.string.login), fontSize = 22.sp)
        }

        Spacer(modifier = Modifier.height(20.dp))

        OutlinedButton(
            onClick = {
                isGoogleLoading = true
                coroutineScope.launch {
                    try {
                        val googleIdOption = GetGoogleIdOption.Builder()
                            .setFilterByAuthorizedAccounts(false)
                            // Use the variable here instead of the composable function
                            .setServerClientId(serverClientId)
                            .setAutoSelectEnabled(true)
                            .build()

                        val request = GetCredentialRequest.Builder()
                            .addCredentialOption(googleIdOption)
                            .build()

                        val result = credentialManager.getCredential(
                            request = request,
                            context = context
                        )
                        val googleIdTokenCredential =
                            GoogleIdTokenCredential.createFrom(result.credential.data)

                        authViewModel.loginWithGoogle(googleIdTokenCredential.idToken) { success, errorMessage ->
                            isGoogleLoading = false
                            if (success) {
                                navController.navigate("home") {
                                    popUpTo("auth") { inclusive = true }
                                }
                            } else {
                                AppUtil.showToast(context, errorMessage ?: "Google sign-in failed.")
                            }
                        }
                    } catch (e: GetCredentialException) {
                        isGoogleLoading = false
                        AppUtil.showToast(context, e.message ?: "Google sign-in canceled.")
                    } catch (e: Exception) {
                        isGoogleLoading = false
                        AppUtil.showToast(context, e.message ?: "Google sign-in failed.")
                    }
                }
            },
            enabled = !isGoogleLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
        ) {
            Text(
                text = if (isGoogleLoading) stringResource(R.string.signing_in) else stringResource(R.string.continue_with_google),
                fontSize = 18.sp
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        OrDivider()

        Spacer(modifier = Modifier.height(20.dp))

        OutlinedButton(onClick = {
            navController.navigate("signup")
        },
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)) {
            Text(stringResource(R.string.signup), fontSize = 22.sp)
        }
    }
}
