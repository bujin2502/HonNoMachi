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

        /** Otvara dijalog za suspenziju korisnika. */
        fun onSuspendClick() {
            updateSuccessState { it.copy(showSuspendDialog = true) }
        }

        /** Otvara dijalog za reaktivaciju korisnika. */
        fun onReactivateClick() {
            updateSuccessState { it.copy(showReactivateDialog = true) }
        }

        /** Zatvara otvoreni dijalog. */
        fun dismissDialog() {
            updateSuccessState { it.copy(showSuspendDialog = false, showReactivateDialog = false) }
        }

        /**
         * Izvršava suspenziju korisnika.
         *
         * @param reason Razlog suspenzije.
         * @param successMessage Poruka za prikaz nakon uspješne suspenzije.
         * @param errorMessage Poruka za prikaz nakon neuspješne suspenzije.
         */
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

        /**
         * Izvršava reaktivaciju korisnika.
         *
         * @param successMessage Poruka za prikaz nakon uspješne reaktivacije.
         * @param errorMessage Poruka za prikaz nakon neuspješne reaktivacije.
         */
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

        /** Briše prikazanu poruku Snackbar-a. */
        fun consumeActionMessage() {
            updateSuccessState { it.copy(actionMessage = null) }
        }

        /**
         * Ponovno dohvaća korisnika nakon uspješne akcije i postavlja poruku.
         *
         * @param message Poruka za prikaz u Snackbar-u.
         */
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

        /**
         * Ažurira [AdminUserDetailUiState.Success] stanje ako je trenutno aktivno.
         *
         * @param transform Transformacija nad trenutnim Success stanjem.
         */
        private fun updateSuccessState(transform: (AdminUserDetailUiState.Success) -> AdminUserDetailUiState.Success) {
            val current = _uiState.value
            if (current is AdminUserDetailUiState.Success) {
                _uiState.value = transform(current)
            }
        }
    }
