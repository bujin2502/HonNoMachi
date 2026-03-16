package hr.foi.air.honnomachi.ui.listings

import hr.foi.air.honnomachi.model.BookModel

data class MyListingsUiState(
    val isLoading: Boolean = false,
    val listings: List<BookModel> = emptyList(),
    val errorMessage: String? = null,
    val selectedFilter: ListingsFilter = ListingsFilter.ALL,
    val snackbarMessage: String? = null,
    val bookToDelete: BookModel? = null,
)

enum class ListingsFilter {
    ALL,
    ACTIVE,
    INACTIVE,
}
