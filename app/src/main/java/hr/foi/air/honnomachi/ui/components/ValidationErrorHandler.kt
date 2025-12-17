package hr.foi.air.honnomachi.ui.components

import androidx.compose.runtime.Composable
import hr.foi.air.honnomachi.R
import hr.foi.air.honnomachi.ValidationErrorType

@Composable
fun errorMessageFor(error: ValidationErrorType): Int {
    return when (error) {
        ValidationErrorType.EMPTY_EMAIL -> R.string.error_email_required
        ValidationErrorType.INVALID_EMAIL -> R.string.error_email_invalid
        ValidationErrorType.EMPTY_NAME -> R.string.error_name_required
        ValidationErrorType.SHORT_NAME -> R.string.error_name_short
        ValidationErrorType.EMPTY_PASSWORD -> R.string.error_password_required
        ValidationErrorType.SHORT_PASSWORD -> R.string.error_password_short
        ValidationErrorType.EMPTY_PHONE -> R.string.error_phone_required
        ValidationErrorType.EMPTY_STREET -> R.string.error_street_required
        ValidationErrorType.EMPTY_CITY -> R.string.error_city_required
        ValidationErrorType.EMPTY_ZIP -> R.string.error_zip_required
        ValidationErrorType.PASSWORDS_DO_NOT_MATCH -> R.string.error_passwords_do_not_match
    }
}
