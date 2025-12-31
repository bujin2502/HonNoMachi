package hr.foi.air.honnomachi.ui.book

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import hr.foi.air.honnomachi.data.BookRepository
import hr.foi.air.honnomachi.util.Result
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

            when (val result = bookRepository.getBookDetails(bookId)) {
                is Result.Success -> {
                    val book = result.data
                    if (book != null) {
                        _uiState.value = BookUiState.Success(book)
                    } else {
                        _uiState.value = BookUiState.BookNotFound
                    }
                }

                is Result.Error -> {
                    _uiState.value = BookUiState.Error(result.exception.message ?: "Unknown error")
                }
            }
        }
    }
}
