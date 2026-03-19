package hr.foi.air.honnomachi.viewmodel

import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.stripe.android.paymentsheet.PaymentSheetResult
import hr.foi.air.honnomachi.data.CheckoutPaymentIntentModel
import hr.foi.air.honnomachi.model.BookModel
import hr.foi.air.honnomachi.model.CartItemModel
import hr.foi.air.honnomachi.model.Currency
import hr.foi.air.honnomachi.ui.cart.CartUiState
import hr.foi.air.honnomachi.ui.cart.CartViewModel
import hr.foi.air.honnomachi.ui.cart.CheckoutNavigationTarget
import hr.foi.air.honnomachi.util.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class CartViewModelNewTest {
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
    fun `addToCart null bookId sets error message synchronously`() =
        runTest(testDispatcher) {
            viewModel.addToCart(BookModel(bookId = null, title = "No ID"))

            assertEquals("Greška: Nevažeći ID knjige.", viewModel.actionMessage.value)
            viewModel.viewModelScope.cancel()
        }

    @Test
    fun `addToCart empty bookId sets error message synchronously`() =
        runTest(testDispatcher) {
            viewModel.addToCart(BookModel(bookId = "", title = "Empty ID"))

            assertEquals("Greška: Nevažeći ID knjige.", viewModel.actionMessage.value)
            viewModel.viewModelScope.cancel()
        }

    @Test
    fun `checkoutWithStripe loading state shows empty cart error`() =
        runTest(testDispatcher) {
            viewModel.checkoutWithStripe()

            assertEquals("Košarica je prazna.", viewModel.actionMessage.value)
            viewModel.viewModelScope.cancel()
        }

    @Test
    fun `checkoutWithStripe success cart calls checkout and sets pendingCheckout`() =
        runTest(testDispatcher) {
            val book = BookModel(bookId = "1", title = "Test", price = 10.0, priceCurrency = Currency.EUR)
            fakeCartRepository.addToCart(book)
            advanceTimeBy(500)
            runCurrent()

            viewModel.checkoutWithStripe()
            advanceTimeBy(500)
            runCurrent()

            assertEquals(1, fakeCheckoutRepository.createPaymentIntentCallCount)
            assertEquals("pi_test", viewModel.pendingCheckout.value?.checkoutId)
            viewModel.viewModelScope.cancel()
        }

    @Test
    fun `checkoutWithStripe error shows error message`() =
        runTest(testDispatcher) {
            fakeCheckoutRepository.resultToReturn = Result.Error(Exception("Backend error"))

            val book = BookModel(bookId = "1", title = "Test", price = 10.0, priceCurrency = Currency.EUR)
            fakeCartRepository.addToCart(book)
            advanceTimeBy(500)
            runCurrent()

            viewModel.checkoutWithStripe()
            advanceTimeBy(500)
            runCurrent()

            assertTrue(viewModel.actionMessage.value!!.contains("Backend error"))
            viewModel.viewModelScope.cancel()
        }

    @Test
    fun `confirmCheckoutPayment no pending checkout does nothing`() =
        runTest(testDispatcher) {
            assertNull(viewModel.pendingCheckout.value)
            viewModel.confirmCheckoutPayment()

            assertNull(viewModel.actionMessage.value)
            viewModel.viewModelScope.cancel()
        }

    @Test
    fun `confirmCheckoutPayment with payment sheet emits request and shows message`() =
        runTest(testDispatcher) {
            val book = BookModel(bookId = "1", title = "Test", price = 10.0, priceCurrency = Currency.EUR)
            fakeCartRepository.addToCart(book)
            advanceTimeBy(500)
            runCurrent()

            viewModel.checkoutWithStripe()
            advanceTimeBy(500)
            runCurrent()

            val emitted = mutableListOf<CheckoutPaymentIntentModel>()
            val job =
                backgroundScope.launch {
                    viewModel.paymentSheetRequests.collect { emitted += it }
                }
            advanceTimeBy(100)
            runCurrent()

            viewModel.confirmCheckoutPayment()
            advanceTimeBy(100)
            runCurrent()

            assertEquals(1, emitted.size)
            assertTrue(viewModel.actionMessage.value!!.contains("Stripe"))

            job.cancel()
            viewModel.viewModelScope.cancel()
        }

    @Test
    fun `confirmCheckoutPayment wallet only shows success message and navigates`() =
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

            val book = BookModel(bookId = "1", title = "Test", price = 10.0, priceCurrency = Currency.EUR)
            fakeCartRepository.addToCart(book)
            advanceTimeBy(500)
            runCurrent()

            viewModel.checkoutWithStripe()
            advanceTimeBy(500)
            runCurrent()

            val navigations = mutableListOf<CheckoutNavigationTarget>()
            val job =
                backgroundScope.launch {
                    viewModel.checkoutNavigation.collect { navigations += it }
                }
            advanceTimeBy(100)
            runCurrent()

            viewModel.confirmCheckoutPayment()
            advanceTimeBy(100)
            runCurrent()

            assertEquals(1, navigations.size)
            assertEquals(CheckoutNavigationTarget.SUCCESS, navigations[0])

            job.cancel()
            viewModel.viewModelScope.cancel()
        }

    @Test
    fun `dismissCheckoutDialog no pending checkout does nothing`() =
        runTest(testDispatcher) {
            assertNull(viewModel.pendingCheckout.value)
            viewModel.dismissCheckoutDialog()

            assertNull(viewModel.pendingCheckout.value)
            viewModel.viewModelScope.cancel()
        }

    @Test
    fun `dismissCheckoutDialog with payment sheet checkout releases checkout`() =
        runTest(testDispatcher) {
            val book = BookModel(bookId = "1", title = "Test", price = 10.0, priceCurrency = Currency.EUR)
            fakeCartRepository.addToCart(book)
            advanceTimeBy(500)
            runCurrent()

            viewModel.checkoutWithStripe()
            advanceTimeBy(500)
            runCurrent()

            viewModel.dismissCheckoutDialog()
            advanceTimeBy(500)
            runCurrent()

            assertNull(viewModel.pendingCheckout.value)
            viewModel.viewModelScope.cancel()
        }

    @Test
    fun `onPaymentSheetResult Completed sets message and navigates to success`() =
        runTest(testDispatcher) {
            val navigations = mutableListOf<CheckoutNavigationTarget>()
            val job =
                backgroundScope.launch {
                    viewModel.checkoutNavigation.collect { navigations += it }
                }
            advanceTimeBy(100)
            runCurrent()

            viewModel.onPaymentSheetResult(PaymentSheetResult.Completed)
            advanceTimeBy(100)
            runCurrent()

            assertEquals("Plaćanje zaprimljeno.", viewModel.actionMessage.value)
            assertEquals(1, navigations.size)
            assertEquals(CheckoutNavigationTarget.SUCCESS, navigations[0])

            job.cancel()
            viewModel.viewModelScope.cancel()
        }

    @Test
    fun `onPaymentSheetResult Canceled sets message`() =
        runTest(testDispatcher) {
            viewModel.onPaymentSheetResult(PaymentSheetResult.Canceled)

            assertEquals("Plaćanje je otkazano.", viewModel.actionMessage.value)
            viewModel.viewModelScope.cancel()
        }

    @Test
    fun `onPaymentSheetResult Failed sets error message`() =
        runTest(testDispatcher) {
            viewModel.onPaymentSheetResult(PaymentSheetResult.Failed(Exception("Card error")))

            assertTrue(viewModel.actionMessage.value!!.contains("Card error"))
            viewModel.viewModelScope.cancel()
        }

    @Test
    fun `onPaymentSheetPresentationFailed sets error message`() =
        runTest(testDispatcher) {
            viewModel.onPaymentSheetPresentationFailed(Exception("Presentation error"))

            assertTrue(viewModel.actionMessage.value!!.contains("Presentation error"))
            viewModel.viewModelScope.cancel()
        }

    @Test
    fun `consumeMessage clears action message`() =
        runTest(testDispatcher) {
            viewModel.addToCart(BookModel(bookId = null, title = "Test"))
            assertEquals("Greška: Nevažeći ID knjige.", viewModel.actionMessage.value)

            viewModel.consumeMessage()

            assertNull(viewModel.actionMessage.value)
            viewModel.viewModelScope.cancel()
        }

    @Test
    fun `removeFromCart success sets message`() =
        runTest(testDispatcher) {
            viewModel.removeFromCart("cartItem1")
            advanceTimeBy(500)
            runCurrent()

            assertEquals("Knjiga uklonjena iz košarice.", viewModel.actionMessage.value)
            viewModel.viewModelScope.cancel()
        }

    @Test
    fun `loadCartItems sets success state with items and total price`() =
        runTest(testDispatcher) {
            val book = BookModel(bookId = "1", title = "Test Book", price = 15.0, priceCurrency = Currency.EUR)
            fakeCartRepository.addToCart(book)
            advanceTimeBy(500)
            runCurrent()

            val state = viewModel.uiState.value
            assertTrue(state is CartUiState.Success)
            assertEquals(1, (state as CartUiState.Success).items.size)
            assertEquals(15.0, state.totalPrice, 0.01)
            viewModel.viewModelScope.cancel()
        }

    @Test
    fun `isCheckoutInProgress is false initially`() =
        runTest(testDispatcher) {
            assertFalse(viewModel.isCheckoutInProgress.value)
            viewModel.viewModelScope.cancel()
        }

    @Test
    fun `reservation expires during active checkout - expiry message shown and checkout still completes`() =
        runTest(testDispatcher) {
            val pastTimestamp = Timestamp(System.currentTimeMillis() / 1000 - 10, 0)
            fakeCartRepository.setItems(
                listOf(
                    CartItemModel(
                        id = "book1",
                        bookId = "book1",
                        title = "Expired Book",
                        price = 10.0,
                        currency = "EUR",
                        reservationExpiresAt = pastTimestamp,
                    ),
                ),
            )
            advanceTimeBy(500)
            runCurrent()

            viewModel.checkoutWithStripe()
            advanceTimeBy(500)
            runCurrent()

            assertEquals(1, fakeCheckoutRepository.createPaymentIntentCallCount)

            val navigations = mutableListOf<CheckoutNavigationTarget>()
            val navJob =
                backgroundScope.launch {
                    viewModel.checkoutNavigation.collect { navigations += it }
                }
            advanceTimeBy(100)
            runCurrent()
            advanceTimeBy(1_500)
            runCurrent()

            assertEquals(
                "Rezervacija je istekla. Knjige su uklonjene iz košarice.",
                viewModel.actionMessage.value,
            )
            viewModel.onPaymentSheetResult(PaymentSheetResult.Completed)
            advanceTimeBy(100)
            runCurrent()

            assertEquals(1, navigations.size)
            assertEquals(CheckoutNavigationTarget.SUCCESS, navigations[0])

            navJob.cancel()
            viewModel.viewModelScope.cancel()
        }

    @Test
    fun `checkoutWithStripe shows loading state during slow network and clears it after response`() =
        runTest(testDispatcher) {
            fakeCheckoutRepository.createPaymentIntentDelayMs = 5_000L

            val book = BookModel(bookId = "1", title = "Test", price = 10.0, priceCurrency = Currency.EUR)
            fakeCartRepository.addToCart(book)
            advanceTimeBy(500)
            runCurrent()

            viewModel.checkoutWithStripe()
            advanceTimeBy(100)
            runCurrent()

            assertTrue(viewModel.isCheckoutInProgress.value)

            advanceTimeBy(5_000)
            runCurrent()

            assertFalse(viewModel.isCheckoutInProgress.value)
            viewModel.viewModelScope.cancel()
        }
}
