package hr.foi.air.honnomachi.ui.profile

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import hr.foi.air.honnomachi.R
import hr.foi.air.honnomachi.ui.components.ProfileItem
import hr.foi.air.honnomachi.ui.components.errorMessageFor

data class ProfileEditFormCallbacks(
    val onNameChange: (String) -> Unit,
    val onPhoneChange: (String) -> Unit,
    val onStreetChange: (String) -> Unit,
    val onZipChange: (String) -> Unit,
    val onCityChange: (String) -> Unit,
    val onValidateName: () -> Unit,
    val onValidatePhone: () -> Unit,
    val onValidateStreet: () -> Unit,
    val onValidateZip: () -> Unit,
    val onValidateCity: () -> Unit,
)

@Composable
fun ProfileEditForm(
    formState: ProfileFormState,
    userEmail: String,
    callbacks: ProfileEditFormCallbacks,
) {
    ProfileItem(
        label = stringResource(R.string.label_name),
        value = formState.name,
        onValueChange = callbacks.onNameChange,
        isEditable = true,
        keyboardOptions =
            KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Next,
            ),
        errorText = formState.nameError?.let { stringResource(errorMessageFor(it)) },
        onFocusLost = callbacks.onValidateName,
    )

    ProfileItem(
        label = stringResource(R.string.label_email),
        value = userEmail,
        isEditable = false,
    )

    ProfileItem(
        label = stringResource(R.string.label_phone),
        value = formState.phone,
        onValueChange = callbacks.onPhoneChange,
        isEditable = true,
        keyboardOptions =
            KeyboardOptions(
                keyboardType = KeyboardType.Phone,
                imeAction = ImeAction.Next,
            ),
        errorText = formState.phoneError?.let { stringResource(errorMessageFor(it)) },
        onFocusLost = callbacks.onValidatePhone,
    )

    ProfileItem(
        label = stringResource(R.string.label_street),
        value = formState.street,
        onValueChange = callbacks.onStreetChange,
        isEditable = true,
        keyboardOptions =
            KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Next,
            ),
        errorText = formState.streetError?.let { stringResource(errorMessageFor(it)) },
        onFocusLost = callbacks.onValidateStreet,
    )

    ProfileItem(
        label = stringResource(R.string.label_zip),
        value = formState.zip,
        onValueChange = callbacks.onZipChange,
        isEditable = true,
        keyboardOptions =
            KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Next,
            ),
        errorText = formState.zipError?.let { stringResource(errorMessageFor(it)) },
        onFocusLost = callbacks.onValidateZip,
    )

    ProfileItem(
        label = stringResource(R.string.label_city),
        value = formState.city,
        onValueChange = callbacks.onCityChange,
        isEditable = true,
        keyboardOptions =
            KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Done,
            ),
        errorText = formState.cityError?.let { stringResource(errorMessageFor(it)) },
        onFocusLost = callbacks.onValidateCity,
    )
}
