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

/**
 * ViewModel za ekran s detaljima korisnika.
 *
 * Dohvaća podatke o korisniku prema [userId] iz navigacijskog argumenta
 * i izlaže ih putem [uiState] StateFlow-a.
 *
 * @param savedStateHandle Sadrži navigacijske argumente (userId).
 * @param adminRepository Repozitorij za administratorske operacije.
 */
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

        /**
         * Dohvaća podatke o korisniku iz repozitorija.
         *
         * Postavlja stanje na [AdminUserDetailUiState.Loading] tijekom dohvata,
         * zatim na [AdminUserDetailUiState.Success] ili [AdminUserDetailUiState.Error].
         */
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
