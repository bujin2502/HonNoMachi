package hr.foi.air.honnomachi.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import hr.foi.air.honnomachi.AppUtil
import hr.foi.air.honnomachi.R
import hr.foi.air.honnomachi.ui.components.ProfileItem
import hr.foi.air.honnomachi.ui.components.errorMessageFor
import hr.foi.air.honnomachi.viewmodel.ProfileUiState
import hr.foi.air.honnomachi.viewmodel.ProfileViewModel
import hr.foi.air.honnomachi.viewmodel.ViewModelFactory

@Composable
fun ProfileScreen(
    paddingValues: PaddingValues,
    onLogout: () -> Unit,
    onNavigateToChangePassword: () -> Unit,
    profileViewModel: ProfileViewModel = viewModel(factory = ViewModelFactory()),
) {
    val context = LocalContext.current
    val uiState by profileViewModel.uiState.collectAsState()
    val formState by profileViewModel.formState.collectAsState()

    // Determine if form has changes
    val hasChanges =
        if (uiState is ProfileUiState.Success) {
            val user = (uiState as ProfileUiState.Success).user
            formState.name != user.name ||
                formState.phone != (user.phoneNumber ?: "") ||
                formState.street != (user.street ?: "") ||
                formState.zip != (user.postNumber ?: "") ||
                formState.city != (user.city ?: "")
        } else {
            false
        }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
    ) {
        // Header with Status (Left) and Logout (Right)
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Status Section
            if (uiState is ProfileUiState.Success) {
                val user = (uiState as ProfileUiState.Success).user
                val isSuspended = user.suspended == true
                val statusText = if (isSuspended) stringResource(R.string.value_suspended) else stringResource(R.string.value_active)
                val statusColor = if (isSuspended) Color.Red else Color(0xFF4CAF50) // Green

                Column {
                    Text(
                        text = stringResource(R.string.label_status),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                    )
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                    )
                }
            } else {
                Spacer(modifier = Modifier.width(1.dp)) // Placeholder to keep Logout to the right
            }

            // Logout Button
            Button(
                onClick = onLogout,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFCFE2F3)),
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

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // User Icon (based on Admin role)
            val icon =
                if (uiState is ProfileUiState.Success && (uiState as ProfileUiState.Success).user.admin == true) {
                    Icons.Default.ManageAccounts
                } else {
                    Icons.Default.Person
                }
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

                    // Editable Fields
                    ProfileItem(
                        label = stringResource(R.string.label_name),
                        value = formState.name,
                        onValueChange = profileViewModel::onNameChange,
                        isEditable = true,
                        keyboardOptions =
                            KeyboardOptions(
                                capitalization = KeyboardCapitalization.Words,
                                imeAction = ImeAction.Next,
                            ),
                        errorText = formState.nameError?.let { stringResource(errorMessageFor(it)) },
                        onFocusLost = profileViewModel::validateName,
                    )

                    // Read-only Email
                    ProfileItem(
                        label = stringResource(R.string.label_email),
                        value = user.email,
                        isEditable = false,
                    )

                    // Editable Phone
                    ProfileItem(
                        label = stringResource(R.string.label_phone),
                        value = formState.phone,
                        onValueChange = profileViewModel::onPhoneChange,
                        isEditable = true,
                        keyboardOptions =
                            KeyboardOptions(
                                keyboardType = KeyboardType.Phone,
                                imeAction = ImeAction.Next,
                            ),
                        errorText = formState.phoneError?.let { stringResource(errorMessageFor(it)) },
                        onFocusLost = profileViewModel::validatePhone,
                    )

                    // Editable Address Fields
                    ProfileItem(
                        label = stringResource(R.string.label_street),
                        value = formState.street,
                        onValueChange = profileViewModel::onStreetChange,
                        isEditable = true,
                        keyboardOptions =
                            KeyboardOptions(
                                capitalization = KeyboardCapitalization.Sentences,
                                imeAction = ImeAction.Next,
                            ),
                        errorText = formState.streetError?.let { stringResource(errorMessageFor(it)) },
                        onFocusLost = profileViewModel::validateStreet,
                    )
                    ProfileItem(
                        label = stringResource(R.string.label_zip),
                        value = formState.zip,
                        onValueChange = profileViewModel::onZipChange,
                        isEditable = true,
                        keyboardOptions =
                            KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Next,
                            ),
                        errorText = formState.zipError?.let { stringResource(errorMessageFor(it)) },
                        onFocusLost = profileViewModel::validateZip,
                    )
                    ProfileItem(
                        label = stringResource(R.string.label_city),
                        value = formState.city,
                        onValueChange = profileViewModel::onCityChange,
                        isEditable = true,
                        keyboardOptions =
                            KeyboardOptions(
                                capitalization = KeyboardCapitalization.Words,
                                imeAction = ImeAction.Done,
                            ),
                        errorText = formState.cityError?.let { stringResource(errorMessageFor(it)) },
                        onFocusLost = profileViewModel::validateCity,
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

            Spacer(modifier = Modifier.height(16.dp))

            // Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                OutlinedButton(
                    onClick = onNavigateToChangePassword,
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                ) {
                    Text(text = stringResource(R.string.button_reset_password))
                }

                Button(
                    enabled = hasChanges && !formState.isSaving,
                    onClick = {
                        profileViewModel.saveProfile { success, message ->
                            if (success) {
                                AppUtil.showToast(context, "Profil uspješno ažuriran!")
                            } else {
                                AppUtil.showToast(context, "Greška: $message")
                            }
                        }
                    },
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.weight(1f).padding(start = 8.dp),
                ) {
                    if (formState.isSaving) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text(text = stringResource(R.string.button_save))
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
