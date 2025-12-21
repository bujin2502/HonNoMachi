package hr.foi.air.honnomachi

import android.os.Bundle
import com.google.android.gms.tasks.Task
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import hr.foi.air.honnomachi.viewmodel.AuthViewModel
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GoogleSignInUnitTest {
    private lateinit var authViewModel: AuthViewModel
    private val mockAuth: FirebaseAuth = mockk()
    private val mockFirestore: FirebaseFirestore = mockk()
    private val mockAnalytics: FirebaseAnalytics = mockk(relaxed = true)

    private val mockAuthResult: AuthResult = mockk()
    private val mockFirebaseUser: FirebaseUser = mockk()
    private val mockDocumentReference: DocumentReference = mockk()
    private val mockCollectionReference: CollectionReference = mockk()
    private val mockDocumentSnapshot: DocumentSnapshot = mockk()
    private val mockCredential: AuthCredential = mockk()

    @Before
    fun setup() {
        mockkStatic(GoogleAuthProvider::class)
        every { GoogleAuthProvider.getCredential(any(), any()) } returns mockCredential

        // Mock Android Bundle class
        mockkConstructor(Bundle::class)
        every { constructedWith<Bundle>().putString(any(), any()) } just Runs

        authViewModel = AuthViewModel(mockAuth, mockFirestore, mockAnalytics)

        every { mockAuthResult.user } returns mockFirebaseUser
        every { mockFirebaseUser.uid } returns "test_uid"
        every { mockFirebaseUser.displayName } returns "Test User"
        every { mockFirebaseUser.email } returns "test@example.com"
        every { mockFirebaseUser.isEmailVerified } returns true

        every { mockFirestore.collection("users") } returns mockCollectionReference
        every { mockCollectionReference.document("test_uid") } returns mockDocumentReference

        every { mockAnalytics.setUserId(any()) } just Runs
        every { mockAnalytics.logEvent(any(), any()) } just Runs
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `loginWithGoogle success for new user`() {
        val idToken = "test_id_token"
        var successResult = false
        var errorMessage: String? = "initial"

        val mockAuthTask: Task<AuthResult> = mockk()
        every { mockAuth.signInWithCredential(mockCredential) } returns mockAuthTask
        every { mockAuthTask.isSuccessful } returns true
        every { mockAuthTask.result } returns mockAuthResult
        every { mockAuthTask.addOnCompleteListener(any()) } answers {
            firstArg<com.google.android.gms.tasks.OnCompleteListener<AuthResult>>().onComplete(mockAuthTask)
            mockAuthTask
        }

        val mockSnapshotTask: Task<DocumentSnapshot> = mockk()
        every { mockDocumentReference.get() } returns mockSnapshotTask
        every { mockDocumentSnapshot.exists() } returns false
        every { mockSnapshotTask.addOnSuccessListener(any()) } answers {
            firstArg<com.google.android.gms.tasks.OnSuccessListener<DocumentSnapshot>>().onSuccess(mockDocumentSnapshot)
            mockSnapshotTask
        }
        every { mockSnapshotTask.addOnFailureListener(any()) } returns mockSnapshotTask

        val mockSetTask: Task<Void> = mockk()
        every { mockDocumentReference.set(any()) } returns mockSetTask
        every { mockSetTask.addOnCompleteListener(any()) } answers {
            firstArg<com.google.android.gms.tasks.OnCompleteListener<Void>>().onComplete(mockSetTask)
            mockSetTask
        }

        authViewModel.loginWithGoogle(idToken) { success, message ->
            successResult = success
            errorMessage = message
        }

        assertTrue(successResult)
        assertNull(errorMessage)
        verify { mockDocumentReference.set(any()) }
        verify { mockAnalytics.logEvent("login", any()) }
    }

    @Test
    fun `loginWithGoogle success for existing user`() {
        val idToken = "test_id_token"
        var successResult = false
        var errorMessage: String? = "initial"

        val mockAuthTask: Task<AuthResult> = mockk()
        every { mockAuth.signInWithCredential(mockCredential) } returns mockAuthTask
        every { mockAuthTask.isSuccessful } returns true
        every { mockAuthTask.result } returns mockAuthResult
        every { mockAuthTask.addOnCompleteListener(any()) } answers {
            firstArg<com.google.android.gms.tasks.OnCompleteListener<AuthResult>>().onComplete(mockAuthTask)
            mockAuthTask
        }

        val mockSnapshotTask: Task<DocumentSnapshot> = mockk()
        every { mockDocumentReference.get() } returns mockSnapshotTask
        every { mockDocumentSnapshot.exists() } returns true
        every { mockSnapshotTask.addOnSuccessListener(any()) } answers {
            firstArg<com.google.android.gms.tasks.OnSuccessListener<DocumentSnapshot>>().onSuccess(mockDocumentSnapshot)
            mockSnapshotTask
        }
        every { mockSnapshotTask.addOnFailureListener(any()) } returns mockSnapshotTask

        val mockUpdateTask: Task<Void> = mockk()
        every { mockDocumentReference.update("isVerified", true) } returns mockUpdateTask
        every { mockUpdateTask.addOnCompleteListener(any()) } answers {
            firstArg<com.google.android.gms.tasks.OnCompleteListener<Void>>().onComplete(mockUpdateTask)
            mockUpdateTask
        }

        authViewModel.loginWithGoogle(idToken) { success, message ->
            successResult = success
            errorMessage = message
        }

        assertTrue(successResult)
        assertNull(errorMessage)
        verify { mockDocumentReference.update("isVerified", true) }
        verify(exactly = 0) { mockDocumentReference.set(any()) }
        verify { mockAnalytics.logEvent("login", any()) }
    }

    @Test
    fun `loginWithGoogle failure from firebase auth`() {
        val idToken = "test_id_token"
        val expectedException = Exception("Firebase auth failed")
        var successResult = true
        var errorMessage: String? = null

        val mockAuthTask: Task<AuthResult> = mockk()
        every { mockAuth.signInWithCredential(mockCredential) } returns mockAuthTask
        every { mockAuthTask.isSuccessful } returns false
        every { mockAuthTask.exception } returns expectedException
        every { mockAuthTask.addOnCompleteListener(any()) } answers {
            firstArg<com.google.android.gms.tasks.OnCompleteListener<AuthResult>>().onComplete(mockAuthTask)
            mockAuthTask
        }

        authViewModel.loginWithGoogle(idToken) { success, message ->
            successResult = success
            errorMessage = message
        }

        assertFalse(successResult)
        assertEquals(expectedException.localizedMessage, errorMessage)
        verify(exactly = 0) { mockDocumentReference.get() }
        verify { mockAnalytics.logEvent("login_failed", any()) }
    }

    @Test
    fun `loginWithGoogle with null auth service`() {
        val idToken = "test_id_token"
        val nullAuthViewModel = AuthViewModel(null, mockFirestore, mockAnalytics)
        var successResult = true
        var errorMessage: String? = null

        nullAuthViewModel.loginWithGoogle(idToken) { success, message ->
            successResult = success
            errorMessage = message
        }

        assertFalse(successResult)
        assertEquals("Authentication service is not available.", errorMessage)
    }
}
