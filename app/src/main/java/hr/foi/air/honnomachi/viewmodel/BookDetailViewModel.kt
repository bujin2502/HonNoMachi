package hr.foi.air.honnomachi.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import hr.foi.air.honnomachi.repository.BookRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class BookDetailViewModel(
    private val bookRepository: BookRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow<BookUiState>(BookUiState.Loading)
    val uiState: StateFlow<BookUiState> = _uiState

    fun loadBookDetails(bookId: String?) {
        if (bookId.isNullOrBlank()) {
            _uiState.value = BookUiState.BookNotFound
            return
        }

        viewModelScope.launch {
            _uiState.value = BookUiState.Loading

            val book = bookRepository.getBookDetails(bookId)

            if (book != null) {
                _uiState.value = BookUiState.Success(book)
            } else {
                _uiState.value = BookUiState.BookNotFound
            }
        }
    }
}
