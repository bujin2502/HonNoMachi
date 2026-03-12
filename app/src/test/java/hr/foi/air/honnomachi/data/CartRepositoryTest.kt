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
import hr.foi.air.honnomachi.model.BookModel
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

class CartRepositoryTest {
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var functions: FirebaseFunctions
    private lateinit var repository: CartRepositoryImpl

    @Before
    fun setup() {
        auth = mockk(relaxed = true)
        firestore = mockk(relaxed = true)
        functions = mockk(relaxed = true)
        repository = CartRepositoryImpl(auth, firestore, functions)

        mockkStatic("kotlinx.coroutines.tasks.TasksKt")

        mockkObject(CrashlyticsManager.Companion)
        every { CrashlyticsManager.instance } returns mockk<CrashlyticsService>(relaxed = true)
    }

    @After
    fun tearDown() {
        unmockkStatic("kotlinx.coroutines.tasks.TasksKt")
        unmockkObject(CrashlyticsManager.Companion)
    }

    @Test
    fun `addToCart no user returns error`() =
        runTest {
            every { auth.currentUser } returns null

            val book = BookModel(bookId = "book1", title = "Test Book")
            val result = repository.addToCart(book)

            assertTrue(result is Result.Error)
            assertEquals(
                "Korisnik nije prijavljen ili knjiga nema ID.",
                (result as Result.Error).exception.message,
            )
        }

    @Test
    fun `addToCart no bookId returns error`() =
        runTest {
            val mockUser = mockk<FirebaseUser>()
            every { mockUser.uid } returns "user123"
            every { auth.currentUser } returns mockUser

            val book = BookModel(bookId = null, title = "No ID Book")
            val result = repository.addToCart(book)

            assertTrue(result is Result.Error)
            assertEquals(
                "Korisnik nije prijavljen ili knjiga nema ID.",
                (result as Result.Error).exception.message,
            )
        }

    @Test
    fun `addToCart success`() =
        runTest {
            val mockUser = mockk<FirebaseUser>()
            every { mockUser.uid } returns "user123"
            every { auth.currentUser } returns mockUser
            val mockTokenTask = mockk<Task<GetTokenResult>>()
            every { mockUser.getIdToken(true) } returns mockTokenTask
            coEvery { mockTokenTask.await() } returns mockk()

            val mockCallable = mockk<HttpsCallableReference>()
            val mockCallTask = mockk<Task<HttpsCallableResult>>()
            every { functions.getHttpsCallable("addToCartAndReserve") } returns mockCallable
            every { mockCallable.call(any()) } returns mockCallTask
            coEvery { mockCallTask.await() } returns mockk()

            val book = BookModel(bookId = "book1", title = "Test Book", price = 15.0)
            val result = repository.addToCart(book)

            assertTrue(result is Result.Success)
        }

    @Test
    fun `removeFromCart no user returns error`() =
        runTest {
            every { auth.currentUser } returns null

            val result = repository.removeFromCart("cart1")

            assertTrue(result is Result.Error)
            assertEquals(
                "Korisnik nije prijavljen.",
                (result as Result.Error).exception.message,
            )
        }

    @Test
    fun `removeFromCart success`() =
        runTest {
            val mockUser = mockk<FirebaseUser>()
            every { mockUser.uid } returns "user123"
            every { auth.currentUser } returns mockUser
            val mockTokenTask = mockk<Task<GetTokenResult>>()
            every { mockUser.getIdToken(true) } returns mockTokenTask
            coEvery { mockTokenTask.await() } returns mockk()

            val mockCallable = mockk<HttpsCallableReference>()
            val mockCallTask = mockk<Task<HttpsCallableResult>>()
            every { functions.getHttpsCallable("removeFromCartAndRelease") } returns mockCallable
            every { mockCallable.call(any()) } returns mockCallTask
            coEvery { mockCallTask.await() } returns mockk()

            val result = repository.removeFromCart("cart1")

            assertTrue(result is Result.Success)
        }
}
