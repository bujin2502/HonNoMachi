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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import hr.foi.air.honnomachi.R
import hr.foi.air.honnomachi.ValidationErrorType

@Composable
fun PasswordInputField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    error: ValidationErrorType? = null,
    imeAction: ImeAction = ImeAction.Done,
    onImeAction: () -> Unit = {},
    label: String = stringResource(R.string.password),
    errorTestTag: String? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions =
            KeyboardOptions(
                keyboardType = KeyboardType.Password,
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
                    if (errorTestTag != null) {
                        Modifier.testTag(errorTestTag)
                    } else {
                        Modifier
                    }
                Text(
                    text = stringResource(errorMessageFor(it)),
                    color = MaterialTheme.colorScheme.error,
                    modifier = errorModifier,
                )
            }
        },
        singleLine = true,
    )
}
