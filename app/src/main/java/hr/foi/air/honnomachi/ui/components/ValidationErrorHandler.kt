package hr.foi.air.honnomachi.ui.components

import androidx.compose.runtime.Composable
import hr.foi.air.honnomachi.R
import hr.foi.air.honnomachi.ValidationErrorType

@Composable
fun errorMessageFor(error: ValidationErrorType): Int =
    when (error) {
        ValidationErrorType.EMPTY_EMAIL -> R.string.error_email_required
        ValidationErrorType.INVALID_EMAIL -> R.string.error_email_invalid
        ValidationErrorType.EMPTY_NAME -> R.string.error_name_required
        ValidationErrorType.SHORT_NAME -> R.string.error_name_short
        ValidationErrorType.EMPTY_PASSWORD -> R.string.error_password_required
        ValidationErrorType.SHORT_PASSWORD -> R.string.error_password_short
    }
