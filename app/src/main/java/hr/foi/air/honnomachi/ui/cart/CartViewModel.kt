package hr.foi.air.honnomachi.ui.cart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import hr.foi.air.honnomachi.data.CartRepository
import hr.foi.air.honnomachi.data.CheckoutRepository
import hr.foi.air.honnomachi.model.BookModel
import hr.foi.air.honnomachi.model.Currency
import hr.foi.air.honnomachi.util.Result
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val USD_TO_EUR_RATE = 1.18

@HiltViewModel
class CartViewModel
    @Inject
    constructor(
        private val cartRepository: CartRepository,
        private val checkoutRepository: CheckoutRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow<CartUiState>(CartUiState.Loading)
        val uiState: StateFlow<CartUiState> = _uiState.asStateFlow()

        private val _actionMessage = MutableStateFlow<String?>(null)
        val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

        private val _isCheckoutInProgress = MutableStateFlow(false)
        val isCheckoutInProgress: StateFlow<Boolean> = _isCheckoutInProgress.asStateFlow()

        private val _checkoutUrl = MutableSharedFlow<String>(extraBufferCapacity = 1)
        val checkoutUrl: SharedFlow<String> = _checkoutUrl.asSharedFlow()

        init {
            loadCartItems()
        }

        private fun loadCartItems() {
            viewModelScope.launch {
                cartRepository.getCartItems().collect { result ->
                    when (result) {
                        is Result.Success -> {
                            val items = result.data
                            val total =
                                items.sumOf { item ->
                                    when (item.currency) {
                                        Currency.USD.name -> item.price / USD_TO_EUR_RATE
                                        else -> item.price
                                    }
                                }
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

        fun checkoutWithStripe() {
            val currentState = _uiState.value
            if (currentState !is CartUiState.Success || currentState.items.isEmpty()) {
                _actionMessage.value = "Košarica je prazna."
                return
            }

            if (_isCheckoutInProgress.value) {
                return
            }

            viewModelScope.launch {
                _isCheckoutInProgress.value = true
                when (
                    val result =
                        checkoutRepository.createCheckoutSession(
                            successUrl = CHECKOUT_SUCCESS_URL,
                            cancelUrl = CHECKOUT_CANCEL_URL,
                        )
                ) {
                    is Result.Success -> {
                        _checkoutUrl.tryEmit(result.data.checkoutUrl)
                        _actionMessage.value = "Preusmjeravanje na Stripe naplatu..."
                    }
                    is Result.Error -> {
                        val message = result.exception.message ?: "Nepoznata greška."
                        _actionMessage.value = "Plaćanje nije pokrenuto: $message"
                    }
                }
                _isCheckoutInProgress.value = false
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

        companion object {
            private const val CHECKOUT_SUCCESS_URL = "https://example.com/honnomachi/checkout/success"
            private const val CHECKOUT_CANCEL_URL = "https://example.com/honnomachi/checkout/cancel"
        }
    }
