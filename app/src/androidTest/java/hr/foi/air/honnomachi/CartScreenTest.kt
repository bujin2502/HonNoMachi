package hr.foi.air.honnomachi

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import hr.foi.air.honnomachi.data.CartRepository
import hr.foi.air.honnomachi.data.CheckoutPaymentIntentModel
import hr.foi.air.honnomachi.data.CheckoutRepository
import hr.foi.air.honnomachi.model.BookModel
import hr.foi.air.honnomachi.model.CartItemModel
import hr.foi.air.honnomachi.ui.cart.CartPage
import hr.foi.air.honnomachi.ui.cart.CartViewModel
import hr.foi.air.honnomachi.ui.theme.HonNoMachiTheme
import hr.foi.air.honnomachi.util.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale

class FakeCartRepository : CartRepository {
    private val _cartItems = MutableStateFlow<List<CartItemModel>>(emptyList())

    fun addInitialItems(items: List<CartItemModel>) {
        _cartItems.value = items
    }

    override suspend fun addToCart(book: BookModel): Result<Unit> = Result.Success(Unit)

    override fun getCartItems(): Flow<Result<List<CartItemModel>>> = _cartItems.map { Result.Success(it) }

    override suspend fun removeFromCart(cartItemId: String): Result<Unit> {
        _cartItems.value = _cartItems.value.filter { it.id != cartItemId }
        return Result.Success(Unit)
    }
}

class FakeCheckoutRepository : CheckoutRepository {
    var createPaymentIntentCallCount = 0

    override suspend fun createCheckoutPaymentIntent(reservationTtlMinutes: Int?): Result<CheckoutPaymentIntentModel> {
        createPaymentIntentCallCount++
        return Result.Success(
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
    }
}

@RunWith(AndroidJUnit4::class)
class CartScreenTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var fakeCartRepository: FakeCartRepository
    private lateinit var fakeCheckoutRepository: FakeCheckoutRepository
    private lateinit var viewModel: CartViewModel

    @Before
    fun setup() {
        fakeCartRepository = FakeCartRepository()
        fakeCheckoutRepository = FakeCheckoutRepository()

        val initialItems =
            listOf(
                CartItemModel(id = "1", bookId = "1", title = "Oracle-Database", price = 11.8, currency = "USD"),
                CartItemModel(id = "2", bookId = "2", title = "Oracle-ADF", price = 30.0, currency = "EUR"),
            )
        fakeCartRepository.addInitialItems(initialItems)

        viewModel = CartViewModel(fakeCartRepository, fakeCheckoutRepository)
    }

    @Test
    fun cartPage_displaysItemsAndCanStartStripeCheckout() {
        composeTestRule.setContent {
            HonNoMachiTheme {
                CartPage(paddingValues = PaddingValues(), viewModel = viewModel, showImages = false)
            }
        }

        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithText("Oracle-Database").fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNodeWithText("Oracle-Database").assertIsDisplayed()
        composeTestRule.onNodeWithText("Oracle-ADF").assertIsDisplayed()

        val expectedPrice = (11.8 / 1.18) + 30.0
        val expectedPriceString = String.format(Locale.getDefault(), "%.2f EUR", expectedPrice)
        composeTestRule.onNodeWithText("Ukupno:").assertIsDisplayed()
        composeTestRule.onNodeWithText(expectedPriceString).assertIsDisplayed()

        composeTestRule.onNodeWithTag("pay_with_stripe_button").performClick()

        composeTestRule.waitUntil(timeoutMillis = 5000) {
            fakeCheckoutRepository.createPaymentIntentCallCount == 1
        }
    }
}
