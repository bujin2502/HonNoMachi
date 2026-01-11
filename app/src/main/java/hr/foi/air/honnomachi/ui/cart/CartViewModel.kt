package hr.foi.air.honnomachi.ui.cart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import hr.foi.air.honnomachi.data.CartRepository
import hr.foi.air.honnomachi.model.BookModel
import hr.foi.air.honnomachi.util.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CartViewModel
    @Inject
    constructor(
        private val cartRepository: CartRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow<CartUiState>(CartUiState.Loading)
        val uiState: StateFlow<CartUiState> = _uiState.asStateFlow()

        private val _actionMessage = MutableStateFlow<String?>(null)
        val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

        init {
            loadCartItems()
        }

        private fun loadCartItems() {
            viewModelScope.launch {
                cartRepository.getCartItems().collect { result ->
                    when (result) {
                        is Result.Success -> {
                            val items = result.data
                            val total = items.sumOf { it.price }
                            _uiState.value = CartUiState.Success(items, total)
                        }
                        is Result.Error -> {
                            _uiState.value = CartUiState.Error(result.exception.message ?: "Failed to load cart")
                        }
                    }
                }
            }
        }

        fun addToCart(book: BookModel) {
            viewModelScope.launch {
                when (val result = cartRepository.addToCart(book)) {
                    is Result.Success -> {
                        _actionMessage.value = "Knjiga dodana u košaricu!"
                    }
                    is Result.Error -> {
                        _actionMessage.value = "Greška: ${result.exception.message}"
                    }
                }
            }
        }

        fun removeFromCart(cartItemId: String) {
            viewModelScope.launch {
                when (val result = cartRepository.removeFromCart(cartItemId)) {
                    is Result.Success -> {
                        _actionMessage.value = "Knjiga uklonjena iz košarice."
                    }
                    is Result.Error -> {
                        _actionMessage.value = "Greška prilikom brisanja: ${result.exception.message}"
                    }
                }
            }
        }

        fun consumeMessage() {
            _actionMessage.value = null
        }
    }
