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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.MaterialTheme
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import hr.foi.air.honnomachi.AppUtil
import hr.foi.air.honnomachi.FormValidator
import hr.foi.air.honnomachi.R
import hr.foi.air.honnomachi.ValidationErrorType
import hr.foi.air.honnomachi.ui.components.errorMessageFor
import hr.foi.air.honnomachi.viewmodel.AuthViewModel

@Composable
fun SignupScreen(modifier: Modifier = Modifier, navController: NavController, authViewModel: AuthViewModel = viewModel()) {

    var email by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf<ValidationErrorType?>(null) }
    var nameError by remember { mutableStateOf<ValidationErrorType?>(null) }
    var passwordError by remember { mutableStateOf<ValidationErrorType?>(null) }

    val verificationEmailSentMessage = stringResource(id = R.string.verification_email_sent)
    val somethingWentWrongMessage = stringResource(id = R.string.something_went_wrong)
    val context = LocalContext.current

    var isLoading by remember { mutableStateOf(false) }



    val nameFocusRequester = remember { FocusRequester() }
    val passwordFocusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            stringResource(R.string.hallo_there),
            modifier = Modifier.fillMaxWidth(),
            style = TextStyle(
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            stringResource(R.string.create_an_account),
            modifier = Modifier.fillMaxWidth(),
            style = TextStyle(
                fontSize = 22.sp
            )
        )

        Spacer(modifier = Modifier.height(20.dp))

        Image(
            painterResource(id = R.drawable.vecteezy_deconstructing_sign_up_and_log_in_49110285),
            contentDescription = "signup_slika",
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(
            value = email, onValueChange = {
                email = it
                if (emailError != null) emailError = null
            },
            label = { Text(stringResource(R.string.email_address)) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("signup_email"),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { nameFocusRequester.requestFocus() }
            ),
            isError = emailError != null,
            supportingText = {
                emailError?.let {
                    Text(
                        text = stringResource(errorMessageFor(it)),
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.testTag("signup_email_error")
                    )
                }
            }
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = name, onValueChange = {
                name = it
                if (nameError != null) nameError = null
            },
            label = { Text(stringResource(R.string.name)) },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(nameFocusRequester)
                .testTag("signup_name"),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { passwordFocusRequester.requestFocus() }
            ),
            isError = nameError != null,
            supportingText = {
                nameError?.let {
                    Text(
                        text = stringResource(errorMessageFor(it)),
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.testTag("signup_name_error")
                    )
                }
            }
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = password, onValueChange = {
                password = it
                if (passwordError != null) passwordError = null
            },
            label = { Text(stringResource(R.string.password)) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("signup_password")
                .focusRequester(passwordFocusRequester),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = { focusManager.clearFocus() }
            ),
            isError = passwordError != null,
            supportingText = {
                passwordError?.let {
                    Text(
                        text = stringResource(errorMessageFor(it)),
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.testTag("signup_password_error")
                    )
                }
            }
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                val validation = FormValidator.validateSignupForm(email, name, password)
                emailError = validation.email.error
                nameError = validation.name.error
                passwordError = validation.password.error
                if (!validation.isValid) {
                    isLoading = false
                    return@Button
                }
                isLoading = true
                authViewModel.signup(email, name, password) { success, errorMessage ->
                    isLoading = false
                    if (success) {
                        AppUtil.showToast(context, verificationEmailSentMessage)
                        navController.navigate("verification") {
                            popUpTo("auth") { inclusive = true }
                        }
                    } else {
                        AppUtil.showToast(context, errorMessage ?: somethingWentWrongMessage)
                    }
                }
            },
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .testTag("signup_button")
        ) {
            Text(if (isLoading) stringResource(R.string.creating_account) else stringResource(R.string.signup), fontSize = 22.sp)
        }
    }
}
