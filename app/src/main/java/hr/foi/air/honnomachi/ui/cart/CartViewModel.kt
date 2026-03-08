package hr.foi.air.honnomachi.ui.cart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stripe.android.paymentsheet.PaymentSheetResult
import dagger.hilt.android.lifecycle.HiltViewModel
import hr.foi.air.honnomachi.data.CartRepository
import hr.foi.air.honnomachi.data.CheckoutPaymentIntentModel
import hr.foi.air.honnomachi.data.CheckoutRepository
import hr.foi.air.honnomachi.model.BookModel
import hr.foi.air.honnomachi.model.Currency
import hr.foi.air.honnomachi.util.Result
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val USD_TO_EUR_RATE = 1.18
private const val DEFAULT_RESERVATION_TTL_MINUTES = 15

enum class CheckoutNavigationTarget {
    SUCCESS,
    CANCELED,
}

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

        private val _addingBookId = MutableStateFlow<String?>(null)
        val addingBookId: StateFlow<String?> = _addingBookId.asStateFlow()

        private val _pendingCheckout = MutableStateFlow<CheckoutPaymentIntentModel?>(null)
        val pendingCheckout: StateFlow<CheckoutPaymentIntentModel?> = _pendingCheckout.asStateFlow()

        private val _paymentSheetRequests =
            MutableSharedFlow<CheckoutPaymentIntentModel>(extraBufferCapacity = 1)
        val paymentSheetRequests: SharedFlow<CheckoutPaymentIntentModel> =
            _paymentSheetRequests.asSharedFlow()

        private val _checkoutNavigation =
            MutableSharedFlow<CheckoutNavigationTarget>(extraBufferCapacity = 1)
        val checkoutNavigation: SharedFlow<CheckoutNavigationTarget> =
            _checkoutNavigation.asSharedFlow()

        private var activeCheckoutId: String? = null

        private val _expiredCartItemIds = mutableSetOf<String>()

        private val _secondsUntilExpiry = MutableStateFlow<Long?>(null)
        val secondsUntilExpiry: StateFlow<Long?> = _secondsUntilExpiry.asStateFlow()

        init {
            loadCartItems()
            startExpiryTicker()
        }

        private fun startExpiryTicker() {
            viewModelScope.launch {
                while (isActive) {
                    val state = _uiState.value
                    if (state is CartUiState.Success) {
                        val nowMs = System.currentTimeMillis()

                        val newlyExpired =
                            state.items.filter { item ->
                                val expiresAtMs = item.reservationExpiresAt?.toDate()?.time
                                expiresAtMs != null && expiresAtMs <= nowMs && item.id !in _expiredCartItemIds
                            }

                        for (item in newlyExpired) {
                            _expiredCartItemIds.add(item.id)
                            viewModelScope.launch {
                                cartRepository.removeFromCart(item.id)
                            }
                        }

                        if (newlyExpired.isNotEmpty()) {
                            _actionMessage.value = "Rezervacija je istekla. Knjige su uklonjene iz košarice."
                        }

                        val nearestExpiry =
                            state.items
                                .filter { it.id !in _expiredCartItemIds }
                                .mapNotNull { it.reservationExpiresAt }
                                .minOfOrNull { it.toDate().time }
                        _secondsUntilExpiry.value =
                            nearestExpiry?.let {
                                val remainingMs = it - nowMs
                                if (remainingMs > 0) remainingMs / 1000L else 0L
                            }
                    } else {
                        _secondsUntilExpiry.value = null
                    }
                    delay(1_000L)
                }
            }
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
            val bookId = book.bookId
            if (bookId.isNullOrBlank()) {
                _actionMessage.value = "Greška: Nevažeći ID knjige."
                return
            }
            if (_addingBookId.value != null) {
                return
            }

            viewModelScope.launch {
                _addingBookId.value = bookId
                try {
                    when (val result = cartRepository.addToCart(book)) {
                        is Result.Success -> {
                            _actionMessage.value = "Knjiga dodana u košaricu!"
                        }
                        is Result.Error -> {
                            _actionMessage.value = "Greška: ${result.exception.message}"
                        }
                    }
                } finally {
                    if (_addingBookId.value == bookId) {
                        _addingBookId.value = null
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
                        checkoutRepository.createCheckoutPaymentIntent(
                            reservationTtlMinutes = DEFAULT_RESERVATION_TTL_MINUTES,
                        )
                ) {
                    is Result.Success -> {
                        val checkout = result.data
                        activeCheckoutId = checkout.checkoutId
                        _pendingCheckout.value = checkout
                    }
                    is Result.Error -> {
                        val message = result.exception.message ?: "Nepoznata greška."
                        _actionMessage.value = "Plaćanje nije pokrenuto: $message"
                    }
                }
                _isCheckoutInProgress.value = false
            }
        }

        fun confirmCheckoutPayment() {
            val checkout = _pendingCheckout.value ?: return
            _pendingCheckout.value = null

            if (checkout.requiresPaymentSheet) {
                if (checkout.clientSecret.isNullOrBlank()) {
                    _actionMessage.value = "Stripe client secret nedostaje."
                    releaseActiveCheckout()
                    return
                }

                _paymentSheetRequests.tryEmit(checkout)
                _actionMessage.value =
                    if (checkout.walletContributionMinor > 0) {
                        "Wallet je djelomično iskorišten. Otvaranje Stripe naplate..."
                    } else {
                        "Otvaranje Stripe naplate..."
                    }
                return
            }

            activeCheckoutId = null
            _actionMessage.value =
                if (checkout.walletContributionMinor > 0) {
                    "Plaćanje uspješno putem walleta."
                } else {
                    "Plaćanje uspješno."
                }
            _checkoutNavigation.tryEmit(CheckoutNavigationTarget.SUCCESS)
        }

        fun dismissCheckoutDialog() {
            val checkout = _pendingCheckout.value ?: return
            _pendingCheckout.value = null

            if (checkout.requiresPaymentSheet) {
                releaseActiveCheckout()
            }
        }

        fun onPaymentSheetResult(result: PaymentSheetResult) {
            when (result) {
                PaymentSheetResult.Completed -> {
                    activeCheckoutId = null
                    _actionMessage.value = "Plaćanje zaprimljeno."
                    _checkoutNavigation.tryEmit(CheckoutNavigationTarget.SUCCESS)
                }
                PaymentSheetResult.Canceled -> {
                    _actionMessage.value = "Plaćanje je otkazano."
                    _checkoutNavigation.tryEmit(CheckoutNavigationTarget.CANCELED)
                    releaseActiveCheckout()
                }
                is PaymentSheetResult.Failed -> {
                    val error = result.error.localizedMessage ?: "Nepoznata greška."
                    _actionMessage.value = "Plaćanje nije uspjelo: $error"
                    releaseActiveCheckout()
                }
            }
        }

        private fun releaseActiveCheckout() {
            val checkoutId = activeCheckoutId ?: return
            activeCheckoutId = null
            viewModelScope.launch {
                checkoutRepository.cancelCheckout(checkoutId)
            }
        }

        fun onPaymentSheetPresentationFailed(throwable: Throwable) {
            val error = throwable.localizedMessage ?: "Nepoznata greška."
            _actionMessage.value = "Prikaz Stripe naplate nije uspio: $error"
            releaseActiveCheckout()
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
