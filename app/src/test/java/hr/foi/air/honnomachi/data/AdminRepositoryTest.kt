package hr.foi.air.honnomachi.data

import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.WriteBatch
import hr.foi.air.honnomachi.CrashlyticsManager
import hr.foi.air.honnomachi.CrashlyticsService
import hr.foi.air.honnomachi.model.SuspensionHistoryEntry
import hr.foi.air.honnomachi.model.UserModel
import hr.foi.air.honnomachi.util.Result
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AdminRepositoryTest {
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var repository: AdminRepositoryImpl

    private lateinit var mockCollection: CollectionReference
    private lateinit var mockDocument: DocumentReference

    @Before
    fun setup() {
        auth = mockk(relaxed = true)
        firestore = mockk(relaxed = true)
        repository = AdminRepositoryImpl(auth, firestore)

        mockCollection = mockk(relaxed = true)
        mockDocument = mockk(relaxed = true)

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
    fun `isCurrentUserAdmin returns true for admin user`() =
        runTest {
            val mockUser = mockk<FirebaseUser>()
            every { mockUser.uid } returns "admin-uid"
            every { auth.currentUser } returns mockUser

            val mockSnapshot = mockk<DocumentSnapshot>()
            val adminUser = UserModel(name = "Admin", email = "admin@test.com", admin = true)

            val mockTask = mockk<Task<DocumentSnapshot>>()
            every { mockDocument.get() } returns mockTask
            coEvery { mockTask.await() } returns mockSnapshot
            every { mockSnapshot.toObject(UserModel::class.java) } returns adminUser

            val result = repository.isCurrentUserAdmin()

            assertTrue(result is Result.Success)
            assertTrue((result as Result.Success).data)
        }

    @Test
    fun `isCurrentUserAdmin returns false for non-admin user`() =
        runTest {
            val mockUser = mockk<FirebaseUser>()
            every { mockUser.uid } returns "user-uid"
            every { auth.currentUser } returns mockUser

            val mockSnapshot = mockk<DocumentSnapshot>()
            val regularUser = UserModel(name = "User", email = "user@test.com", admin = null)

            val mockTask = mockk<Task<DocumentSnapshot>>()
            every { mockDocument.get() } returns mockTask
            coEvery { mockTask.await() } returns mockSnapshot
            every { mockSnapshot.toObject(UserModel::class.java) } returns regularUser

            val result = repository.isCurrentUserAdmin()

            assertTrue(result is Result.Success)
            assertFalse((result as Result.Success).data)
        }

    @Test
    fun `isCurrentUserAdmin returns error when no user logged in`() =
        runTest {
            every { auth.currentUser } returns null

            val result = repository.isCurrentUserAdmin()

            assertTrue(result is Result.Error)
            assertEquals("No user logged in.", (result as Result.Error).exception.message)
        }

    @Test
    fun `isCurrentUserAdmin returns error on exception`() =
        runTest {
            val mockUser = mockk<FirebaseUser>()
            every { mockUser.uid } returns "user-uid"
            every { auth.currentUser } returns mockUser

            val mockTask = mockk<Task<DocumentSnapshot>>()
            every { mockDocument.get() } returns mockTask
            coEvery { mockTask.await() } throws Exception("Network error")

            val result = repository.isCurrentUserAdmin()

            assertTrue(result is Result.Error)
            assertEquals("Network error", (result as Result.Error).exception.message)
        }

    @Test
    fun `getAllUsers returns first page successfully`() =
        runTest {
            val mockQuery = mockk<Query>(relaxed = true)
            every { mockCollection.orderBy("name") } returns mockQuery
            every { mockQuery.limit(any()) } returns mockQuery

            val mockQuerySnapshot = mockk<QuerySnapshot>()
            val mockDoc1 = mockk<DocumentSnapshot>()
            val mockDoc2 = mockk<DocumentSnapshot>()

            val user1 = UserModel(name = "Alice", email = "alice@test.com")
            val user2 = UserModel(name = "Bob", email = "bob@test.com")

            every { mockDoc1.toObject(UserModel::class.java) } returns user1
            every { mockDoc1.id } returns "uid-1"
            every { mockDoc2.toObject(UserModel::class.java) } returns user2
            every { mockDoc2.id } returns "uid-2"
            every { mockQuerySnapshot.documents } returns listOf(mockDoc1, mockDoc2)

            val mockTask = mockk<Task<QuerySnapshot>>()
            every { mockQuery.get() } returns mockTask
            coEvery { mockTask.await() } returns mockQuerySnapshot

            val result = repository.getAllUsers(pageSize = 20)

            assertTrue(result is Result.Success)
            val page = (result as Result.Success).data
            assertEquals(2, page.users.size)
            assertEquals("uid-1", page.users[0].uid)
            assertEquals("Alice", page.users[0].name)
            assertEquals("uid-2", page.users[1].uid)
        }

    @Test
    fun `getAllUsers returns error on exception`() =
        runTest {
            val mockQuery = mockk<Query>(relaxed = true)
            every { mockCollection.orderBy("name") } returns mockQuery
            every { mockQuery.limit(any()) } returns mockQuery

            val mockTask = mockk<Task<QuerySnapshot>>()
            every { mockQuery.get() } returns mockTask
            coEvery { mockTask.await() } throws Exception("Firestore error")

            val result = repository.getAllUsers()

            assertTrue(result is Result.Error)
            assertEquals("Firestore error", (result as Result.Error).exception.message)
        }

    @Test
    fun `getUserById returns user successfully`() =
        runTest {
            val mockSnapshot = mockk<DocumentSnapshot>()
            val expectedUser = UserModel(name = "Test User", email = "test@test.com")

            val mockTask = mockk<Task<DocumentSnapshot>>()
            every { mockDocument.get() } returns mockTask
            coEvery { mockTask.await() } returns mockSnapshot
            every { mockSnapshot.exists() } returns true
            every { mockSnapshot.toObject(UserModel::class.java) } returns expectedUser
            every { mockSnapshot.id } returns "test-uid"

            val result = repository.getUserById("test-uid")

            assertTrue(result is Result.Success)
            assertEquals("Test User", (result as Result.Success).data.name)
            assertEquals("test-uid", result.data.uid)
        }

    @Test
    fun `getUserById returns error when user not found`() =
        runTest {
            val mockSnapshot = mockk<DocumentSnapshot>()

            val mockTask = mockk<Task<DocumentSnapshot>>()
            every { mockDocument.get() } returns mockTask
            coEvery { mockTask.await() } returns mockSnapshot
            every { mockSnapshot.exists() } returns false

            val result = repository.getUserById("nonexistent")

            assertTrue(result is Result.Error)
            assertEquals("User not found.", (result as Result.Error).exception.message)
        }

    @Test
    fun `getUserById returns error on exception`() =
        runTest {
            val mockTask = mockk<Task<DocumentSnapshot>>()
            every { mockDocument.get() } returns mockTask
            coEvery { mockTask.await() } throws Exception("Timeout")

            val result = repository.getUserById("test-uid")

            assertTrue(result is Result.Error)
            assertEquals("Timeout", (result as Result.Error).exception.message)
        }

    @Test
    fun `searchUsers filters by name case-insensitive`() =
        runTest {
            val mockQuerySnapshot = mockk<QuerySnapshot>()
            val mockDoc1 = mockk<DocumentSnapshot>()
            val mockDoc2 = mockk<DocumentSnapshot>()
            val mockDoc3 = mockk<DocumentSnapshot>()

            every { mockDoc1.toObject(UserModel::class.java) } returns UserModel(name = "Ivan Giljevic", email = "ivan@test.com")
            every { mockDoc1.id } returns "uid-1"
            every { mockDoc2.toObject(UserModel::class.java) } returns UserModel(name = "Ana Markovic", email = "ana@test.com")
            every { mockDoc2.id } returns "uid-2"
            every { mockDoc3.toObject(UserModel::class.java) } returns UserModel(name = "Marko Ivanic", email = "marko@test.com")
            every { mockDoc3.id } returns "uid-3"
            every { mockQuerySnapshot.documents } returns listOf(mockDoc1, mockDoc2, mockDoc3)

            val mockTask = mockk<Task<QuerySnapshot>>()
            every { mockCollection.get() } returns mockTask
            coEvery { mockTask.await() } returns mockQuerySnapshot

            val result = repository.searchUsers("ivan")

            assertTrue(result is Result.Success)
            val users = (result as Result.Success).data
            assertEquals(2, users.size)
            assertEquals("Ivan Giljevic", users[0].name)
            assertEquals("Marko Ivanic", users[1].name)
        }

    @Test
    fun `getUsersByStatus returns only suspended users`() =
        runTest {
            val mockQuerySnapshot = mockk<QuerySnapshot>()
            val mockDoc1 = mockk<DocumentSnapshot>()
            val mockDoc2 = mockk<DocumentSnapshot>()

            every { mockDoc1.toObject(UserModel::class.java) } returns UserModel(name = "Active User", suspended = false)
            every { mockDoc1.id } returns "uid-1"
            every { mockDoc2.toObject(UserModel::class.java) } returns UserModel(name = "Suspended User", suspended = true)
            every { mockDoc2.id } returns "uid-2"
            every { mockQuerySnapshot.documents } returns listOf(mockDoc1, mockDoc2)

            val mockTask = mockk<Task<QuerySnapshot>>()
            every { mockCollection.get() } returns mockTask
            coEvery { mockTask.await() } returns mockQuerySnapshot

            val result = repository.getUsersByStatus(isSuspended = true)

            assertTrue(result is Result.Success)
            val users = (result as Result.Success).data
            assertEquals(1, users.size)
            assertEquals("Suspended User", users[0].name)
        }

    @Test
    fun `suspendUser returns error when no admin logged in`() =
        runTest {
            every { auth.currentUser } returns null

            val result = repository.suspendUser("user-uid", "Razlog")

            assertTrue(result is Result.Error)
            assertEquals("Administrator nije prijavljen.", (result as Result.Error).exception.message)
        }

    @Test
    fun `suspendUser returns error when user not found`() =
        runTest {
            val mockAdmin = mockk<FirebaseUser>()
            every { mockAdmin.uid } returns "admin-uid"
            every { auth.currentUser } returns mockAdmin

            val mockSnapshot = mockk<DocumentSnapshot>()
            val mockTask = mockk<Task<DocumentSnapshot>>()
            every { mockDocument.get() } returns mockTask
            coEvery { mockTask.await() } returns mockSnapshot
            every { mockSnapshot.exists() } returns false

            val result = repository.suspendUser("nonexistent", "Razlog")

            assertTrue(result is Result.Error)
            assertEquals("User not found.", (result as Result.Error).exception.message)
        }

    @Test
    fun `suspendUser returns error when user already suspended`() =
        runTest {
            val mockAdmin = mockk<FirebaseUser>()
            every { mockAdmin.uid } returns "admin-uid"
            every { auth.currentUser } returns mockAdmin

            val suspendedUser = UserModel(name = "User", email = "user@test.com", suspended = true)
            val mockSnapshot = mockk<DocumentSnapshot>()
            val mockTask = mockk<Task<DocumentSnapshot>>()
            every { mockDocument.get() } returns mockTask
            coEvery { mockTask.await() } returns mockSnapshot
            every { mockSnapshot.exists() } returns true
            every { mockSnapshot.toObject(UserModel::class.java) } returns suspendedUser
            every { mockSnapshot.id } returns "user-uid"

            val result = repository.suspendUser("user-uid", "Razlog")

            assertTrue(result is Result.Error)
            assertEquals("Korisnik je već suspendiran.", (result as Result.Error).exception.message)
        }

    @Test
    fun `reactivateUser returns error when no admin logged in`() =
        runTest {
            every { auth.currentUser } returns null

            val result = repository.reactivateUser("user-uid")

            assertTrue(result is Result.Error)
            assertEquals("Administrator nije prijavljen.", (result as Result.Error).exception.message)
        }

    @Test
    fun `reactivateUser returns error when user not found`() =
        runTest {
            val mockAdmin = mockk<FirebaseUser>()
            every { mockAdmin.uid } returns "admin-uid"
            every { auth.currentUser } returns mockAdmin

            val mockSnapshot = mockk<DocumentSnapshot>()
            val mockTask = mockk<Task<DocumentSnapshot>>()
            every { mockDocument.get() } returns mockTask
            coEvery { mockTask.await() } returns mockSnapshot
            every { mockSnapshot.exists() } returns false

            val result = repository.reactivateUser("nonexistent")

            assertTrue(result is Result.Error)
            assertEquals("User not found.", (result as Result.Error).exception.message)
        }

    @Test
    fun `reactivateUser returns error when user not suspended`() =
        runTest {
            val mockAdmin = mockk<FirebaseUser>()
            every { mockAdmin.uid } returns "admin-uid"
            every { auth.currentUser } returns mockAdmin

            val activeUser = UserModel(name = "User", email = "user@test.com", suspended = false)
            val mockSnapshot = mockk<DocumentSnapshot>()
            val mockTask = mockk<Task<DocumentSnapshot>>()
            every { mockDocument.get() } returns mockTask
            coEvery { mockTask.await() } returns mockSnapshot
            every { mockSnapshot.exists() } returns true
            every { mockSnapshot.toObject(UserModel::class.java) } returns activeUser
            every { mockSnapshot.id } returns "user-uid"

            val result = repository.reactivateUser("user-uid")

            assertTrue(result is Result.Error)
            assertEquals("Korisnik nije suspendiran.", (result as Result.Error).exception.message)
        }

    @Test
    fun `suspendUser writes to suspension_history subcollection`() =
        runTest {
            val mockAdmin = mockk<FirebaseUser>()
            every { mockAdmin.uid } returns "admin-uid"
            every { auth.currentUser } returns mockAdmin

            val activeUser = UserModel(name = "User", email = "user@test.com", suspended = false)
            val mockSnapshot = mockk<DocumentSnapshot>()
            val mockTask = mockk<Task<DocumentSnapshot>>()
            every { mockDocument.get() } returns mockTask
            coEvery { mockTask.await() } returns mockSnapshot
            every { mockSnapshot.exists() } returns true
            every { mockSnapshot.toObject(UserModel::class.java) } returns activeUser
            every { mockSnapshot.id } returns "user-uid"

            val mockBatch = mockk<WriteBatch>(relaxed = true)
            every { firestore.batch() } returns mockBatch

            val mockHistoryCollection = mockk<CollectionReference>(relaxed = true)
            val mockHistoryDoc = mockk<DocumentReference>(relaxed = true)
            every {
                firestore.collection("users").document("user-uid")
                    .collection("suspension_history")
            } returns mockHistoryCollection
            every { mockHistoryCollection.document() } returns mockHistoryDoc

            val mockBooksQuery = mockk<Query>(relaxed = true)
            val mockBooksSnapshot = mockk<QuerySnapshot>()
            every { firestore.collection("books") } returns mockk(relaxed = true) {
                every { whereEqualTo("userID", "user-uid") } returns mockBooksQuery
            }
            val mockBooksTask = mockk<Task<QuerySnapshot>>()
            every { mockBooksQuery.get() } returns mockBooksTask
            coEvery { mockBooksTask.await() } returns mockBooksSnapshot
            every { mockBooksSnapshot.documents } returns emptyList()

            val mockCommitTask = mockk<Task<Void>>()
            every { mockBatch.commit() } returns mockCommitTask
            coEvery { mockCommitTask.await() } returns null

            val result = repository.suspendUser("user-uid", "Kršenje pravila")

            assertTrue(result is Result.Success)
            val entrySlot = slot<SuspensionHistoryEntry>()
            verify { mockBatch.set(mockHistoryDoc, capture(entrySlot)) }
            assertEquals("USER_SUSPENDED", entrySlot.captured.action)
            assertEquals("admin-uid", entrySlot.captured.adminUserId)
            assertEquals("Kršenje pravila", entrySlot.captured.reason)
        }

    @Test
    fun `reactivateUser writes to suspension_history subcollection`() =
        runTest {
            val mockAdmin = mockk<FirebaseUser>()
            every { mockAdmin.uid } returns "admin-uid"
            every { auth.currentUser } returns mockAdmin

            val suspendedUser = UserModel(
                name = "User",
                email = "user@test.com",
                suspended = true,
                suspendedReason = "Kršenje pravila",
            )
            val mockSnapshot = mockk<DocumentSnapshot>()
            val mockTask = mockk<Task<DocumentSnapshot>>()
            every { mockDocument.get() } returns mockTask
            coEvery { mockTask.await() } returns mockSnapshot
            every { mockSnapshot.exists() } returns true
            every { mockSnapshot.toObject(UserModel::class.java) } returns suspendedUser
            every { mockSnapshot.id } returns "user-uid"

            val mockBatch = mockk<WriteBatch>(relaxed = true)
            every { firestore.batch() } returns mockBatch

            val mockHistoryCollection = mockk<CollectionReference>(relaxed = true)
            val mockHistoryDoc = mockk<DocumentReference>(relaxed = true)
            every {
                firestore.collection("users").document("user-uid")
                    .collection("suspension_history")
            } returns mockHistoryCollection
            every { mockHistoryCollection.document() } returns mockHistoryDoc

            val mockBooksQuery = mockk<Query>(relaxed = true)
            val mockBooksSnapshot = mockk<QuerySnapshot>()
            every { firestore.collection("books") } returns mockk(relaxed = true) {
                every { whereEqualTo("userID", "user-uid") } returns mockBooksQuery
            }
            val mockBooksTask = mockk<Task<QuerySnapshot>>()
            every { mockBooksQuery.get() } returns mockBooksTask
            coEvery { mockBooksTask.await() } returns mockBooksSnapshot
            every { mockBooksSnapshot.documents } returns emptyList()

            val mockCommitTask = mockk<Task<Void>>()
            every { mockBatch.commit() } returns mockCommitTask
            coEvery { mockCommitTask.await() } returns null

            val result = repository.reactivateUser("user-uid")

            assertTrue(result is Result.Success)
            val entrySlot = slot<SuspensionHistoryEntry>()
            verify { mockBatch.set(mockHistoryDoc, capture(entrySlot)) }
            assertEquals("USER_REACTIVATED", entrySlot.captured.action)
            assertEquals("admin-uid", entrySlot.captured.adminUserId)
        }

    @Test
    fun `suspension captures previousState correctly`() =
        runTest {
            val mockAdmin = mockk<FirebaseUser>()
            every { mockAdmin.uid } returns "admin-uid"
            every { auth.currentUser } returns mockAdmin

            val activeUser = UserModel(
                name = "User",
                email = "user@test.com",
                suspended = false,
                suspendedAt = null,
                suspendedReason = null,
            )
            val mockSnapshot = mockk<DocumentSnapshot>()
            val mockTask = mockk<Task<DocumentSnapshot>>()
            every { mockDocument.get() } returns mockTask
            coEvery { mockTask.await() } returns mockSnapshot
            every { mockSnapshot.exists() } returns true
            every { mockSnapshot.toObject(UserModel::class.java) } returns activeUser
            every { mockSnapshot.id } returns "user-uid"

            val mockBatch = mockk<WriteBatch>(relaxed = true)
            every { firestore.batch() } returns mockBatch

            val mockHistoryCollection = mockk<CollectionReference>(relaxed = true)
            val mockHistoryDoc = mockk<DocumentReference>(relaxed = true)
            every {
                firestore.collection("users").document("user-uid")
                    .collection("suspension_history")
            } returns mockHistoryCollection
            every { mockHistoryCollection.document() } returns mockHistoryDoc

            val mockBooksQuery = mockk<Query>(relaxed = true)
            val mockBooksSnapshot = mockk<QuerySnapshot>()
            every { firestore.collection("books") } returns mockk(relaxed = true) {
                every { whereEqualTo("userID", "user-uid") } returns mockBooksQuery
            }
            val mockBooksTask = mockk<Task<QuerySnapshot>>()
            every { mockBooksQuery.get() } returns mockBooksTask
            coEvery { mockBooksTask.await() } returns mockBooksSnapshot
            every { mockBooksSnapshot.documents } returns emptyList()

            val mockCommitTask = mockk<Task<Void>>()
            every { mockBatch.commit() } returns mockCommitTask
            coEvery { mockCommitTask.await() } returns null

            repository.suspendUser("user-uid", "Spam")

            val entrySlot = slot<SuspensionHistoryEntry>()
            verify { mockBatch.set(mockHistoryDoc, capture(entrySlot)) }
            val prev = entrySlot.captured.previousState
            assertEquals(false, prev["suspended"])
            assertEquals(null, prev["suspendedAt"])
            assertEquals(null, prev["suspendedReason"])
        }
}
