package hr.foi.air.honnomachi.viewmodel

import hr.foi.air.honnomachi.data.CartRepository
import hr.foi.air.honnomachi.data.OrderRepository
import hr.foi.air.honnomachi.model.BookModel
import hr.foi.air.honnomachi.model.CartItemModel
import hr.foi.air.honnomachi.model.Currency
import hr.foi.air.honnomachi.ui.cart.CartUiState
import hr.foi.air.honnomachi.ui.cart.CartViewModel
import hr.foi.air.honnomachi.util.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
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
                currency = book.priceCurrency.name
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
}

class FakeOrderRepository(
    private val fakeCartRepository: FakeCartRepository
) : OrderRepository {
    var placeOrderCalled = false
        private set

    override suspend fun placeOrder(): Result<Unit> {
        placeOrderCalled = true
        fakeCartRepository.clearCart()
        return Result.Success(Unit)
    }
}


@ExperimentalCoroutinesApi
class CartViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeCartRepository: FakeCartRepository
    private lateinit var fakeOrderRepository: FakeOrderRepository
    private lateinit var viewModel: CartViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeCartRepository = FakeCartRepository()
        fakeOrderRepository = FakeOrderRepository(fakeCartRepository)
        viewModel = CartViewModel(fakeCartRepository, fakeOrderRepository)
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

            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state is CartUiState.Success)
            val successState = state as CartUiState.Success
            assertEquals(1, successState.items.size)
            assertEquals("Test Book", successState.items[0].title)
            assertEquals(10.0, successState.totalPrice, 0.01)
        }

    @Test
    fun `loadCartItems calculates total price correctly with mixed currencies`() = runTest(testDispatcher) {
        val bookEur = BookModel(bookId = "1", title = "EUR Book", price = 20.0, priceCurrency = Currency.EUR)
        val bookUsd = BookModel(bookId = "2", title = "USD Book", price = 23.6, priceCurrency = Currency.USD)
        fakeCartRepository.addToCart(bookEur)
        fakeCartRepository.addToCart(bookUsd)

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is CartUiState.Success)
        val successState = state as CartUiState.Success
        assertEquals(2, successState.items.size)
        assertEquals(40.0, successState.totalPrice, 0.01)
    }

    @Test
    fun `placeOrder updates message and clears cart on success`() = runTest(testDispatcher) {
        val book = BookModel(bookId = "1", title = "Test Book", price = 10.0, priceCurrency = Currency.EUR)
        fakeCartRepository.addToCart(book)
        advanceUntilIdle()

        var state = viewModel.uiState.value
        assertTrue(state is CartUiState.Success)
        assertEquals(1, (state as CartUiState.Success).items.size)

        viewModel.placeOrder()
        advanceUntilIdle()

        assertTrue(fakeOrderRepository.placeOrderCalled)
        assertEquals("Narudžba uspješno kreirana!", viewModel.actionMessage.value)

        state = viewModel.uiState.value
        assertTrue(state is CartUiState.Success)
        assertEquals(0, (state as CartUiState.Success).items.size)
    }

    @Test
    fun `addToCart updates action message`() =
        runTest(testDispatcher) {
            val book = BookModel(bookId = "2", title = "New Book")
            viewModel.addToCart(book)
            advanceUntilIdle()

            val message = viewModel.actionMessage.value
            assertEquals("Knjiga dodana u košaricu!", message)
        }

    @Test
    fun `removeFromCart updates list and message`() =
        runTest(testDispatcher) {
            val book = BookModel(bookId = "3", title = "Delete Me")
            fakeCartRepository.addToCart(book)
            advanceUntilIdle()

            viewModel.removeFromCart("3")
            advanceUntilIdle()

            val message = viewModel.actionMessage.value
            assertEquals("Knjiga uklonjena iz košarice.", message)

            val state = viewModel.uiState.value
            assertTrue(state is CartUiState.Success)
            assertEquals(0, (state as CartUiState.Success).items.size)
        }
}
