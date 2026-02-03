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

@Composable
fun ProfileEditForm(
    formState: ProfileFormState,
    userEmail: String,
    onNameChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onStreetChange: (String) -> Unit,
    onZipChange: (String) -> Unit,
    onCityChange: (String) -> Unit,
    onValidateName: () -> Unit,
    onValidatePhone: () -> Unit,
    onValidateStreet: () -> Unit,
    onValidateZip: () -> Unit,
    onValidateCity: () -> Unit,
) {
    ProfileItem(
        label = stringResource(R.string.label_name),
        value = formState.name,
        onValueChange = onNameChange,
        isEditable = true,
        keyboardOptions =
            KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Next,
            ),
        errorText = formState.nameError?.let { stringResource(errorMessageFor(it)) },
        onFocusLost = onValidateName,
    )

    ProfileItem(
        label = stringResource(R.string.label_email),
        value = userEmail,
        isEditable = false,
    )

    ProfileItem(
        label = stringResource(R.string.label_phone),
        value = formState.phone,
        onValueChange = onPhoneChange,
        isEditable = true,
        keyboardOptions =
            KeyboardOptions(
                keyboardType = KeyboardType.Phone,
                imeAction = ImeAction.Next,
            ),
        errorText = formState.phoneError?.let { stringResource(errorMessageFor(it)) },
        onFocusLost = onValidatePhone,
    )

    ProfileItem(
        label = stringResource(R.string.label_street),
        value = formState.street,
        onValueChange = onStreetChange,
        isEditable = true,
        keyboardOptions =
            KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Next,
            ),
        errorText = formState.streetError?.let { stringResource(errorMessageFor(it)) },
        onFocusLost = onValidateStreet,
    )

    ProfileItem(
        label = stringResource(R.string.label_zip),
        value = formState.zip,
        onValueChange = onZipChange,
        isEditable = true,
        keyboardOptions =
            KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Next,
            ),
        errorText = formState.zipError?.let { stringResource(errorMessageFor(it)) },
        onFocusLost = onValidateZip,
    )

    ProfileItem(
        label = stringResource(R.string.label_city),
        value = formState.city,
        onValueChange = onCityChange,
        isEditable = true,
        keyboardOptions =
            KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Done,
            ),
        errorText = formState.cityError?.let { stringResource(errorMessageFor(it)) },
        onFocusLost = onValidateCity,
    )
}
