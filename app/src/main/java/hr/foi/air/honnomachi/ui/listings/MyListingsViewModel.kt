package hr.foi.air.honnomachi.ui.listings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import hr.foi.air.honnomachi.data.BookRepository
import hr.foi.air.honnomachi.model.ItemStatus
import hr.foi.air.honnomachi.util.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MyListingsViewModel
    @Inject
    constructor(
        private val bookRepository: BookRepository,
        private val auth: FirebaseAuth,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(MyListingsUiState(isLoading = true))
        val uiState: StateFlow<MyListingsUiState> = _uiState.asStateFlow()

        init {
            loadListings()
        }

        private fun loadListings() {
            val userId = auth.currentUser?.uid ?: return
            viewModelScope.launch {
                bookRepository.getMyListings(userId).collect { result ->
                    when (result) {
                        is Result.Success -> {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    listings = result.data,
                                    errorMessage = null,
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
            }
        }

        fun onFilterChange(filter: ListingsFilter) {
            _uiState.update { it.copy(selectedFilter = filter) }
        }

        fun onToggleStatus(
            bookId: String,
            currentStatus: ItemStatus,
        ) {
            val newStatus =
                if (currentStatus == ItemStatus.INACTIVE) ItemStatus.AVAILABLE else ItemStatus.INACTIVE
            viewModelScope.launch {
                when (val result = bookRepository.updateListingStatus(bookId, newStatus)) {
                    is Result.Success -> {
                        _uiState.update { it.copy(snackbarMessage = null) }
                    }
                    is Result.Error -> {
                        _uiState.update {
                            it.copy(snackbarMessage = result.exception.message)
                        }
                    }
                }
            }
        }

        fun onRequestDelete(book: hr.foi.air.honnomachi.model.BookModel) {
            _uiState.update { it.copy(bookToDelete = book) }
        }

        fun onDismissDelete() {
            _uiState.update { it.copy(bookToDelete = null) }
        }

        fun onConfirmDelete() {
            val book = _uiState.value.bookToDelete ?: return
            val bookId = book.bookId ?: return
            _uiState.update { it.copy(bookToDelete = null) }
            viewModelScope.launch {
                when (val result = bookRepository.deleteListing(bookId)) {
                    is Result.Success -> {
                        _uiState.update { it.copy(snackbarMessage = null) }
                    }
                    is Result.Error -> {
                        _uiState.update {
                            it.copy(snackbarMessage = result.exception.message)
                        }
                    }
                }
            }
        }

        fun onSnackbarDismissed() {
            _uiState.update { it.copy(snackbarMessage = null) }
        }
    }
