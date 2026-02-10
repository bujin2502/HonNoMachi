package hr.foi.air.honnomachi.data

import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.DocumentSnapshot
import hr.foi.air.honnomachi.model.UserModel
import hr.foi.air.honnomachi.util.Result
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkStatic
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AuthRepositoryTest {
    private lateinit var auth: FirebaseAuth
    private lateinit var userDataSource: FirestoreUserDataSource
    private lateinit var crashlytics: FirebaseCrashlytics
    private lateinit var repository: AuthRepositoryImpl

    @Before
    fun setup() {
        auth = mockk(relaxed = true)
        userDataSource = mockk(relaxed = true)
        crashlytics = mockk(relaxed = true)
        repository = AuthRepositoryImpl(auth, userDataSource, crashlytics)

        mockkStatic("kotlinx.coroutines.tasks.TasksKt")
    }

    @After
    fun tearDown() {
        unmockkStatic("kotlinx.coroutines.tasks.TasksKt")
    }

    // --- currentUser ---

    @Test
    fun `currentUser returns null when not logged in`() {
        every { auth.currentUser } returns null

        val result = repository.currentUser

        assertNull(result)
    }

    @Test
    fun `currentUser returns UserModel when logged in`() {
        val mockFirebaseUser = mockk<FirebaseUser>()
        every { mockFirebaseUser.uid } returns "uid123"
        every { mockFirebaseUser.email } returns "test@example.com"
        every { mockFirebaseUser.isEmailVerified } returns true
        every { auth.currentUser } returns mockFirebaseUser

        val result = repository.currentUser

        assertEquals("uid123", result?.uid)
        assertEquals("test@example.com", result?.email)
        assertTrue(result?.isVerified == true)
    }

    // --- signOut ---

    @Test
    fun `signOut returns success`() =
        runTest {
            every { auth.signOut() } just runs

            val result = repository.signOut()

            assertTrue(result is Result.Success)
        }

    @Test
    fun `signOut returns error on exception`() =
        runTest {
            every { auth.signOut() } throws RuntimeException("Sign out failed")

            val result = repository.signOut()

            assertTrue(result is Result.Error)
            assertEquals("Sign out failed", (result as Result.Error).exception.message)
        }

    // --- sendPasswordResetEmail ---

    @Test
    fun `sendPasswordResetEmail success`() =
        runTest {
            val mockTask = mockk<Task<Void>>()
            every { auth.sendPasswordResetEmail("test@example.com") } returns mockTask
            coEvery { mockTask.await() } returns mockk()

            val result = repository.sendPasswordResetEmail("test@example.com")

            assertTrue(result is Result.Success)
        }

    @Test
    fun `sendPasswordResetEmail failure`() =
        runTest {
            val mockTask = mockk<Task<Void>>()
            every { auth.sendPasswordResetEmail("bad@example.com") } returns mockTask
            coEvery { mockTask.await() } throws Exception("User not found")

            val result = repository.sendPasswordResetEmail("bad@example.com")

            assertTrue(result is Result.Error)
            assertEquals("User not found", (result as Result.Error).exception.message)
        }

    // --- reloadUser ---

    @Test
    fun `reloadUser success`() =
        runTest {
            val mockUser = mockk<FirebaseUser>()
            val mockTask = mockk<Task<Void>>()
            every { auth.currentUser } returns mockUser
            every { mockUser.reload() } returns mockTask
            coEvery { mockTask.await() } returns mockk()

            val result = repository.reloadUser()

            assertTrue(result is Result.Success)
        }

    @Test
    fun `reloadUser failure`() =
        runTest {
            val mockUser = mockk<FirebaseUser>()
            val mockTask = mockk<Task<Void>>()
            every { auth.currentUser } returns mockUser
            every { mockUser.uid } returns "uid123"
            every { mockUser.reload() } returns mockTask
            coEvery { mockTask.await() } throws Exception("Network error")

            val result = repository.reloadUser()

            assertTrue(result is Result.Error)
            assertEquals("Network error", (result as Result.Error).exception.message)
        }

    // --- checkSession ---

    @Test
    fun `checkSession no user returns error`() =
        runTest {
            every { auth.currentUser } returns null

            val result = repository.checkSession()

            assertTrue(result is Result.Error)
            assertEquals(
                "No user is currently logged in",
                (result as Result.Error).exception.message,
            )
        }

    // --- testSecureRead ---

    @Test
    fun `testSecureRead no user returns error`() =
        runTest {
            every { auth.currentUser } returns null

            val result = repository.testSecureRead()

            assertTrue(result is Result.Error)
            assertEquals("No authenticated user", (result as Result.Error).exception.message)
        }

    @Test
    fun `testSecureRead document exists returns success`() =
        runTest {
            val mockUser = mockk<FirebaseUser>()
            every { mockUser.uid } returns "uid123"
            every { auth.currentUser } returns mockUser

            val mockDoc = mockk<DocumentSnapshot>()
            every { mockDoc.exists() } returns true
            coEvery { userDataSource.getUserDocument("uid123") } returns mockDoc

            val result = repository.testSecureRead()

            assertTrue(result is Result.Success)
            assertEquals(
                "Secure read successful - token is valid",
                (result as Result.Success).data,
            )
        }

    @Test
    fun `testSecureRead document missing returns error`() =
        runTest {
            val mockUser = mockk<FirebaseUser>()
            every { mockUser.uid } returns "uid123"
            every { auth.currentUser } returns mockUser

            val mockDoc = mockk<DocumentSnapshot>()
            every { mockDoc.exists() } returns false
            coEvery { userDataSource.getUserDocument("uid123") } returns mockDoc

            val result = repository.testSecureRead()

            assertTrue(result is Result.Error)
            assertEquals("Document not found", (result as Result.Error).exception.message)
        }
}
