package hr.foi.air.honnomachi.viewmodel

import hr.foi.air.honnomachi.data.CartRepository
import hr.foi.air.honnomachi.data.CheckoutPaymentIntentModel
import hr.foi.air.honnomachi.data.CheckoutRepository
import hr.foi.air.honnomachi.model.BookModel
import hr.foi.air.honnomachi.model.CartItemModel
import hr.foi.air.honnomachi.model.Currency
import hr.foi.air.honnomachi.ui.cart.CartUiState
import hr.foi.air.honnomachi.ui.cart.CartViewModel
import hr.foi.air.honnomachi.util.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Ignore
import org.junit.Test

class FakeCartRepository : CartRepository {
    private val _cartItems = MutableStateFlow<List<CartItemModel>>(emptyList())

    override suspend fun addToCart(book: BookModel): Result<Unit> {
        val newItem =
            CartItemModel(
                id = book.bookId ?: "testId",
                bookId = book.bookId ?: "testId",
                title = book.title,
                price = book.price,
                currency = book.priceCurrency.name,
            )
        _cartItems.value = _cartItems.value + newItem
        return Result.Success(Unit)
    }

    override fun getCartItems(): Flow<Result<List<CartItemModel>>> =
        _cartItems.map {
            Result.Success(it)
        }

    override suspend fun removeFromCart(cartItemId: String): Result<Unit> {
        _cartItems.value = _cartItems.value.filter { it.id != cartItemId }
        return Result.Success(Unit)
    }

    fun clearCart() {
        _cartItems.value = emptyList()
    }

    fun setItems(items: List<CartItemModel>) {
        _cartItems.value = items
    }
}

class FakeCheckoutRepository : CheckoutRepository {
    var createPaymentIntentCallCount = 0
    var resultToReturn: Result<CheckoutPaymentIntentModel> =
        Result.Success(
            CheckoutPaymentIntentModel(
                checkoutId = "pi_test",
                paymentIntentId = "pi_test",
                clientSecret = "pi_test_secret_123",
                amountMinor = 1200,
                totalAmountMinor = 1200,
                walletContributionMinor = 0,
                currency = "eur",
                expiresAt = null,
                reservationIds = emptyList(),
                requiresPaymentSheet = true,
                checkoutCompleted = false,
            ),
        )

    override suspend fun createCheckoutPaymentIntent(reservationTtlMinutes: Int?): Result<CheckoutPaymentIntentModel> {
        if (createPaymentIntentDelayMs > 0) delay(createPaymentIntentDelayMs)
        createPaymentIntentCallCount++
        return resultToReturn
    }

    var createPaymentIntentDelayMs: Long = 0

    var cancelCheckoutResult: Result<Unit> = Result.Success(Unit)

    override suspend fun cancelCheckout(checkoutId: String): Result<Unit> = cancelCheckoutResult
}

