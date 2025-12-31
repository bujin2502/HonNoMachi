package hr.foi.air.honnomachi.ui.auth

import hr.foi.air.honnomachi.model.UserModel

data class AuthUiState(
    val isLoading: Boolean = false,
    val user: UserModel? = null,
    val errorMessage: String? = null,
    val isUserLoggedIn: Boolean = false,
    val needsVerification: Boolean = false,
)
