package hr.foi.air.honnomachi.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import hr.foi.air.honnomachi.R
import hr.foi.air.honnomachi.viewmodel.AuthViewModel

private const val SHOW_QA_BUTTON = false

@Composable
fun ProfilePage(
    paddingValues: PaddingValues,
    navController: NavController,
    authViewModel: AuthViewModel = viewModel(),
) {
    val message = remember { mutableStateOf("") }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = stringResource(R.string.profile_page))
        Button(
            onClick = {
                authViewModel.signOut()
                navController.navigate(route = "auth") {
                    popUpTo("home") { inclusive = true }
                }
            },
            modifier = Modifier.testTag("logout_button"),
        ) {
            Text(stringResource(id = R.string.logout))
        }
        if (SHOW_QA_BUTTON) {
            Button(
                onClick = {
                    authViewModel.testSecureRead(
                        onSuccess = { message.value = "Secure read OK — token is valid" },
                        onError = { e -> message.value = "Error: ${e.message}" },
                    )
                },
            ) {
                Text(stringResource(id = R.string.test_token_qa))
            }
            if (message.value.isNotEmpty()) {
                Text(text = message.value)
            }
        }
    }
}
