package hr.foi.air.honnomachi.ui.auth

import hr.foi.air.honnomachi.model.UserModel

data class OperationResult(
    val success: Boolean,
    val message: String,
)

data class AuthUiState(
    val isLoading: Boolean = false,
    val user: UserModel? = null,
    val errorMessage: String? = null,
    val isUserLoggedIn: Boolean = false,
    val needsVerification: Boolean = false,
    val isSuspended: Boolean = false,
    val suspendedReason: String? = null,
    val secureReadResult: String? = null,
    val googleLoginResult: OperationResult? = null,
    val verificationEmailResult: OperationResult? = null,
    val verificationStatusResult: OperationResult? = null,
)
