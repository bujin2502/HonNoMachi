package hr.foi.air.honnomachi.ui.admin

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import hr.foi.air.honnomachi.data.AdminRepository
import hr.foi.air.honnomachi.util.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
open class AdminUserDetailViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val adminRepository: AdminRepository,
    ) : ViewModel() {
        private val userId: String = checkNotNull(savedStateHandle["userId"])

        private val _uiState = MutableStateFlow<AdminUserDetailUiState>(AdminUserDetailUiState.Loading)
        val uiState: StateFlow<AdminUserDetailUiState> = _uiState.asStateFlow()

        init {
            loadUser()
        }

        open fun loadUser() {
            viewModelScope.launch {
                _uiState.value = AdminUserDetailUiState.Loading

                when (val result = adminRepository.getUserById(userId)) {
                    is Result.Success -> {
                        _uiState.value = AdminUserDetailUiState.Success(result.data)
                    }
                    is Result.Error -> {
                        _uiState.value =
                            AdminUserDetailUiState.Error(
                                result.exception.message ?: "Unknown error",
                            )
                    }
                }
            }
        }

        fun onSuspendClick() {
            updateSuccessState { it.copy(showSuspendDialog = true) }
        }

        fun onReactivateClick() {
            updateSuccessState { it.copy(showReactivateDialog = true) }
        }

        fun dismissDialog() {
            updateSuccessState { it.copy(showSuspendDialog = false, showReactivateDialog = false) }
        }

        fun confirmSuspend(
            reason: String,
            successMessage: String,
            errorMessage: String,
        ) {
            viewModelScope.launch {
                updateSuccessState { it.copy(isActionLoading = true, showSuspendDialog = false) }

                when (val result = adminRepository.suspendUser(userId, reason)) {
                    is Result.Success -> {
                        refreshUserAfterAction(successMessage)
                    }
                    is Result.Error -> {
                        updateSuccessState {
                            it.copy(
                                isActionLoading = false,
                                actionMessage = "$errorMessage ${result.exception.message}",
                            )
                        }
                    }
                }
            }
        }

        fun confirmReactivate(
            successMessage: String,
            errorMessage: String,
        ) {
            viewModelScope.launch {
                updateSuccessState { it.copy(isActionLoading = true, showReactivateDialog = false) }

                when (val result = adminRepository.reactivateUser(userId)) {
                    is Result.Success -> {
                        refreshUserAfterAction(successMessage)
                    }
                    is Result.Error -> {
                        updateSuccessState {
                            it.copy(
                                isActionLoading = false,
                                actionMessage = "$errorMessage ${result.exception.message}",
                            )
                        }
                    }
                }
            }
        }

        fun consumeActionMessage() {
            updateSuccessState { it.copy(actionMessage = null) }
        }

        private suspend fun refreshUserAfterAction(message: String) {
            when (val result = adminRepository.getUserById(userId)) {
                is Result.Success -> {
                    _uiState.value =
                        AdminUserDetailUiState.Success(
                            user = result.data,
                            actionMessage = message,
                        )
                }
                is Result.Error -> {
                    updateSuccessState {
                        it.copy(isActionLoading = false, actionMessage = message)
                    }
                }
            }
        }

        private fun updateSuccessState(transform: (AdminUserDetailUiState.Success) -> AdminUserDetailUiState.Success) {
            val current = _uiState.value
            if (current is AdminUserDetailUiState.Success) {
                _uiState.value = transform(current)
            }
        }
    }
