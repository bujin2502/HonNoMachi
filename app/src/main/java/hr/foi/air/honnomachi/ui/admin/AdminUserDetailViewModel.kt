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
class AdminUserDetailViewModel
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

        fun loadUser() {
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
    }
