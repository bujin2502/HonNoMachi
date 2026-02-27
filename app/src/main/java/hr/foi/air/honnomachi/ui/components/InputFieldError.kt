package hr.foi.air.honnomachi.ui.components

import hr.foi.air.honnomachi.ValidationErrorType

data class InputFieldError(
    val type: ValidationErrorType,
    val testTag: String? = null,
)
