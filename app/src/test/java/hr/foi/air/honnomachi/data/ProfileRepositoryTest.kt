package hr.foi.air.honnomachi.data

import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import hr.foi.air.honnomachi.CrashlyticsManager
import hr.foi.air.honnomachi.CrashlyticsService
import hr.foi.air.honnomachi.model.UserModel
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

class ProfileRepositoryTest {
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var repository: ProfileRepositoryImpl

    private lateinit var mockCollection: CollectionReference
    private lateinit var mockDocument: DocumentReference

    @Before
    fun setup() {
        auth = mockk(relaxed = true)
        firestore = mockk(relaxed = true)
        repository = ProfileRepositoryImpl(auth, firestore)

        mockCollection = mockk()
        mockDocument = mockk()

        every { firestore.collection("users") } returns mockCollection
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
    fun `getUserProfile no user returns error`() =
        runTest {
            every { auth.currentUser } returns null

            val result = repository.getUserProfile()

            assertTrue(result is Result.Error)
            assertEquals("No user logged in.", (result as Result.Error).exception.message)
        }

    @Test
    fun `getUserProfile success`() =
        runTest {
            val mockUser = mockk<FirebaseUser>()
            every { mockUser.uid } returns "uid123"
            every { auth.currentUser } returns mockUser

            val expectedUser = UserModel(uid = "uid123", name = "Test", email = "test@example.com")
            val mockSnapshot = mockk<DocumentSnapshot>()
            val mockTask = mockk<Task<DocumentSnapshot>>()

            every { mockDocument.get() } returns mockTask
            coEvery { mockTask.await() } returns mockSnapshot
            every { mockSnapshot.exists() } returns true
            every { mockSnapshot.toObject(UserModel::class.java) } returns expectedUser

            val result = repository.getUserProfile()

            assertTrue(result is Result.Success)
            assertEquals("Test", (result as Result.Success).data.name)
        }

    @Test
    fun `getUserProfile document not found`() =
        runTest {
            val mockUser = mockk<FirebaseUser>()
            every { mockUser.uid } returns "uid123"
            every { auth.currentUser } returns mockUser

            val mockSnapshot = mockk<DocumentSnapshot>()
            val mockTask = mockk<Task<DocumentSnapshot>>()

            every { mockDocument.get() } returns mockTask
            coEvery { mockTask.await() } returns mockSnapshot
            every { mockSnapshot.exists() } returns false

            val result = repository.getUserProfile()

            assertTrue(result is Result.Error)
            assertEquals(
                "User document not found.",
                (result as Result.Error).exception.message,
            )
        }

    @Test
    fun `updateUserProfile no user returns error`() =
        runTest {
            every { auth.currentUser } returns null

            val result = repository.updateUserProfile("Name", "123", "Street", "10000", "City")

            assertTrue(result is Result.Error)
            assertEquals("No user logged in.", (result as Result.Error).exception.message)
        }

    @Test
    fun `updateUserProfile success`() =
        runTest {
            val mockUser = mockk<FirebaseUser>()
            every { mockUser.uid } returns "uid123"
            every { auth.currentUser } returns mockUser

            val mockUpdateTask = mockk<Task<Void>>()
            every { mockDocument.update(any<Map<String, Any>>()) } returns mockUpdateTask
            coEvery { mockUpdateTask.await() } returns mockk()

            val updatedUser = UserModel(uid = "uid123", name = "Updated", email = "test@example.com")
            val mockSnapshot = mockk<DocumentSnapshot>()
            val mockGetTask = mockk<Task<DocumentSnapshot>>()

            every { mockDocument.get() } returns mockGetTask
            coEvery { mockGetTask.await() } returns mockSnapshot
            every { mockSnapshot.toObject(UserModel::class.java) } returns updatedUser

            val result = repository.updateUserProfile("Updated", "123", "Street", "10000", "City")

            assertTrue(result is Result.Success)
            assertEquals("Updated", (result as Result.Success).data.name)
        }

    @Test
    fun `changePassword no user returns error`() =
        runTest {
            every { auth.currentUser } returns null

            val result = repository.changePassword("oldPass", "newPass")

            assertTrue(result is Result.Error)
            assertEquals("No user logged in.", (result as Result.Error).exception.message)
        }

    @Test
    fun `changePassword success`() =
        runTest {
            val mockUser = mockk<FirebaseUser>()
            every { mockUser.email } returns "test@example.com"
            every { auth.currentUser } returns mockUser

            mockkStatic("com.google.firebase.auth.EmailAuthProvider")
            val mockCredential = mockk<AuthCredential>()
            every {
                com.google.firebase.auth.EmailAuthProvider
                    .getCredential("test@example.com", "oldPass")
            } returns mockCredential

            val mockReauthTask = mockk<Task<Void>>()
            every { mockUser.reauthenticate(mockCredential) } returns mockReauthTask
            coEvery { mockReauthTask.await() } returns mockk()

            val mockUpdateTask = mockk<Task<Void>>()
            every { mockUser.updatePassword("newPass") } returns mockUpdateTask
            coEvery { mockUpdateTask.await() } returns mockk()

            val result = repository.changePassword("oldPass", "newPass")

            assertTrue(result is Result.Success)

            unmockkStatic("com.google.firebase.auth.EmailAuthProvider")
        }

    @Test
    fun `changePassword failure on reauthenticate`() =
        runTest {
            val mockUser = mockk<FirebaseUser>()
            every { mockUser.email } returns "test@example.com"
            every { auth.currentUser } returns mockUser

            mockkStatic("com.google.firebase.auth.EmailAuthProvider")
            val mockCredential = mockk<AuthCredential>()
            every {
                com.google.firebase.auth.EmailAuthProvider
                    .getCredential("test@example.com", "oldPass")
            } returns mockCredential

            val mockReauthTask = mockk<Task<Void>>()
            every { mockUser.reauthenticate(mockCredential) } returns mockReauthTask
            coEvery { mockReauthTask.await() } throws Exception("Wrong password")

            val result = repository.changePassword("oldPass", "newPass")

            assertTrue(result is Result.Error)
            assertEquals("Wrong password", (result as Result.Error).exception.message)

            unmockkStatic("com.google.firebase.auth.EmailAuthProvider")
        }
}
