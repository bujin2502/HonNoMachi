package hr.foi.air.honnomachi.ui.auth

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import hr.foi.air.honnomachi.AppUtil
import hr.foi.air.honnomachi.FormValidator
import hr.foi.air.honnomachi.R
import hr.foi.air.honnomachi.ValidationErrorType
import hr.foi.air.honnomachi.ui.components.EmailInputField
import hr.foi.air.honnomachi.ui.components.PasswordInputField

@Composable
fun LoginScreen(
    modifier: Modifier = Modifier,
    navController: NavController,
    @Suppress("DEPRECATION")
    authViewModel: AuthViewModel = hiltViewModel(),
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf<ValidationErrorType?>(null) }
    var passwordError by remember { mutableStateOf<ValidationErrorType?>(null) }
    val passwordFocusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val uiState by authViewModel.uiState.collectAsState()

    LoginAuthEffect(
        uiState = uiState,
        navController = navController,
        onConsumeError = authViewModel::consumeErrorMessage,
    )

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(AuthDimensions.ScreenPadding),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            stringResource(R.string.welcome_back),
            modifier = Modifier.fillMaxWidth(),
            style =
                TextStyle(
                    fontSize = AuthDimensions.TitleFontSize,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                ),
        )

        Spacer(modifier = Modifier.height(AuthDimensions.SmallSpacing))

        Text(
            stringResource(R.string.sign_in_your_account),
            modifier = Modifier.fillMaxWidth(),
            style =
                TextStyle(
                    fontSize = AuthDimensions.SubtitleFontSize,
                ),
        )

        Spacer(modifier = Modifier.height(AuthDimensions.LargeSpacing))

        Image(
            painterResource(id = R.drawable.vecteezy_deconstructing_sign_up_and_log_in_49110285),
            contentDescription = "signup_image",
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(AuthDimensions.ImageHeight),
        )

        Spacer(modifier = Modifier.height(AuthDimensions.LargeSpacing))

        EmailInputField(
            value = email,
            onValueChange = {
                email = it
                if (emailError != null) emailError = null
            },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .testTag("email_field"),
            error = emailError,
            imeAction = ImeAction.Next,
            onImeAction = { passwordFocusRequester.requestFocus() },
            errorTestTag = "login_email_error",
        )

        Spacer(modifier = Modifier.height(AuthDimensions.SmallSpacing))

        PasswordInputField(
            value = password,
            onValueChange = {
                password = it
                if (passwordError != null) passwordError = null
            },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .focusRequester(passwordFocusRequester)
                    .testTag("password_field"),
            error = passwordError,
            imeAction = ImeAction.Done,
            onImeAction = { focusManager.clearFocus() },
            errorTestTag = "login_password_error",
        )

        Spacer(modifier = Modifier.height(AuthDimensions.LargeSpacing))

        LoginButton(
            isLoading = uiState.isLoading,
            onClick = {
                val validation = FormValidator.validateLoginForm(email, password)
                emailError = validation.email.error
                passwordError = validation.password.error
                if (validation.isValid) {
                    authViewModel.login(email, password)
                }
            },
        )

        Spacer(modifier = Modifier.height(AuthDimensions.SmallSpacing))

        TextButton(onClick = { navController.navigate("forgotPassword") }) {
            Text(text = stringResource(R.string.forgot_password_question))
        }
    }
}

@Composable
private fun LoginAuthEffect(
    uiState: AuthUiState,
    navController: NavController,
    onConsumeError: () -> Unit,
) {
    val context = LocalContext.current
    LaunchedEffect(key1 = uiState) {
        handleLoginNavigation(uiState, navController, context, onConsumeError)
    }
}

private fun handleLoginNavigation(
    uiState: AuthUiState,
    navController: NavController,
    context: Context,
    onConsumeError: () -> Unit,
) {
    if (uiState.isUserLoggedIn) {
        navController.navigate("home") {
            popUpTo("auth") { inclusive = true }
        }
    }

    uiState.errorMessage?.let { error ->
        if (error == "Please verify your email before logging in.") {
            navController.navigate("verification") {
                popUpTo("auth") { inclusive = true }
            }
        } else {
            AppUtil.showToast(context, error)
        }
        onConsumeError()
    }
}

@Composable
private fun LoginButton(
    isLoading: Boolean,
    onClick: () -> Unit,
) {
    val buttonText =
        if (isLoading) stringResource(R.string.logging_in) else stringResource(R.string.login)
    Button(
        onClick = onClick,
        enabled = !isLoading,
        modifier =
            Modifier
                .fillMaxWidth()
                .height(AuthDimensions.ButtonHeight)
                .testTag("login_button"),
    ) {
        Text(
            text = buttonText,
            fontSize = AuthDimensions.ButtonFontSize,
        )
    }
}
