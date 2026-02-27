package hr.foi.air.honnomachi.ui.components

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import hr.foi.air.honnomachi.R

@Composable
fun EmailInputField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    error: InputFieldError? = null,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: () -> Unit = {},
    label: String = stringResource(R.string.email_address),
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier,
        keyboardOptions =
            KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = imeAction,
            ),
        keyboardActions =
            KeyboardActions(
                onNext = { onImeAction() },
                onDone = { onImeAction() },
            ),
        isError = error != null,
        supportingText = {
            error?.let {
                val errorModifier =
                    if (it.testTag != null) {
                        Modifier.testTag(it.testTag)
                    } else {
                        Modifier
                    }
                Text(
                    text = stringResource(errorMessageFor(it.type)),
                    color = MaterialTheme.colorScheme.error,
                    modifier = errorModifier,
                )
            }
        },
        singleLine = true,
    )
}
