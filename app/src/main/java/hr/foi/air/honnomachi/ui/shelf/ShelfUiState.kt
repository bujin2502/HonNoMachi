package hr.foi.air.honnomachi.ui.shelf

import hr.foi.air.honnomachi.model.BookModel

data class ShelfUiState(
    val isLoading: Boolean = false,
    val books: List<BookModel> = emptyList(),
    val errorMessage: String? = null,
    val selectedTab: ShelfTab = ShelfTab.PURCHASED
)
