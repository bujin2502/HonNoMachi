package hr.foi.air.honnomachi.ui.admin

import hr.foi.air.honnomachi.model.UserModel

sealed interface AdminUserDetailUiState {
    data object Loading : AdminUserDetailUiState

    data class Success(
        val user: UserModel,
    ) : AdminUserDetailUiState

    data class Error(
        val message: String,
    ) : AdminUserDetailUiState
}
