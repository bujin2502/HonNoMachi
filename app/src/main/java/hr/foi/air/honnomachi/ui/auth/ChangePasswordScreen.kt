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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import hr.foi.air.honnomachi.AppUtil
import hr.foi.air.honnomachi.R
import hr.foi.air.honnomachi.ui.components.InputFieldError
import hr.foi.air.honnomachi.ui.components.PasswordInputField
import hr.foi.air.honnomachi.ui.profile.ProfileViewModel

@Composable
fun ChangePasswordScreen(
    navController: NavController,
    @Suppress("DEPRECATION")
    profileViewModel: ProfileViewModel = hiltViewModel(),
) {
    val changePasswordState by profileViewModel.changePasswordState.collectAsState()
    val newPasswordFocusRequester = remember { FocusRequester() }
    val confirmPasswordFocusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current

    DisposableEffect(Unit) {
        onDispose {
            profileViewModel.resetChangePasswordForm()
        }
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
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

        PasswordInputField(
            value = changePasswordState.oldPassword,
            onValueChange = { profileViewModel.onOldPasswordChange(it) },
            modifier = Modifier.fillMaxWidth(),
            error = changePasswordState.oldPasswordError?.let { InputFieldError(it) },
            label = stringResource(R.string.label_old_password),
            imeAction = ImeAction.Next,
            onImeAction = { newPasswordFocusRequester.requestFocus() },
        )

        Spacer(modifier = Modifier.height(16.dp))

        PasswordInputField(
            value = changePasswordState.newPassword,
            onValueChange = { profileViewModel.onNewPasswordChange(it) },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .focusRequester(newPasswordFocusRequester),
            error = changePasswordState.newPasswordError?.let { InputFieldError(it) },
            label = stringResource(R.string.label_new_password),
            imeAction = ImeAction.Next,
            onImeAction = { confirmPasswordFocusRequester.requestFocus() },
        )

        Spacer(modifier = Modifier.height(16.dp))

        PasswordInputField(
            value = changePasswordState.confirmPassword,
            onValueChange = { profileViewModel.onConfirmPasswordChange(it) },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .focusRequester(confirmPasswordFocusRequester),
            error = changePasswordState.confirmPasswordError?.let { InputFieldError(it) },
            label = stringResource(R.string.label_confirm_password),
            imeAction = ImeAction.Done,
            onImeAction = { focusManager.clearFocus() },
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                profileViewModel.changePassword { success, message ->
                    if (success) {
                        AppUtil.showToast(context, "Lozinka uspješno promijenjena.")
                        navController.popBackStack()
                    } else {
                        AppUtil.showToast(context, "Greška: ${message ?: "Nepoznata greška"}")
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            enabled = !changePasswordState.isLoading,
        ) {
            if (changePasswordState.isLoading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            } else {
                Text(stringResource(R.string.button_save))
            }
        }
    }
}
