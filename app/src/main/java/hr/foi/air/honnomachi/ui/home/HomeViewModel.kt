package hr.foi.air.honnomachi.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import hr.foi.air.honnomachi.ui.home.BookListState
import hr.foi.air.honnomachi.data.BookRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

open class HomeViewModel(
    private val bookRepository: BookRepository,
) : ViewModel() {
    private val _bookListState = MutableStateFlow<BookListState>(BookListState.Loading)
    val bookListState = _bookListState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    init {
        getBooks()
    }

    open fun getBooks() {
        viewModelScope.launch {
            bookRepository.getBooks().collect {
                _bookListState.value = it
            }
        }
    }

    fun onSearchQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
    }

    protected fun setBookListState(state: BookListState) {
        _bookListState.value = state
    }
}
