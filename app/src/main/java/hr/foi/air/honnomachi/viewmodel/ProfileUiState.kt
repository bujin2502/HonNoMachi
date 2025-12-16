package hr.foi.air.honnomachi.viewmodel

import hr.foi.air.honnomachi.model.UserModel

sealed interface ProfileUiState {
    data object Loading : ProfileUiState
    data class Success(val user: UserModel) : ProfileUiState
    data class Error(val message: String) : ProfileUiState
}
