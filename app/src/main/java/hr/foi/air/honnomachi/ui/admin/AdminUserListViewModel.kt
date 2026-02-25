package hr.foi.air.honnomachi.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.DocumentSnapshot
import dagger.hilt.android.lifecycle.HiltViewModel
import hr.foi.air.honnomachi.data.AdminRepository
import hr.foi.air.honnomachi.util.Result
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
open class AdminUserListViewModel
    @Inject
    constructor(
        private val adminRepository: AdminRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(AdminUserListUiState())
        val uiState: StateFlow<AdminUserListUiState> = _uiState.asStateFlow()

        private var lastDocSnapshot: DocumentSnapshot? = null
        private val searchQueryFlow = MutableStateFlow("")

        companion object {
            private const val PAGE_SIZE = 20
            private const val DEBOUNCE_MS = 300L
        }

        init {
            loadUsers()
            viewModelScope.launch {
                searchQueryFlow
                    .debounce(DEBOUNCE_MS)
                    .collectLatest { applySearchAndFilter() }
            }
        }

        open fun loadUsers() {
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

        open fun loadMoreUsers() {
            val currentState = _uiState.value
            if (currentState.isLoading ||
                currentState.isRefreshing ||
                currentState.isLoadingMore ||
                !currentState.hasMorePages
            ) {
                return
            }
            if (isSearchOrFilterActive()) return

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

        fun refreshUsers() {
            viewModelScope.launch {
                _uiState.update { it.copy(isRefreshing = true, errorMessage = null) }
                lastDocSnapshot = null

                if (isSearchOrFilterActive()) {
                    executeSearchAndFilter()
                    _uiState.update { it.copy(isRefreshing = false) }
                } else {
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

        fun onSearchQueryChanged(query: String) {
            _uiState.update { it.copy(searchQuery = query) }
            searchQueryFlow.value = query
        }

        fun onFilterSelected(filter: UserFilter) {
            _uiState.update { it.copy(selectedFilter = filter) }
            viewModelScope.launch { applySearchAndFilter() }
        }

        private suspend fun applySearchAndFilter() {
            if (!isSearchOrFilterActive()) {
                loadUsers()
                return
            }

            _uiState.update { it.copy(isLoading = true, errorMessage = null, hasMorePages = false) }
            executeSearchAndFilter()
        }

        private suspend fun executeSearchAndFilter() {
            val query = _uiState.value.searchQuery.trim()
            val filter = _uiState.value.selectedFilter

            val result =
                if (query.isNotEmpty()) {
                    adminRepository.searchUsers(query)
                } else {
                    adminRepository.searchUsers("")
                }

            when (result) {
                is Result.Success -> {
                    val filtered = applyStatusFilter(result.data, filter)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            users = filtered,
                            hasMorePages = false,
                        )
                    }
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

        private fun applyStatusFilter(
            users: List<hr.foi.air.honnomachi.model.UserModel>,
            filter: UserFilter,
        ): List<hr.foi.air.honnomachi.model.UserModel> =
            when (filter) {
                UserFilter.ALL -> users
                UserFilter.ACTIVE -> users.filter { it.suspended != true }
                UserFilter.SUSPENDED -> users.filter { it.suspended == true }
            }

        private fun isSearchOrFilterActive(): Boolean {
            val state = _uiState.value
            return state.searchQuery.isNotBlank() || state.selectedFilter != UserFilter.ALL
        }
    }
