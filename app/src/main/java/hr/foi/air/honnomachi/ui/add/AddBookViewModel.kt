package hr.foi.air.honnomachi.ui.add

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import hr.foi.air.honnomachi.data.BookRepository
import hr.foi.air.honnomachi.model.BookModel
import hr.foi.air.honnomachi.util.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddBookViewModel
    @Inject
    constructor(
        private val bookRepository: BookRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow<AddBookUiState>(AddBookUiState.Idle)
        val uiState: StateFlow<AddBookUiState> = _uiState.asStateFlow()

        fun createListing(book: BookModel) {
            if (_uiState.value == AddBookUiState.Submitting) {
                return
            }

            _uiState.value = AddBookUiState.Submitting
            viewModelScope.launch {
                when (val result = bookRepository.addBook(book)) {
                    is Result.Success -> {
                        _uiState.value = AddBookUiState.Success
                    }
                    is Result.Error -> {
                        _uiState.value =
                            AddBookUiState.Error(result.exception.message ?: "Greska prilikom spremanja ponude.")
                    }
                }
            }
        }

        fun resetState() {
            _uiState.value = AddBookUiState.Idle
        }
    }
