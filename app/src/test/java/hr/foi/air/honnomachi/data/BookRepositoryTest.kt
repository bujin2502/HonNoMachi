package hr.foi.air.honnomachi.data

import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
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

class BookRepositoryTest {
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var repository: BookRepositoryImpl

    private lateinit var mockCollection: CollectionReference
    private lateinit var mockDocument: DocumentReference

    @Before
    fun setup() {
        auth = mockk(relaxed = true)
        firestore = mockk(relaxed = true)
        repository = BookRepositoryImpl(auth, firestore)

        mockCollection = mockk()
        mockDocument = mockk()

        every { firestore.collection("books") } returns mockCollection
        every { mockCollection.document(any()) } returns mockDocument

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
    fun `getBookDetails returns book successfully`() =
        runTest {
            val mockSnapshot = mockk<DocumentSnapshot>()
            val expectedBook = BookModel(title = "Test Book", authors = listOf("Author"))
            val mockTask = mockk<Task<DocumentSnapshot>>()

            every { mockDocument.get() } returns mockTask
            coEvery { mockTask.await() } returns mockSnapshot
            every { mockSnapshot.toObject(BookModel::class.java) } returns expectedBook

            val result = repository.getBookDetails("book123")

            assertTrue(result is Result.Success)
            assertEquals("Test Book", (result as Result.Success).data?.title)
        }

    @Test
    fun `getBookDetails returns error on exception`() =
        runTest {
            val mockTask = mockk<Task<DocumentSnapshot>>()

            every { mockDocument.get() } returns mockTask
            coEvery { mockTask.await() } throws Exception("Firestore error")

            val result = repository.getBookDetails("book123")

            assertTrue(result is Result.Error)
            assertEquals("Firestore error", (result as Result.Error).exception.message)
        }

    @Test
    fun `addBook returns error when no user`() =
        runTest {
            every { auth.currentUser } returns null

            val result = repository.addBook(BookModel(title = "New Book"))

            assertTrue(result is Result.Error)
            assertEquals(
                "Korisnik nije prijavljen.",
                (result as Result.Error).exception.message,
            )
        }

    @Test
    fun `addBook returns success with valid user`() =
        runTest {
            val mockUser = mockk<FirebaseUser>()
            every { mockUser.uid } returns "user123"
            every { auth.currentUser } returns mockUser

            every { mockCollection.document() } returns mockDocument
            every { mockDocument.id } returns "newBookId"

            val mockSetTask = mockk<Task<Void>>()
            every { mockDocument.set(any()) } returns mockSetTask
            coEvery { mockSetTask.await() } returns mockk()

            val result = repository.addBook(BookModel(title = "New Book"))

            assertTrue(result is Result.Success)
            assertEquals("newBookId", (result as Result.Success).data)
        }

    @Test
    fun `addBook returns error on exception`() =
        runTest {
            val mockUser = mockk<FirebaseUser>()
            every { mockUser.uid } returns "user123"
            every { auth.currentUser } returns mockUser

            every { mockCollection.document() } returns mockDocument

            val mockSetTask = mockk<Task<Void>>()
            every { mockDocument.set(any()) } returns mockSetTask
            coEvery { mockSetTask.await() } throws Exception("Write failed")

            val result = repository.addBook(BookModel(title = "New Book"))

            assertTrue(result is Result.Error)
            assertEquals("Write failed", (result as Result.Error).exception.message)
        }
}
