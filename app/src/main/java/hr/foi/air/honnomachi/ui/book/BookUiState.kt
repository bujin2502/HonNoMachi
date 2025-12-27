package hr.foi.air.honnomachi.ui.book

import hr.foi.air.honnomachi.model.BookModel

sealed class BookUiState {
    data object Loading : BookUiState()

    data class Success(
        val book: BookModel,
    ) : BookUiState()

    data class Error(
        val message: String,
    ) : BookUiState()

    data object BookNotFound : BookUiState()
}