@Ignore("Ticker coroutine causes infinite loop with runTest – needs refactor")
@ExperimentalCoroutinesApi
class CartViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeCartRepository: FakeCartRepository
    private lateinit var fakeCheckoutRepository: FakeCheckoutRepository
    private lateinit var viewModel: CartViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeCartRepository = FakeCartRepository()
        fakeCheckoutRepository = FakeCheckoutRepository()
        viewModel = CartViewModel(fakeCartRepository, fakeCheckoutRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadCartItems loads items and calculates total price`() =
        runTest(testDispatcher) {
            val book = BookModel(bookId = "1", title = "Test Book", price = 10.0, priceCurrency = Currency.EUR)
            fakeCartRepository.addToCart(book)

            advanceTimeBy(2_000)
            runCurrent()

            val state = viewModel.uiState.value
            assertTrue(state is CartUiState.Success)
            val successState = state as CartUiState.Success
            assertEquals(1, successState.items.size)
            assertEquals("Test Book", successState.items[0].title)
            assertEquals(10.0, successState.totalPrice, 0.01)
        }

    @Test
    fun `loadCartItems calculates total price correctly with mixed currencies`() =
        runTest(testDispatcher) {
            val bookEur = BookModel(bookId = "1", title = "EUR Book", price = 20.0, priceCurrency = Currency.EUR)
            val bookUsd = BookModel(bookId = "2", title = "USD Book", price = 23.6, priceCurrency = Currency.USD)
            fakeCartRepository.addToCart(bookEur)
            fakeCartRepository.addToCart(bookUsd)

            advanceTimeBy(2_000)
            runCurrent()

            val state = viewModel.uiState.value
            assertTrue(state is CartUiState.Success)
            val successState = state as CartUiState.Success
            assertEquals(2, successState.items.size)
            assertEquals(40.0, successState.totalPrice, 0.01)
        }

    @Test
    fun `checkoutWithStripe empty cart does not call backend`() =
        runTest(testDispatcher) {
            viewModel.checkoutWithStripe()
            advanceTimeBy(2_000)
            runCurrent()

            assertEquals(0, fakeCheckoutRepository.createPaymentIntentCallCount)
            assertEquals("Košarica je prazna.", viewModel.actionMessage.value)
        }

    @Test
    fun `checkoutWithStripe emits payment sheet request on success`() =
        runTest(testDispatcher) {
            val emittedRequests = mutableListOf<CheckoutPaymentIntentModel>()
            val collectionJob =
                backgroundScope.launch {
                    viewModel.paymentSheetRequests.collect { emittedRequests += it }
                }

            val book = BookModel(bookId = "4", title = "Checkout Book", price = 12.0, priceCurrency = Currency.EUR)
            fakeCartRepository.addToCart(book)
            advanceTimeBy(2_000)
            runCurrent()

            viewModel.checkoutWithStripe()
            advanceTimeBy(2_000)
            runCurrent()

            assertEquals(1, fakeCheckoutRepository.createPaymentIntentCallCount)
            assertEquals(1, emittedRequests.size)
            assertEquals("pi_test_secret_123", emittedRequests[0].clientSecret)
            assertEquals(false, viewModel.isCheckoutInProgress.value)

            collectionJob.cancel()
        }

    @Test
    fun `checkoutWithStripe wallet only navigates to success without payment sheet`() =
        runTest(testDispatcher) {
            fakeCheckoutRepository.resultToReturn =
                Result.Success(
                    CheckoutPaymentIntentModel(
                        checkoutId = "wallet_checkout",
                        paymentIntentId = null,
                        clientSecret = null,
                        amountMinor = 0,
                        totalAmountMinor = 1200,
                        walletContributionMinor = 1200,
                        currency = "eur",
                        expiresAt = null,
                        reservationIds = emptyList(),
                        requiresPaymentSheet = false,
                        checkoutCompleted = true,
                    ),
                )

            val emittedRequests = mutableListOf<CheckoutPaymentIntentModel>()
            val emittedNavigation = mutableListOf<hr.foi.air.honnomachi.ui.cart.CheckoutNavigationTarget>()

            val requestCollectionJob =
                backgroundScope.launch {
                    viewModel.paymentSheetRequests.collect { emittedRequests += it }
                }
            val navigationCollectionJob =
                backgroundScope.launch {
                    viewModel.checkoutNavigation.collect { emittedNavigation += it }
                }

            val book = BookModel(bookId = "4", title = "Checkout Book", price = 12.0, priceCurrency = Currency.EUR)
            fakeCartRepository.addToCart(book)
            advanceTimeBy(2_000)
            runCurrent()

            viewModel.checkoutWithStripe()
            advanceTimeBy(2_000)
            runCurrent()

            assertEquals(0, emittedRequests.size)
            assertEquals(1, emittedNavigation.size)
            assertEquals(hr.foi.air.honnomachi.ui.cart.CheckoutNavigationTarget.SUCCESS, emittedNavigation[0])

            requestCollectionJob.cancel()
            navigationCollectionJob.cancel()
        }

    @Test
    fun `addToCart updates action message`() =
        runTest(testDispatcher) {
            val book = BookModel(bookId = "2", title = "New Book")
            viewModel.addToCart(book)
            advanceTimeBy(2_000)
            runCurrent()

            val message = viewModel.actionMessage.value
            assertEquals("Knjiga dodana u košaricu!", message)
        }

    @Test
    fun `removeFromCart updates list and message`() =
        runTest(testDispatcher) {
            val book = BookModel(bookId = "3", title = "Delete Me")
            fakeCartRepository.addToCart(book)
            advanceTimeBy(2_000)
            runCurrent()

            viewModel.removeFromCart("3")
            advanceTimeBy(2_000)
            runCurrent()

            val message = viewModel.actionMessage.value
            assertEquals("Knjiga uklonjena iz košarice.", message)

            val state = viewModel.uiState.value
            assertTrue(state is CartUiState.Success)
            assertEquals(0, (state as CartUiState.Success).items.size)
        }
}
