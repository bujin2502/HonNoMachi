package hr.foi.air.honnomachi.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.TextButton
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import hr.foi.air.honnomachi.AppUtil
import hr.foi.air.honnomachi.R
import hr.foi.air.honnomachi.viewmodel.AuthViewModel
import androidx.compose.ui.platform.testTag

@Composable
fun LoginScreen(modifier: Modifier=Modifier, navController: NavController, authViewModel: AuthViewModel=viewModel()){

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val passwordFocusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    var context = LocalContext.current

    var isLoading by remember { mutableStateOf(false) }
    var showResendButton by remember { mutableStateOf(false) }


    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            stringResource(R.string.welcome_back),
            modifier = Modifier.fillMaxWidth(),
            style = TextStyle(
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            ))

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            stringResource(R.string.sign_in_your_account),
            modifier = Modifier.fillMaxWidth(),
            style = TextStyle(
                fontSize = 22.sp
            ))

        Spacer(modifier = Modifier.height(20.dp))

        Image(painterResource(id = R.drawable.vecteezy_deconstructing_sign_up_and_log_in_49110285),
            contentDescription = "signup_slika",
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(
            value = email, onValueChange = {
                email = it
            },
            label = { Text(stringResource(R.string.email_address)) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("email_field"),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { passwordFocusRequester.requestFocus() }
            )
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = password, onValueChange = {
                password = it
            },
            label = { Text(stringResource(R.string.password)) },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(passwordFocusRequester)
                .testTag("password_field"),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = { focusManager.clearFocus() }
            )
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(onClick = {
            isLoading = true
            authViewModel.login(email, password) { success, errorMessage ->
                isLoading = false
                if (success) {
                    navController.navigate("home") {
                        popUpTo("auth") { inclusive = true }
                    }
                } else {
                    val message = errorMessage ?: "Something went wrong"
                    AppUtil.showToast(context, message)
                    if (message == "Please verify your email before logging in.") {
                        showResendButton = true
                    }
                }
            }
        },
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .testTag("login_button")
        ) {
            Text(text = if(isLoading) stringResource(R.string.logging_in) else stringResource(R.string.login), fontSize = 22.sp)
        }

        if (showResendButton) {
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = {
                    authViewModel.resendVerificationEmail(email, password) { success, message ->
                        AppUtil.showToast(context, message ?: "An unexpected error occurred.")
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
            ) {
                Text(text = stringResource(id = R.string.resend_verification_email), fontSize = 16.sp)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        TextButton(onClick = { navController.navigate("forgotPassword") }) {
            Text(text = stringResource(R.string.forgot_password) + "?")
        }
    }
}