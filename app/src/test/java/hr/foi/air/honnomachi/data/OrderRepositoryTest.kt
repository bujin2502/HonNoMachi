package hr.foi.air.honnomachi.data

import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
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

class OrderRepositoryTest {
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var repository: OrderRepositoryImpl

    @Before
    fun setup() {
        auth = mockk(relaxed = true)
        firestore = mockk(relaxed = true)
        repository = OrderRepositoryImpl(auth, firestore)

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
    fun `placeOrder no user returns error`() =
        runTest {
            every { auth.currentUser } returns null

            val result = repository.placeOrder()

            assertTrue(result is Result.Error)
            assertEquals("Korisnik nije prijavljen.", (result as Result.Error).exception.message)
        }

    @Test
    fun `placeOrder firestore exception returns error`() =
        runTest {
            val mockUser = mockk<FirebaseUser>()
            every { mockUser.uid } returns "user123"
            every { auth.currentUser } returns mockUser

            coEvery { any<Task<QuerySnapshot>>().await() } throws RuntimeException("Firestore nedostupan.")

            val result = repository.placeOrder()

            assertTrue(result is Result.Error)
            assertEquals("Firestore nedostupan.", (result as Result.Error).exception.message)
        }

    @Test
    fun `placeOrder empty cart returns error`() =
        runTest {
            val mockUser = mockk<FirebaseUser>()
            every { mockUser.uid } returns "user123"
            every { auth.currentUser } returns mockUser

            val mockSnapshot = mockk<QuerySnapshot>()
            every { mockSnapshot.isEmpty } returns true
            coEvery { any<Task<QuerySnapshot>>().await() } returns mockSnapshot

            val result = repository.placeOrder()

            assertTrue(result is Result.Error)
            assertEquals("Košarica je prazna.", (result as Result.Error).exception.message)
        }
}
