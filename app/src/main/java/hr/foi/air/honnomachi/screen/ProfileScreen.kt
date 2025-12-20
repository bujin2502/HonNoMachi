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
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import hr.foi.air.honnomachi.FormValidator
import hr.foi.air.honnomachi.R
import hr.foi.air.honnomachi.ValidationErrorType
import hr.foi.air.honnomachi.model.UserModel
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
    profileViewModel: ProfileViewModel = viewModel(factory = ViewModelFactory())
) {
    val context = LocalContext.current
    val uiState by profileViewModel.uiState.collectAsState()

    // Form State
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var street by remember { mutableStateOf("") }
    var zip by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }

    // Error State
    var nameError by remember { mutableStateOf<ValidationErrorType?>(null) }
    var phoneError by remember { mutableStateOf<ValidationErrorType?>(null) }
    var streetError by remember { mutableStateOf<ValidationErrorType?>(null) }
    var zipError by remember { mutableStateOf<ValidationErrorType?>(null) }
    var cityError by remember { mutableStateOf<ValidationErrorType?>(null) }

    // Initialize state when data is loaded
    LaunchedEffect(uiState) {
        if (uiState is ProfileUiState.Success) {
            val user = (uiState as ProfileUiState.Success).user
            name = user.name
            phone = user.phoneNumber ?: ""
            street = user.street ?: ""
            zip = user.postNumber ?: ""
            city = user.city ?: ""
        }
    }

    val hasChanges = if (uiState is ProfileUiState.Success) {
        val user = (uiState as ProfileUiState.Success).user
        name != user.name ||
                phone != (user.phoneNumber ?: "") ||
                street != (user.street ?: "") ||
                zip != (user.postNumber ?: "") ||
                city != (user.city ?: "")
    } else false

    val isFormValid = nameError == null && phoneError == null && streetError == null && zipError == null && cityError == null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .verticalScroll(rememberScrollState())
    ) {
        // Header with Status (Left) and Logout (Right)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
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
                        color = Color.Gray
                    )
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = statusColor,
                        fontWeight = FontWeight.Bold
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
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = stringResource(R.string.label_logout),
                        tint = Color.Black
                    )
                    Spacer(modifier = Modifier.size(4.dp))
                    Text(
                        text = stringResource(R.string.label_logout),
                        color = Color.Black,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // User Icon (based on Admin role)
            val icon = if (uiState is ProfileUiState.Success && (uiState as ProfileUiState.Success).user.admin == true) {
                Icons.Default.ManageAccounts
            } else {
                Icons.Default.Person
            }
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.label_my_data),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
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
                        value = name,
                        onValueChange = { 
                            name = it
                            nameError = null // Reset error on change
                        },
                        isEditable = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Next
                        ),
                        errorText = nameError?.let { stringResource(errorMessageFor(it)) },
                        onFocusLost = {
                            val result = FormValidator.validateName(name)
                            nameError = result.error
                        }
                    )
                    
                    // Read-only Email
                    ProfileItem(
                        label = stringResource(R.string.label_email),
                        value = user.email,
                        isEditable = false
                    )
                    
                    // Editable Phone
                    ProfileItem(
                        label = stringResource(R.string.label_phone),
                        value = phone,
                        onValueChange = { 
                            if (it.length <= 16) {
                                phone = it
                                phoneError = null
                            }
                        },
                        isEditable = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Phone,
                            imeAction = ImeAction.Next
                        ),
                        errorText = phoneError?.let { stringResource(errorMessageFor(it)) },
                        onFocusLost = {
                            val result = FormValidator.validatePhone(phone)
                            phoneError = result.error
                        }
                    )
                    
                    // Editable Address Fields
                    ProfileItem(
                        label = stringResource(R.string.label_street),
                        value = street,
                        onValueChange = { 
                            street = it
                            streetError = null
                        },
                        isEditable = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Next
                        ),
                        errorText = streetError?.let { stringResource(errorMessageFor(it)) },
                        onFocusLost = {
                            val result = FormValidator.validateStreet(street)
                            streetError = result.error
                        }
                    )
                    ProfileItem(
                        label = stringResource(R.string.label_zip),
                        value = zip,
                        onValueChange = { 
                            if (it.length <= 5) {
                                zip = it
                                zipError = null
                            }
                        },
                        isEditable = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        ),
                        errorText = zipError?.let { stringResource(errorMessageFor(it)) },
                        onFocusLost = {
                            val result = FormValidator.validateZip(zip)
                            zipError = result.error
                        }
                    )
                    ProfileItem(
                        label = stringResource(R.string.label_city),
                        value = city,
                        onValueChange = { 
                            city = it
                            cityError = null
                        },
                        isEditable = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Done
                        ),
                        errorText = cityError?.let { stringResource(errorMessageFor(it)) },
                        onFocusLost = {
                            val result = FormValidator.validateCity(city)
                            cityError = result.error
                        }
                    )
                }
                is ProfileUiState.Error -> {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OutlinedButton(
                    onClick = onNavigateToChangePassword,
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                ) {
                    Text(text = stringResource(R.string.button_reset_password))
                }

                Button(
                    enabled = hasChanges && !isSaving && isFormValid,
                    onClick = {
                        if (uiState is ProfileUiState.Success) {
                            // Final validation before save
                            val validationResult = FormValidator.validateProfileEditForm(name, phone, street, city, zip)
                            if (!validationResult.isValid) {
                                nameError = validationResult.name.error
                                phoneError = validationResult.phone.error
                                streetError = validationResult.street.error
                                zipError = validationResult.zip.error
                                cityError = validationResult.city.error
                                AppUtil.showToast(context, "Molimo ispravno popunite sva polja.")
                            } else {
                                isSaving = true
                                val currentUser = (uiState as ProfileUiState.Success).user
                                val updatedUser = currentUser.copy(
                                    name = name,
                                    phoneNumber = phone,
                                    street = street,
                                    postNumber = zip,
                                    city = city
                                )
                                profileViewModel.updateUserProfile(updatedUser) { success, message ->
                                    isSaving = false
                                    if (success) {
                                        AppUtil.showToast(context, "Profil uspješno ažuriran!")
                                    } else {
                                        AppUtil.showToast(context, "Greška: $message")
                                    }
                                }
                            }
                        }
                    },
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.weight(1f).padding(start = 8.dp)
                ) {
                    if (isSaving) {
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