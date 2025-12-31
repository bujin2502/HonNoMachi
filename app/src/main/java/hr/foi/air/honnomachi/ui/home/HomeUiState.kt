package hr.foi.air.honnomachi.ui.home

import hr.foi.air.honnomachi.model.BookModel

data class HomeUiState(
    val isLoading: Boolean = false,
    val books: List<BookModel> = emptyList(),
    val errorMessage: String? = null,
    val searchQuery: String = "",
)
