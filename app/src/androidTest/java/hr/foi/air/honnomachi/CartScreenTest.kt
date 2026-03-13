package hr.foi.air.honnomachi

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
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
import kotlinx.coroutines.flow.flowOf
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

class FakeCartRepositoryError : CartRepository {
    override suspend fun addToCart(book: BookModel): Result<Unit> = Result.Success(Unit)

    override fun getCartItems(): Flow<Result<List<CartItemModel>>> = flowOf(Result.Error(Exception("Greška pri učitavanju")))

    override suspend fun removeFromCart(cartItemId: String): Result<Unit> = Result.Success(Unit)
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

    override suspend fun cancelCheckout(checkoutId: String): Result<Unit> = Result.Success(Unit)
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

    private fun launchCart(vm: CartViewModel = viewModel) {
        composeTestRule.setContent {
            HonNoMachiTheme {
                CartPage(paddingValues = PaddingValues(), viewModel = vm, showImages = false)
            }
        }
    }

    private fun waitForItems(vararg titles: String) {
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithText(titles.first()).fetchSemanticsNodes().isNotEmpty()
        }
    }

    // region Postojeći test

    @Test
    fun cartPage_displaysItemsAndCanStartStripeCheckout() {
        launchCart()
        waitForItems("Oracle-Database")

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

    // endregion

    // region Naslov i opći prikaz

    @Test
    fun cartPage_showsCartTitle() {
        launchCart()
        waitForItems("Oracle-Database")

        composeTestRule.onNodeWithTag("cart_page_title").assertIsDisplayed()
    }

    // endregion

    // region Stavke košarice

    @Test
    fun cartPage_showsItemAuthor() {
        fakeCartRepository.addInitialItems(
            listOf(CartItemModel(id = "10", title = "Test Book", author = "Autor Autorić", price = 10.0, currency = "EUR")),
        )
        val vm = CartViewModel(fakeCartRepository, fakeCheckoutRepository)
        launchCart(vm)
        waitForItems("Test Book")

        composeTestRule.onNodeWithText("Autor Autorić").assertIsDisplayed()
    }

    @Test
    fun cartPage_showsItemPriceAndCurrency() {
        fakeCartRepository.addInitialItems(
            listOf(CartItemModel(id = "11", title = "Skupa Knjiga", author = "A", price = 25.99, currency = "EUR")),
        )
        val vm = CartViewModel(fakeCartRepository, fakeCheckoutRepository)
        launchCart(vm)
        waitForItems("Skupa Knjiga")

        composeTestRule.onNodeWithText("25.99 EUR").assertIsDisplayed()
    }

    @Test
    fun cartPage_removingItem_removesItFromList() {
        fakeCartRepository.addInitialItems(
            listOf(
                CartItemModel(id = "a1", title = "Knjiga za brisanje", author = "A", price = 5.0, currency = "EUR"),
                CartItemModel(id = "a2", title = "Ova ostaje", author = "B", price = 8.0, currency = "EUR"),
            ),
        )
        val vm = CartViewModel(fakeCartRepository, fakeCheckoutRepository)
        launchCart(vm)
        waitForItems("Knjiga za brisanje")

        // Klikni delete na prvu stavku (contentDescription = "Remove")
        composeTestRule
            .onAllNodesWithContentDescription("Remove")
            .get(0)
            .performClick()
        composeTestRule.waitForIdle()

        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithText("Knjiga za brisanje").fetchSemanticsNodes().isEmpty()
        }

        assert(composeTestRule.onAllNodesWithText("Knjiga za brisanje").fetchSemanticsNodes().isEmpty())
        composeTestRule.onNodeWithText("Ova ostaje").assertIsDisplayed()
    }

    // endregion

    // region Prazna košarica

    @Test
    fun cartPage_showsEmptyCartMessage_whenNoItems() {
        // Pokrenuti s praznom košaricom (default setup bez addInitialItems)
        val emptyRepo = FakeCartRepository()
        val vm = CartViewModel(emptyRepo, fakeCheckoutRepository)
        launchCart(vm)

        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithText("Košarica je prazna.").fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNodeWithText("Košarica je prazna.").assertIsDisplayed()
    }

    @Test
    fun cartPage_emptyCart_doesNotShowPayButton() {
        val emptyRepo = FakeCartRepository()
        val vm = CartViewModel(emptyRepo, fakeCheckoutRepository)
        launchCart(vm)

        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithText("Košarica je prazna.").fetchSemanticsNodes().isNotEmpty()
        }

        assert(composeTestRule.onAllNodesWithTag("pay_with_stripe_button").fetchSemanticsNodes().isEmpty())
    }

    // endregion

    // region Stanje greške

    @Test
    fun cartPage_showsErrorMessage_whenRepositoryFails() {
        val errorRepo = FakeCartRepositoryError()
        val vm = CartViewModel(errorRepo, fakeCheckoutRepository)
        launchCart(vm)

        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule
                .onAllNodesWithText("Greška: Greška pri učitavanju")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        composeTestRule
            .onNodeWithText("Greška: Greška pri učitavanju")
            .assertIsDisplayed()
    }

    // endregion

    // region Checkout dialog

    @Test
    fun cartPage_showsCheckoutDialog_afterPayButtonClick() {
        launchCart()
        waitForItems("Oracle-Database")

        composeTestRule.onNodeWithTag("pay_with_stripe_button").performClick()

        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule
                .onAllNodesWithText(composeTestRule.activity.getString(R.string.checkout_dialog_title))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        composeTestRule
            .onNodeWithText(composeTestRule.activity.getString(R.string.checkout_dialog_title))
            .assertIsDisplayed()
    }

    // endregion
}
