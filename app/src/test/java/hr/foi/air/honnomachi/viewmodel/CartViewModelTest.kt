package hr.foi.air.honnomachi.viewmodel

import hr.foi.air.honnomachi.data.CartRepository
import hr.foi.air.honnomachi.data.CheckoutRepository
import hr.foi.air.honnomachi.data.CheckoutSessionModel
import hr.foi.air.honnomachi.model.BookModel
import hr.foi.air.honnomachi.model.CartItemModel
import hr.foi.air.honnomachi.ui.cart.CartUiState
import hr.foi.air.honnomachi.ui.cart.CartViewModel
import hr.foi.air.honnomachi.util.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
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
}

class FakeCheckoutRepository : CheckoutRepository {
    var createSessionCallCount = 0
    var resultToReturn: Result<CheckoutSessionModel> =
        Result.Success(
            CheckoutSessionModel(
                sessionId = "cs_test",
                checkoutUrl = "https://stripe.test/checkout",
                expiresAt = null,
                reservationIds = emptyList(),
            ),
        )

    override suspend fun createCheckoutSession(
        successUrl: String,
        cancelUrl: String,
    ): Result<CheckoutSessionModel> {
        createSessionCallCount++
        return resultToReturn
    }
}

@ExperimentalCoroutinesApi
class CartViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeCartRepository
    private lateinit var fakeCheckoutRepository: FakeCheckoutRepository
    private lateinit var viewModel: CartViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeCartRepository()
        fakeCheckoutRepository = FakeCheckoutRepository()
        viewModel = CartViewModel(fakeRepository, fakeCheckoutRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadCartItems loads items successfully`() =
        runTest(testDispatcher) {
            val book = BookModel(bookId = "1", title = "Test Book", price = 10.0)
            fakeRepository.addToCart(book)

            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state is CartUiState.Success)
            val successState = state as CartUiState.Success
            assertEquals(1, successState.items.size)
            assertEquals("Test Book", successState.items[0].title)
            assertEquals(10.0, successState.totalPrice, 0.01)
        }

    @Test
    fun `addToCart updates action message`() =
        runTest(testDispatcher) {
            val book = BookModel(bookId = "2", title = "New Book")
            viewModel.addToCart(book)
            advanceUntilIdle()

            val message = viewModel.actionMessage.value
            assertEquals("Knjiga dodana u košaricu!", message)

            val state = viewModel.uiState.value
            assertTrue(state is CartUiState.Success)
            assertEquals(1, (state as CartUiState.Success).items.size)
        }

    @Test
    fun `removeFromCart updates list and message`() =
        runTest(testDispatcher) {
            val book = BookModel(bookId = "3", title = "Delete Me")
            fakeRepository.addToCart(book)
            advanceUntilIdle()

            viewModel.removeFromCart("3")
            advanceUntilIdle()

            val message = viewModel.actionMessage.value
            assertEquals("Knjiga uklonjena iz košarice.", message)

            val state = viewModel.uiState.value
            assertTrue(state is CartUiState.Success)
            assertEquals(0, (state as CartUiState.Success).items.size)
        }

    @Test
    fun `checkoutWithStripe empty cart does not call backend`() =
        runTest(testDispatcher) {
            viewModel.checkoutWithStripe()
            advanceUntilIdle()

            assertEquals(0, fakeCheckoutRepository.createSessionCallCount)
            assertEquals("Košarica je prazna.", viewModel.actionMessage.value)
        }

    @Test
    fun `checkoutWithStripe emits checkout url on success`() =
        runTest(testDispatcher) {
            val emittedUrls = mutableListOf<String>()
            val collectionJob =
                backgroundScope.launch {
                    viewModel.checkoutUrl.collect { emittedUrls += it }
                }

            val book = BookModel(bookId = "4", title = "Checkout Book", price = 12.0)
            fakeRepository.addToCart(book)
            advanceUntilIdle()

            viewModel.checkoutWithStripe()
            advanceUntilIdle()

            assertEquals(1, fakeCheckoutRepository.createSessionCallCount)
            assertEquals(listOf("https://stripe.test/checkout"), emittedUrls)
            assertEquals(false, viewModel.isCheckoutInProgress.value)

            collectionJob.cancel()
        }
}
