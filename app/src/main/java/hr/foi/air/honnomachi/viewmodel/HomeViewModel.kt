package hr.foi.air.honnomachi.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import hr.foi.air.honnomachi.pages.BookListState
import hr.foi.air.honnomachi.repository.BookRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel(
    private val bookRepository: BookRepository,
) : ViewModel() {
    private val _bookListState = MutableStateFlow<BookListState>(BookListState.Loading)
    val bookListState = _bookListState.asStateFlow()

    var searchQuery = mutableStateOf("")
        private set

    init {
        getBooks()
    }

    private fun getBooks() {
        viewModelScope.launch {
            bookRepository.getBooks().collect {
                _bookListState.value = it
            }
        }
    }

    fun onSearchQueryChange(newQuery: String) {
        searchQuery.value = newQuery
    }
}
