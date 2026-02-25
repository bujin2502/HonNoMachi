package hr.foi.air.honnomachi.ui.admin

import hr.foi.air.honnomachi.model.UserModel

sealed interface AdminUserDetailUiState {
    data object Loading : AdminUserDetailUiState

    data class Success(
        val user: UserModel,
        val showSuspendDialog: Boolean = false,
        val showReactivateDialog: Boolean = false,
        val isActionLoading: Boolean = false,
        val actionMessage: String? = null,
    ) : AdminUserDetailUiState

    data class Error(
        val message: String,
    ) : AdminUserDetailUiState
}
