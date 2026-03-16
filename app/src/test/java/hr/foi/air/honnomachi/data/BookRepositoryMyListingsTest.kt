package hr.foi.air.honnomachi.data

import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import hr.foi.air.honnomachi.CrashlyticsManager
import hr.foi.air.honnomachi.CrashlyticsService
import hr.foi.air.honnomachi.model.ItemStatus
import hr.foi.air.honnomachi.util.Result
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BookRepositoryMyListingsTest {
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
    fun `updateListingStatus returns success`() =
        runTest {
            val mockUpdateTask = mockk<Task<Void>>()
            every { mockDocument.update("status", ItemStatus.INACTIVE.name) } returns mockUpdateTask
            coEvery { mockUpdateTask.await() } returns mockk()

            val result = repository.updateListingStatus("book123", ItemStatus.INACTIVE)

            assertTrue(result is Result.Success)
        }

    @Test
    fun `updateListingStatus returns error on exception`() =
        runTest {
            val mockUpdateTask = mockk<Task<Void>>()
            every { mockDocument.update("status", ItemStatus.INACTIVE.name) } returns mockUpdateTask
            coEvery { mockUpdateTask.await() } throws Exception("Update failed")

            val result = repository.updateListingStatus("book123", ItemStatus.INACTIVE)

            assertTrue(result is Result.Error)
            assertEquals("Update failed", (result as Result.Error).exception.message)
        }

    @Test
    fun `deleteListing returns success`() =
        runTest {
            val mockDeleteTask = mockk<Task<Void>>()
            every { mockDocument.delete() } returns mockDeleteTask
            coEvery { mockDeleteTask.await() } returns mockk()

            val result = repository.deleteListing("book123")

            assertTrue(result is Result.Success)
        }

    @Test
    fun `deleteListing returns error on exception`() =
        runTest {
            val mockDeleteTask = mockk<Task<Void>>()
            every { mockDocument.delete() } returns mockDeleteTask
            coEvery { mockDeleteTask.await() } throws Exception("Delete failed")

            val result = repository.deleteListing("book123")

            assertTrue(result is Result.Error)
            assertEquals("Delete failed", (result as Result.Error).exception.message)
        }

    @Test
    fun `updateListingStatus activates inactive listing`() =
        runTest {
            val mockUpdateTask = mockk<Task<Void>>()
            every { mockDocument.update("status", ItemStatus.AVAILABLE.name) } returns mockUpdateTask
            coEvery { mockUpdateTask.await() } returns mockk()

            val result = repository.updateListingStatus("book123", ItemStatus.AVAILABLE)

            assertTrue(result is Result.Success)
            verify { mockDocument.update("status", ItemStatus.AVAILABLE.name) }
        }
}
