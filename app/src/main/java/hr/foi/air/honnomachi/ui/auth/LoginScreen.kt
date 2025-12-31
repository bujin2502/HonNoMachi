package hr.foi.air.honnomachi.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import hr.foi.air.honnomachi.AppUtil
import hr.foi.air.honnomachi.FormValidator
import hr.foi.air.honnomachi.R
import hr.foi.air.honnomachi.ValidationErrorType
import hr.foi.air.honnomachi.ui.components.errorMessageFor

@Composable
fun LoginScreen(
    modifier: Modifier = Modifier,
    navController: NavController,
    authViewModel: AuthViewModel = hiltViewModel(),
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf<ValidationErrorType?>(null) }
    var passwordError by remember { mutableStateOf<ValidationErrorType?>(null) }
    val passwordFocusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val uiState by authViewModel.uiState.collectAsState()

    LaunchedEffect(key1 = uiState) {
        if (uiState.isUserLoggedIn) {
            navController.navigate("home") {
                popUpTo("auth") { inclusive = true }
            }
        }
        uiState.errorMessage?.let {
            if (it == "Please verify your email.") {
                navController.navigate("verification") {
                    popUpTo("auth") { inclusive = true }
                }
            } else {
                AppUtil.showToast(context, it)
            }
            authViewModel.consumeErrorMessage()
        }
    }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            stringResource(R.string.welcome_back),
            modifier = Modifier.fillMaxWidth(),
            style =
                TextStyle(
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                ),
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            stringResource(R.string.sign_in_your_account),
            modifier = Modifier.fillMaxWidth(),
            style =
                TextStyle(
                    fontSize = 22.sp,
                ),
        )
        Spacer(modifier = Modifier.height(20.dp))
        Image(
            painterResource(id = R.drawable.vecteezy_deconstructing_sign_up_and_log_in_49110285),
            contentDescription = "signup_image",
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(200.dp),
        )
        Spacer(modifier = Modifier.height(20.dp))
        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
                if (emailError != null) emailError = null
            },
            label = { Text(stringResource(R.string.email_address)) },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .testTag("email_field"),
            keyboardOptions =
                KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next,
                ),
            keyboardActions =
                KeyboardActions(
                    onNext = { passwordFocusRequester.requestFocus() },
                ),
            isError = emailError != null,
            supportingText = {
                emailError?.let {
                    Text(
                        text = stringResource(errorMessageFor(it)),
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.testTag("login_email_error"),
                    )
                }
            },
        )
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
                if (passwordError != null) passwordError = null
            },
            label = { Text(stringResource(R.string.password)) },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .focusRequester(passwordFocusRequester)
                    .testTag("password_field"),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions =
                KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                ),
            keyboardActions =
                KeyboardActions(
                    onDone = { focusManager.clearFocus() },
                ),
            isError = passwordError != null,
            supportingText = {
                passwordError?.let {
                    Text(
                        text = stringResource(errorMessageFor(it)),
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.testTag("login_password_error"),
                    )
                }
            },
        )
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = {
                val validation = FormValidator.validateLoginForm(email, password)
                emailError = validation.email.error
                passwordError = validation.password.error
                if (validation.isValid) {
                    authViewModel.login(email, password)
                }
            },
            enabled = !uiState.isLoading,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .testTag("login_button"),
        ) {
            Text(text = if (uiState.isLoading) stringResource(R.string.logging_in) else stringResource(R.string.login), fontSize = 22.sp)
        }
        Spacer(modifier = Modifier.height(10.dp))
        TextButton(onClick = { navController.navigate("forgotPassword") }) {
            Text(text = stringResource(R.string.forgot_password_question))
        }
    }
}
