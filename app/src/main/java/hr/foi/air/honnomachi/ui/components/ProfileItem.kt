package hr.foi.air.honnomachi.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
    val borderColor = if (errorText != null) Color.Red else Color.Transparent
    val borderWidth = if (errorText != null) 2.dp else 0.dp
    var hasFocus by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .border(width = borderWidth, color = borderColor, shape = RoundedCornerShape(12.dp))
            .background(
                color = Color(0xFFCFE2F3), // Light blue from wireframe
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Black
        )
        if (isEditable && onValueChange != null) {
            BasicTextField(
                value = value ?: "",
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 18.sp,
                    color = Color.Black
                ),
                cursorBrush = SolidColor(Color.Black),
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { focusState ->
                        if (hasFocus && !focusState.isFocused) {
                            onFocusLost()
                        }
                        hasFocus = focusState.isFocused
                    },
                keyboardOptions = keyboardOptions
            )
        } else {
            Text(
                text = value ?: "-",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 18.sp
                ),
                color = if (isEditable) Color.Black else Color.Gray
            )
        }
        
        if (errorText != null) {
            Text(
                text = errorText,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Red,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
