package hr.foi.air.honnomachi.viewmodel

import hr.foi.air.honnomachi.ValidationErrorType
import hr.foi.air.honnomachi.model.UserModel

sealed interface ProfileUiState {
    data object Loading : ProfileUiState

    data class Success(
        val user: UserModel,
    ) : ProfileUiState

    data class Error(
        val message: String,
    ) : ProfileUiState
}

data class ProfileFormState(
    val name: String = "",
    val nameError: ValidationErrorType? = null,
    val phone: String = "",
    val phoneError: ValidationErrorType? = null,
    val street: String = "",
    val streetError: ValidationErrorType? = null,
    val city: String = "",
    val cityError: ValidationErrorType? = null,
    val zip: String = "",
    val zipError: ValidationErrorType? = null,
    val analyticsEnabled: Boolean = true,
    val isSaving: Boolean = false,
    val isFormValid: Boolean = true, // aggregated validity
)
