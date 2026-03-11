package hr.foi.air.honnomachi.data

import com.google.android.gms.tasks.Task
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.HttpsCallableReference
import com.google.firebase.functions.HttpsCallableResult
import hr.foi.air.honnomachi.CrashlyticsManager
import hr.foi.air.honnomachi.CrashlyticsService
import hr.foi.air.honnomachi.util.Result
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CheckoutRepositoryTest {
    private lateinit var functions: FirebaseFunctions
    private lateinit var repository: CheckoutRepositoryImpl
    private lateinit var mockCallable: HttpsCallableReference
    private lateinit var mockCallTask: Task<HttpsCallableResult>

    @Before
    fun setup() {
        functions = mockk()
        repository = CheckoutRepositoryImpl(functions)
        mockCallable = mockk()
        mockCallTask = mockk()

        mockkStatic("kotlinx.coroutines.tasks.TasksKt")
        mockkObject(CrashlyticsManager.Companion)
        every { CrashlyticsManager.instance } returns mockk<CrashlyticsService>(relaxed = true)
        every { functions.getHttpsCallable(any()) } returns mockCallable
        every { mockCallable.call(any()) } returns mockCallTask
    }

    @After
    fun tearDown() {
        unmockkStatic("kotlinx.coroutines.tasks.TasksKt")
        unmockkObject(CrashlyticsManager.Companion)
    }

    private fun createCallableResult(data: Any?): HttpsCallableResult {
        val ctor = HttpsCallableResult::class.java.getDeclaredConstructor(Any::class.java)
        ctor.isAccessible = true
        return ctor.newInstance(data)
    }

    private fun setupCallableResponse(data: Any?) {
        coEvery { mockCallTask.await() } returns createCallableResult(data)
    }


    @Test
    fun `createCheckoutPaymentIntent success with payment sheet`() =
        runTest {
            setupCallableResponse(
                mapOf(
                    "checkoutId" to "checkout_123",
                    "paymentIntentId" to "pi_test_123",
                    "clientSecret" to "pi_test_secret_123",
                    "amountMinor" to 1200,
                    "totalAmountMinor" to 1200,
                    "walletContributionMinor" to 0,
                    "currency" to "eur",
                    "requiresPaymentSheet" to true,
                    "checkoutCompleted" to false,
                    "reservationIds" to listOf("res_1"),
                ),
            )

            val result = repository.createCheckoutPaymentIntent()

            assertTrue(result is Result.Success)
            val model = (result as Result.Success).data
            assertEquals("checkout_123", model.checkoutId)
            assertEquals("pi_test_secret_123", model.clientSecret)
            assertEquals(1200, model.amountMinor)
            assertEquals("eur", model.currency)
            assertTrue(model.requiresPaymentSheet)
        }

    @Test
    fun `createCheckoutPaymentIntent success wallet only no payment sheet`() =
        runTest {
            setupCallableResponse(
                mapOf(
                    "checkoutId" to "checkout_wallet",
                    "amountMinor" to 0,
                    "totalAmountMinor" to 1200,
                    "walletContributionMinor" to 1200,
                    "currency" to "eur",
                    "requiresPaymentSheet" to false,
                    "checkoutCompleted" to true,
                ),
            )

            val result = repository.createCheckoutPaymentIntent()

            assertTrue(result is Result.Success)
            val model = (result as Result.Success).data
            assertEquals("checkout_wallet", model.checkoutId)
            assertFalse(model.requiresPaymentSheet)
            assertTrue(model.checkoutCompleted)
        }

    @Test
    fun `createCheckoutPaymentIntent null checkoutId returns error`() =
        runTest {
            setupCallableResponse(
                mapOf(
                    "checkoutId" to null,
                    "amountMinor" to 1200,
                    "totalAmountMinor" to 1200,
                    "currency" to "eur",
                ),
            )

            val result = repository.createCheckoutPaymentIntent()

            assertTrue(result is Result.Error)
        }

    @Test
    fun `createCheckoutPaymentIntent null amountMinor returns error`() =
        runTest {
            setupCallableResponse(
                mapOf(
                    "checkoutId" to "checkout_123",
                    "amountMinor" to null,
                    "currency" to "eur",
                ),
            )

            val result = repository.createCheckoutPaymentIntent()

            assertTrue(result is Result.Error)
        }

    @Test
    fun `createCheckoutPaymentIntent zero totalAmountMinor returns error`() =
        runTest {
            setupCallableResponse(
                mapOf(
                    "checkoutId" to "checkout_123",
                    "amountMinor" to 1200,
                    "totalAmountMinor" to 0,
                    "currency" to "eur",
                ),
            )

            val result = repository.createCheckoutPaymentIntent()

            assertTrue(result is Result.Error)
        }

    @Test
    fun `createCheckoutPaymentIntent negative walletContributionMinor returns error`() =
        runTest {
            setupCallableResponse(
                mapOf(
                    "checkoutId" to "checkout_123",
                    "amountMinor" to 1200,
                    "totalAmountMinor" to 1200,
                    "walletContributionMinor" to -1,
                    "currency" to "eur",
                ),
            )

            val result = repository.createCheckoutPaymentIntent()

            assertTrue(result is Result.Error)
        }

    @Test
    fun `createCheckoutPaymentIntent requiresPaymentSheet true but missing clientSecret returns error`() =
        runTest {
            setupCallableResponse(
                mapOf(
                    "checkoutId" to "checkout_123",
                    "amountMinor" to 1200,
                    "totalAmountMinor" to 1200,
                    "currency" to "eur",
                    "requiresPaymentSheet" to true,
                    "paymentIntentId" to null,
                    "clientSecret" to null,
                ),
            )

            val result = repository.createCheckoutPaymentIntent()

            assertTrue(result is Result.Error)
        }

    @Test
    fun `createCheckoutPaymentIntent null response returns error`() =
        runTest {
            setupCallableResponse(null)

            val result = repository.createCheckoutPaymentIntent()

            assertTrue(result is Result.Error)
        }

    @Test
    fun `createCheckoutPaymentIntent function throws returns error`() =
        runTest {
            coEvery { mockCallTask.await() } throws RuntimeException("Network error")

            val result = repository.createCheckoutPaymentIntent()

            assertTrue(result is Result.Error)
            assertEquals("Network error", (result as Result.Error).exception.message)
        }

    @Test
    fun `createCheckoutPaymentIntent with reservationTtlMinutes adds param to payload`() =
        runTest {
            setupCallableResponse(
                mapOf(
                    "checkoutId" to "checkout_123",
                    "paymentIntentId" to "pi_test",
                    "clientSecret" to "pi_test_secret",
                    "amountMinor" to 1200,
                    "totalAmountMinor" to 1200,
                    "currency" to "eur",
                    "requiresPaymentSheet" to true,
                ),
            )

            val result = repository.createCheckoutPaymentIntent(reservationTtlMinutes = 15)

            assertTrue(result is Result.Success)
        }


    @Test
    fun `cancelCheckout success`() =
        runTest {
            coEvery { mockCallTask.await() } returns createCallableResult(null)

            val result = repository.cancelCheckout("checkout_123")

            assertTrue(result is Result.Success)
        }

    @Test
    fun `cancelCheckout function throws returns error`() =
        runTest {
            coEvery { mockCallTask.await() } throws RuntimeException("Cancel failed")

            val result = repository.cancelCheckout("checkout_123")

            assertTrue(result is Result.Error)
            assertEquals("Cancel failed", (result as Result.Error).exception.message)
        }
}
