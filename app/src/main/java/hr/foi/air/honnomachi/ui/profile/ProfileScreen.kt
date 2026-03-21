package hr.foi.air.honnomachi.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Person
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import hr.foi.air.honnomachi.AppUtil
import hr.foi.air.honnomachi.R
import hr.foi.air.honnomachi.ui.theme.LabelGray
import hr.foi.air.honnomachi.ui.theme.LogoutButtonBackground
import hr.foi.air.honnomachi.ui.suspension.LocalIsSuspended
import hr.foi.air.honnomachi.ui.theme.StatusActive
import hr.foi.air.honnomachi.ui.theme.StatusSuspended
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    paddingValues: PaddingValues,
    onLogout: () -> Unit,
    onNavigateToChangePassword: () -> Unit,
    onNavigateToPrivacyPolicy: () -> Unit,
    onNavigateToAdmin: () -> Unit,
    @Suppress("DEPRECATION")
    profileViewModel: ProfileViewModel = hiltViewModel(),
) {
    val isSuspended = LocalIsSuspended.current
    val context = LocalContext.current
    val uiState by profileViewModel.uiState.collectAsState()
    val formState by profileViewModel.formState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val suspendedEditMessage = stringResource(R.string.suspended_cannot_edit_profile)

    val successState = uiState as? ProfileUiState.Success
    val isAdmin = successState?.user?.admin == true
    val hasChanges = computeHasChanges(uiState, formState)
    val icon = if (isAdmin) Icons.Default.ManageAccounts else Icons.Default.Person

    val lastSnackbarTime = remember { mutableLongStateOf(0L) }
    val showSuspendedSnackbar: () -> Unit = {
        val now = System.currentTimeMillis()
        if (now - lastSnackbarTime.longValue > 4000L) {
            lastSnackbarTime.longValue = now
            scope.launch { snackbarHostState.showSnackbar(suspendedEditMessage) }
        }
    }

    val suspendedFieldColors =
        if (isSuspended) {
            OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = StatusSuspended,
                focusedBorderColor = StatusSuspended,
            )
        } else {
            null
        }
    val suspendedBorder =
        if (isSuspended) BorderStroke(1.dp, StatusSuspended) else null

    val successMessage = stringResource(R.string.profile_update_success)
    val errorMessageFormat = stringResource(R.string.profile_update_error)
    val onSaveClick: () -> Unit = {
        if (!isSuspended) {
            profileViewModel.saveProfile { success, message ->
                if (success) {
                    AppUtil.showToast(context, successMessage)
                } else {
                    AppUtil.showToast(context, String.format(errorMessageFormat, message))
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier.padding(paddingValues),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            ProfileHeaderRow(uiState = uiState, onLogout = onLogout)
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .pointerInput(isSuspended) {
                        if (!isSuspended) return@pointerInput
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                if (event.changes.any { it.pressed && !it.previousPressed }) {
                                    showSuspendedSnackbar()
                                }
                            }
                        }
                    }
                    .verticalScroll(rememberScrollState()),
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.label_my_data),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(12.dp))

                when (val state = uiState) {
                    is ProfileUiState.Loading -> {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }

                    is ProfileUiState.Success -> {
                        val user = state.user

                        ProfileEditForm(
                            formState = formState,
                            userEmail = user.email,
                            isEditable = true,
                            callbacks =
                                ProfileEditFormCallbacks(
                                    onNameChange = { if (isSuspended) showSuspendedSnackbar() else profileViewModel.onNameChange(it) },
                                    onPhoneChange = { if (isSuspended) showSuspendedSnackbar() else profileViewModel.onPhoneChange(it) },
                                    onStreetChange = { if (isSuspended) showSuspendedSnackbar() else profileViewModel.onStreetChange(it) },
                                    onZipChange = { if (isSuspended) showSuspendedSnackbar() else profileViewModel.onZipChange(it) },
                                    onCityChange = { if (isSuspended) showSuspendedSnackbar() else profileViewModel.onCityChange(it) },
                                    onValidateName = profileViewModel::validateName,
                                    onValidatePhone = profileViewModel::validatePhone,
                                    onValidateStreet = profileViewModel::validateStreet,
                                    onValidateZip = profileViewModel::validateZip,
                                    onValidateCity = profileViewModel::validateCity,
                                ),
                            colors = suspendedFieldColors,
                        )
                    }

                    is ProfileUiState.Error -> {
                        Text(
                            text = state.message,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = stringResource(R.string.label_privacy_settings),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.label_analytics_consent),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = stringResource(R.string.desc_analytics_consent),
                            style = MaterialTheme.typography.bodySmall,
                            color = LabelGray,
                        )
                    }
                    Switch(
                        checked = formState.analyticsEnabled,
                        onCheckedChange = {
                            profileViewModel.onAnalyticsToggled(it)
                        },
                        enabled = true,
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.label_notifications_consent),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = stringResource(R.string.desc_notifications_consent),
                            style = MaterialTheme.typography.bodySmall,
                            color = LabelGray,
                        )
                    }
                    Switch(
                        checked = formState.notificationsEnabled,
                        onCheckedChange = {
                            profileViewModel.onNotificationsToggled(it)
                        },
                        enabled = true,
                    )
                }

                TextButton(onClick = onNavigateToPrivacyPolicy) {
                    Text(text = stringResource(id = R.string.title_privacy_policy))
                }

                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))

                if (isAdmin) {
                    Button(
                        onClick = onNavigateToAdmin,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.ManageAccounts,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = stringResource(R.string.button_admin_panel))
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    OutlinedButton(
                        onClick = {
                            if (!isSuspended) {
                                onNavigateToChangePassword()
                            }
                        },
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.weight(1f).padding(end = 8.dp),
                        border = suspendedBorder ?: ButtonDefaults.outlinedButtonBorder,
                    ) {
                        Text(text = stringResource(R.string.button_reset_password))
                    }

                    SaveButton(
                        hasChanges = hasChanges,
                        isSaving = formState.isSaving,
                        modifier = Modifier.weight(1f).padding(start = 8.dp),
                        onClick = onSaveClick,
                        border = suspendedBorder,
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

private fun computeHasChanges(
    uiState: ProfileUiState,
    formState: ProfileFormState,
): Boolean {
    if (uiState !is ProfileUiState.Success) return false
    val user = uiState.user
    return formState.name != user.name ||
        formState.phone != (user.phoneNumber ?: "") ||
        formState.street != (user.street ?: "") ||
        formState.zip != (user.postNumber ?: "") ||
        formState.city != (user.city ?: "")
}

@Composable
private fun ProfileHeaderRow(
    uiState: ProfileUiState,
    onLogout: () -> Unit,
) {
    Surface {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (uiState is ProfileUiState.Success) {
                val isSuspended = uiState.user.suspended == true
                val statusText =
                    if (isSuspended) stringResource(R.string.value_suspended) else stringResource(R.string.value_active)
                val statusColor = if (isSuspended) StatusSuspended else StatusActive
                Column {
                    Text(
                        text = stringResource(R.string.label_status),
                        style = MaterialTheme.typography.labelSmall,
                        color = LabelGray,
                    )
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                    )
                }
            } else {
                Spacer(modifier = Modifier.width(1.dp))
            }

            Button(
                onClick = onLogout,
                colors = ButtonDefaults.buttonColors(containerColor = LogoutButtonBackground),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = stringResource(R.string.label_logout),
                        tint = Color.Black,
                    )
                    Spacer(modifier = Modifier.size(4.dp))
                    Text(
                        text = stringResource(R.string.label_logout),
                        color = Color.Black,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    )
                }
            }
        }
    }
}

@Composable
private fun SaveButton(
    hasChanges: Boolean,
    isSaving: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    border: BorderStroke? = null,
) {
    Button(
        enabled = hasChanges && !isSaving,
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        modifier = modifier,
        border = border,
    ) {
        if (isSaving) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(24.dp),
            )
        } else {
            Text(text = stringResource(R.string.button_save))
        }
    }
}
