package hr.foi.air.honnomachi.ui.shelf

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import hr.foi.air.honnomachi.data.BookRepository
import hr.foi.air.honnomachi.util.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ShelfViewModel @Inject constructor(
    private val bookRepository: BookRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow(ShelfUiState())
    val uiState: StateFlow<ShelfUiState> = _uiState.asStateFlow()

    init {
        loadBooks()
    }

    fun selectTab(tab: ShelfTab) {
        _uiState.update { it.copy(selectedTab = tab) }
        loadBooks()
    }

    fun loadBooks() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            _uiState.update { it.copy(errorMessage = "Korisnik nije prijavljen.", isLoading = false) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val currentTab = _uiState.value.selectedTab

            if (currentTab == ShelfTab.SOLD) {
                bookRepository.getSoldBooks(currentUser.uid).collectLatest { result ->
                    when (result) {
                        is Result.Success -> {
                            _uiState.update { it.copy(books = result.data, isLoading = false, errorMessage = null) }
                        }
                        is Result.Error -> {
                            _uiState.update { it.copy(errorMessage = result.exception.message, isLoading = false) }
                        }
                    }
                }
            } else {
                // Za Purchased tab (ako još nemaš repo metodu)
                _uiState.update { it.copy(books = emptyList(), isLoading = false, errorMessage = null) }
            }
        }
    }
}