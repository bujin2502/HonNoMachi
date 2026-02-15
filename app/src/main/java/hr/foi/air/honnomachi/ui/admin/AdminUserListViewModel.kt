package hr.foi.air.honnomachi.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.DocumentSnapshot
import dagger.hilt.android.lifecycle.HiltViewModel
import hr.foi.air.honnomachi.data.AdminRepository
import hr.foi.air.honnomachi.util.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel za ekran s listom korisnika.
 *
 * Upravlja dohvatom korisnika s cursor-based straničenjem,
 * osvježavanjem liste i učitavanjem sljedećih stranica.
 *
 * @param adminRepository Repozitorij za administratorske operacije.
 */
@HiltViewModel
class AdminUserListViewModel
    @Inject
    constructor(
        private val adminRepository: AdminRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(AdminUserListUiState())
        val uiState: StateFlow<AdminUserListUiState> = _uiState.asStateFlow()

        private var lastDocSnapshot: DocumentSnapshot? = null

        companion object {
            private const val PAGE_SIZE = 20
        }

        init {
            loadUsers()
        }

        /**
         * Dohvaća prvu stranicu korisnika.
         *
         * Resetira listu i straničenje te prikazuje loading stanje.
         */
        fun loadUsers() {
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
                lastDocSnapshot = null

                when (val result = adminRepository.getAllUsers(pageSize = PAGE_SIZE)) {
                    is Result.Success -> {
                        val page = result.data
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                users = page.users,
                                hasMorePages = page.users.size == PAGE_SIZE,
                            )
                        }
                        lastDocSnapshot = page.lastDocSnapshot
                    }
                    is Result.Error -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = result.exception.message,
                            )
                        }
                    }
                }
            }
        }

        /**
         * Učitava sljedeću stranicu korisnika i dodaje ih na postojeću listu.
         *
         * Ignorira poziv ako je učitavanje već u tijeku ili nema više stranica.
         */
        fun loadMoreUsers() {
            val currentState = _uiState.value
            if (currentState.isLoadingMore || !currentState.hasMorePages) return

            viewModelScope.launch {
                _uiState.update { it.copy(isLoadingMore = true) }

                when (val result = adminRepository.getAllUsers(PAGE_SIZE, lastDocSnapshot)) {
                    is Result.Success -> {
                        val page = result.data
                        _uiState.update {
                            it.copy(
                                isLoadingMore = false,
                                users = it.users + page.users,
                                hasMorePages = page.users.size == PAGE_SIZE,
                            )
                        }
                        lastDocSnapshot = page.lastDocSnapshot
                    }
                    is Result.Error -> {
                        _uiState.update { it.copy(isLoadingMore = false) }
                    }
                }
            }
        }

        /**
         * Osvježava listu korisnika (pull-to-refresh).
         *
         * Resetira straničenje i dohvaća prvu stranicu iznova.
         */
        fun refreshUsers() {
            viewModelScope.launch {
                _uiState.update { it.copy(isRefreshing = true, errorMessage = null) }
                lastDocSnapshot = null

                when (val result = adminRepository.getAllUsers(pageSize = PAGE_SIZE)) {
                    is Result.Success -> {
                        val page = result.data
                        _uiState.update {
                            it.copy(
                                isRefreshing = false,
                                users = page.users,
                                hasMorePages = page.users.size == PAGE_SIZE,
                            )
                        }
                        lastDocSnapshot = page.lastDocSnapshot
                    }
                    is Result.Error -> {
                        _uiState.update {
                            it.copy(
                                isRefreshing = false,
                                errorMessage = result.exception.message,
                            )
                        }
                    }
                }
            }
        }
    }
