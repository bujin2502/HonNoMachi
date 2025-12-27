package hr.foi.air.honnomachi.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import hr.foi.air.honnomachi.AppUtil
import hr.foi.air.honnomachi.FormValidator
import hr.foi.air.honnomachi.R
import hr.foi.air.honnomachi.ValidationErrorType
import hr.foi.air.honnomachi.ui.components.errorMessageFor
import hr.foi.air.honnomachi.ui.profile.ProfileViewModel
import hr.foi.air.honnomachi.di.ViewModelFactory

@Composable
fun ChangePasswordScreen(
    navController: NavController,
    profileViewModel: ProfileViewModel = viewModel(factory = ViewModelFactory()),
) {
    var oldPass by remember { mutableStateOf("") }
    var newPass by remember { mutableStateOf("") }
    var confirmPass by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    var oldPassError by remember { mutableStateOf<ValidationErrorType?>(null) }
    var newPassError by remember { mutableStateOf<ValidationErrorType?>(null) }
    var confirmPassError by remember { mutableStateOf<ValidationErrorType?>(null) }

    val context = LocalContext.current

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Back Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
        ) {
            IconButton(
                onClick = { navController.popBackStack() },
                modifier =
                    Modifier
                        .size(48.dp)
                        .background(Color(0xFFCFE2F3), shape = RoundedCornerShape(12.dp)),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    modifier = Modifier.size(24.dp),
                    tint = Color.Black,
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Lock Icon
        Icon(
            imageVector = Icons.Filled.LockReset,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary,
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.title_change_password),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 32.dp),
        )

        // Old Password
        OutlinedTextField(
            value = oldPass,
            onValueChange = {
                oldPass = it
                oldPassError = null
            },
            label = { Text(stringResource(R.string.label_old_password)) },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .onFocusChanged { focusState ->
                        if (!focusState.isFocused && oldPass.isNotEmpty()) {
                            val result = FormValidator.validateStrictPassword(oldPass)
                            oldPassError = result.error
                        }
                    },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
            isError = oldPassError != null,
            supportingText = { oldPassError?.let { Text(stringResource(errorMessageFor(it)), color = MaterialTheme.colorScheme.error) } },
        )
        Spacer(modifier = Modifier.height(16.dp))

        // New Password
        OutlinedTextField(
            value = newPass,
            onValueChange = {
                newPass = it
                newPassError = null
            },
            label = { Text(stringResource(R.string.label_new_password)) },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .onFocusChanged { focusState ->
                        if (!focusState.isFocused && newPass.isNotEmpty()) {
                            val result = FormValidator.validateStrictPassword(newPass)
                            newPassError = result.error
                        }
                    },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
            isError = newPassError != null,
            supportingText = { newPassError?.let { Text(stringResource(errorMessageFor(it)), color = MaterialTheme.colorScheme.error) } },
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Confirm Password
        OutlinedTextField(
            value = confirmPass,
            onValueChange = {
                confirmPass = it
                confirmPassError = null
            },
            label = { Text(stringResource(R.string.label_confirm_password)) },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .onFocusChanged { focusState ->
                        if (!focusState.isFocused && confirmPass.isNotEmpty()) {
                            val result = FormValidator.validatePasswordConfirmation(newPass, confirmPass)
                            confirmPassError = result.error
                        }
                    },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            isError = confirmPassError != null,
            supportingText = {
                confirmPassError?.let {
                    Text(
                        stringResource(errorMessageFor(it)),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
        )
        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                val validation = FormValidator.validateChangePasswordForm(oldPass, newPass, confirmPass)
                oldPassError = validation.oldPassword.error
                newPassError = validation.newPassword.error
                confirmPassError = validation.confirmPassword.error

                if (validation.isValid) {
                    isLoading = true
                    profileViewModel.changePassword(oldPass, newPass) { success, message ->
                        isLoading = false
                        if (success) {
                            AppUtil.showToast(context, "Lozinka uspješno promijenjena.")
                            navController.popBackStack()
                        } else {
                            AppUtil.showToast(context, "Greška: ${message ?: "Nepoznata greška"}")
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            enabled = !isLoading,
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            } else {
                Text(stringResource(R.string.button_save))
            }
        }
    }
}
