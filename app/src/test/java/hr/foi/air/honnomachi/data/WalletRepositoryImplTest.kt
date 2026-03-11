package hr.foi.air.honnomachi.data

import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GetTokenResult
import com.google.firebase.firestore.FirebaseFirestore
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class WalletRepositoryImplTest {
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var functions: FirebaseFunctions
    private lateinit var repository: WalletRepositoryImpl

    private lateinit var mockUser: FirebaseUser
    private lateinit var mockCallable: HttpsCallableReference
    private lateinit var mockCallTask: Task<HttpsCallableResult>
    private lateinit var mockTokenTask: Task<GetTokenResult>

    @Before
    fun setup() {
        auth = mockk(relaxed = true)
        firestore = mockk(relaxed = true)
        functions = mockk(relaxed = true)
        repository = WalletRepositoryImpl(auth, firestore, functions)

        mockUser = mockk()
        mockCallable = mockk()
        mockCallTask = mockk()
        mockTokenTask = mockk()

        mockkStatic("kotlinx.coroutines.tasks.TasksKt")
        mockkObject(CrashlyticsManager.Companion)
        every { CrashlyticsManager.instance } returns mockk<CrashlyticsService>(relaxed = true)

        every { functions.getHttpsCallable(any()) } returns mockCallable
        every { mockCallable.call(any()) } returns mockCallTask
        every { mockUser.uid } returns "user123"
        every { mockUser.getIdToken(true) } returns mockTokenTask
        coEvery { mockTokenTask.await() } returns mockk()
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

    @Test
    fun `createWalletTopupIntent no user returns error`() =
        runTest {
            every { auth.currentUser } returns null

            val result = repository.createWalletTopupIntent(500, "eur", "key_1")

            assertTrue(result is Result.Error)
        }

    @Test
    fun `createWalletTopupIntent function throws returns error`() =
        runTest {
            every { auth.currentUser } returns mockUser
            coEvery { mockCallTask.await() } throws RuntimeException("Network error")

            val result = repository.createWalletTopupIntent(500, "eur", "key_1")

            assertTrue(result is Result.Error)
        }

    @Test
    fun `createWalletTopupIntent success returns model`() =
        runTest {
            every { auth.currentUser } returns mockUser
            coEvery { mockCallTask.await() } returns createCallableResult(
                mapOf(
                    "topupId" to "topup_123",
                    "paymentIntentId" to "pi_test",
                    "clientSecret" to "pi_test_secret",
                    "amountMinor" to 500,
                    "currency" to "eur",
                ),
            )

            val result = repository.createWalletTopupIntent(500, "eur", "key_1")

            assertTrue(result is Result.Success)
            val model = (result as Result.Success).data
            assertEquals("topup_123", model.topupId)
            assertEquals("pi_test_secret", model.clientSecret)
            assertEquals(500, model.amountMinor)
        }

    @Test
    fun `createWalletTopupIntent null topupId returns error`() =
        runTest {
            every { auth.currentUser } returns mockUser
            coEvery { mockCallTask.await() } returns createCallableResult(
                mapOf(
                    "topupId" to null,
                    "paymentIntentId" to "pi_test",
                    "clientSecret" to "pi_test_secret",
                    "amountMinor" to 500,
                    "currency" to "eur",
                ),
            )

            val result = repository.createWalletTopupIntent(500, "eur", "key_1")

            assertTrue(result is Result.Error)
        }

    @Test
    fun `createWalletTopupIntent zero amountMinor returns error`() =
        runTest {
            every { auth.currentUser } returns mockUser
            coEvery { mockCallTask.await() } returns createCallableResult(
                mapOf(
                    "topupId" to "topup_123",
                    "paymentIntentId" to "pi_test",
                    "clientSecret" to "pi_test_secret",
                    "amountMinor" to 0,
                    "currency" to "eur",
                ),
            )

            val result = repository.createWalletTopupIntent(500, "eur", "key_1")

            assertTrue(result is Result.Error)
        }

    @Test
    fun `createWalletTopupIntent null response returns error`() =
        runTest {
            every { auth.currentUser } returns mockUser
            coEvery { mockCallTask.await() } returns createCallableResult(null)

            val result = repository.createWalletTopupIntent(500, "eur", "key_1")

            assertTrue(result is Result.Error)
        }

    @Test
    fun `createWalletTopupIntent null clientSecret returns error`() =
        runTest {
            every { auth.currentUser } returns mockUser
            coEvery { mockCallTask.await() } returns createCallableResult(
                mapOf(
                    "topupId" to "topup_123",
                    "paymentIntentId" to "pi_test",
                    "clientSecret" to null,
                    "amountMinor" to 500,
                    "currency" to "eur",
                ),
            )

            val result = repository.createWalletTopupIntent(500, "eur", "key_1")

            assertTrue(result is Result.Error)
        }
}
