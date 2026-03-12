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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import hr.foi.air.honnomachi.AppUtil
import hr.foi.air.honnomachi.FormValidator
import hr.foi.air.honnomachi.R
import hr.foi.air.honnomachi.ValidationErrorType
import hr.foi.air.honnomachi.ui.components.EmailInputField
import hr.foi.air.honnomachi.ui.components.InputFieldError
import hr.foi.air.honnomachi.ui.components.NameInputField
import hr.foi.air.honnomachi.ui.components.PasswordInputField

@Composable
fun SignupScreen(
    modifier: Modifier = Modifier,
    navController: NavController,
    @Suppress("DEPRECATION")
    authViewModel: AuthViewModel = hiltViewModel(),
) {
    var email by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf<ValidationErrorType?>(null) }
    var nameError by remember { mutableStateOf<ValidationErrorType?>(null) }
    var passwordError by remember { mutableStateOf<ValidationErrorType?>(null) }
    val uiState by authViewModel.uiState.collectAsState()

    val nameFocusRequester = remember { FocusRequester() }
    val passwordFocusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    SignupAuthEffect(
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
            stringResource(R.string.hallo_there),
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
            stringResource(R.string.create_an_account),
            modifier = Modifier.fillMaxWidth(),
            style =
                TextStyle(
                    fontSize = AuthDimensions.SubtitleFontSize,
                ),
        )

        Spacer(modifier = Modifier.height(AuthDimensions.LargeSpacing))

        Image(
            painterResource(id = R.drawable.vecteezy_deconstructing_sign_up_and_log_in_49110285),
            contentDescription = "signup_slika",
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
                    .testTag("signup_email"),
            error = emailError?.let { InputFieldError(it, "signup_email_error") },
            imeAction = ImeAction.Next,
            onImeAction = { nameFocusRequester.requestFocus() },
        )

        Spacer(modifier = Modifier.height(AuthDimensions.SmallSpacing))

        NameInputField(
            value = name,
            onValueChange = {
                name = it
                if (nameError != null) nameError = null
            },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .focusRequester(nameFocusRequester)
                    .testTag("signup_name"),
            error = nameError?.let { InputFieldError(it, "signup_name_error") },
            imeAction = ImeAction.Next,
            onImeAction = { passwordFocusRequester.requestFocus() },
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
                    .testTag("signup_password"),
            error = passwordError?.let { InputFieldError(it, "signup_password_error") },
            imeAction = ImeAction.Done,
            onImeAction = { focusManager.clearFocus() },
        )

        Spacer(modifier = Modifier.height(AuthDimensions.LargeSpacing))

        SignupButton(
            isLoading = uiState.isLoading,
            onClick = {
                val validation = FormValidator.validateSignupForm(email, name, password)
                emailError = validation.email.error
                nameError = validation.name.error
                passwordError = validation.password.error
                if (validation.isValid) {
                    authViewModel.signup(name, email, password)
                }
            },
        )
    }
}

@Composable
private fun SignupAuthEffect(
    uiState: AuthUiState,
    navController: NavController,
    onConsumeError: () -> Unit,
) {
    val context = LocalContext.current
    LaunchedEffect(key1 = uiState.needsVerification) {
        handleSignupNavigation(uiState, navController, context, onConsumeError)
    }
}

private fun handleSignupNavigation(
    uiState: AuthUiState,
    navController: NavController,
    context: Context,
    onConsumeError: () -> Unit,
) {
    if (uiState.needsVerification) {
        AppUtil.showToast(context, R.string.verification_email_sent)
        navController.navigate("verification") {
            popUpTo("auth") { inclusive = true }
        }
    }
    uiState.errorMessage?.let {
        AppUtil.showToast(context, it)
        onConsumeError()
    }
}

@Composable
private fun SignupButton(
    isLoading: Boolean,
    onClick: () -> Unit,
) {
    val buttonText =
        if (isLoading) stringResource(R.string.creating_account) else stringResource(R.string.signup)
    Button(
        onClick = onClick,
        enabled = !isLoading,
        modifier =
            Modifier
                .fillMaxWidth()
                .height(AuthDimensions.ButtonHeight)
                .testTag("signup_button"),
    ) {
        Text(
            text = buttonText,
            fontSize = AuthDimensions.ButtonFontSize,
        )
    }
}
