package hr.foi.air.honnomachi.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun ProfileItem(
    label: String,
    value: String?,
    onValueChange: ((String) -> Unit)? = null,
    isEditable: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    errorText: String? = null,
    onFocusLost: () -> Unit = {}
) {
    var hasFocus by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = value ?: "",
        onValueChange = { onValueChange?.invoke(it) },
        label = { Text(label) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp)
            .onFocusChanged { focusState ->
                if (hasFocus && !focusState.isFocused) {
                    onFocusLost()
                }
                hasFocus = focusState.isFocused
            },
        enabled = isEditable && onValueChange != null,
        singleLine = true,
        keyboardOptions = keyboardOptions,
        isError = errorText != null,
        supportingText = {
            if (errorText != null) {
                Text(
                    text = errorText,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    )
}