package hr.foi.air.honnomachi.ui.admin

import hr.foi.air.honnomachi.model.UserModel

enum class UserFilter {
    ALL,
    ACTIVE,
    SUSPENDED,
}

data class AdminUserListUiState(
    val isLoading: Boolean = true,
    val users: List<UserModel> = emptyList(),
    val errorMessage: String? = null,
    val isLoadingMore: Boolean = false,
    val hasMorePages: Boolean = true,
    val isRefreshing: Boolean = false,
    val searchQuery: String = "",
    val selectedFilter: UserFilter = UserFilter.ALL,
    val scrollToTopTrigger: Int = 0,
)
