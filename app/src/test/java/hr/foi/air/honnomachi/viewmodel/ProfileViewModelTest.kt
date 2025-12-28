package hr.foi.air.honnomachi.viewmodel

import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import hr.foi.air.honnomachi.model.UserModel
import hr.foi.air.honnomachi.ui.profile.ProfileUiState
import hr.foi.air.honnomachi.ui.profile.ProfileViewModel
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class ProfileViewModelTest {
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var user: FirebaseUser
    private lateinit var collectionRef: CollectionReference
    private lateinit var docRef: DocumentReference
    private lateinit var docSnapshot: DocumentSnapshot
    private lateinit var getTask: Task<DocumentSnapshot>
    private lateinit var updateTask: Task<Void>

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        // Mock static for await() which is an extension function
        mockkStatic("kotlinx.coroutines.tasks.TasksKt")

        auth = mockk()
        firestore = mockk()
        user = mockk()
        collectionRef = mockk()
        docRef = mockk()
        docSnapshot = mockk()
        getTask = mockk()
        updateTask = mockk()

        // Common Mocks setup
        every { auth.currentUser } returns user
        every { user.uid } returns "test-uid"
        every { user.email } returns "test@example.com"

        every { firestore.collection("users") } returns collectionRef
        every { collectionRef.document("test-uid") } returns docRef
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `loadUserProfile success updates uiState`() =
        runTest(testDispatcher) {
            // Mock Firestore get
            every { docRef.get() } returns getTask
            coEvery { getTask.await() } returns docSnapshot
            every { docSnapshot.exists() } returns true

            val userModel =
                UserModel(
                    name = "Test User",
                    email = "test@example.com",
                    uid = "test-uid",
                    phoneNumber = "1234567890",
                    street = "Main St 1",
                    city = "Test City",
                    postNumber = "12345",
                )
            every { docSnapshot.toObject(UserModel::class.java) } returns userModel

            // Init ViewModel (triggers loadUserProfile)
            val viewModel = ProfileViewModel(auth, firestore)
            advanceUntilIdle()

            // Verify Success State
            val state = viewModel.uiState.value
            assertTrue(state is ProfileUiState.Success)
            assertEquals(userModel, (state as ProfileUiState.Success).user)

            // Verify Form State Initialized
            val formState = viewModel.formState.value
            assertEquals("Test User", formState.name)
            assertEquals("1234567890", formState.phone)
            assertEquals("Main St 1", formState.street)
            assertEquals("Test City", formState.city)
            assertEquals("12345", formState.zip)
        }

    @Test
    fun `saveProfile success updates uiState`() =
        runTest(testDispatcher) {
            // --- Setup Load ---
            every { docRef.get() } returns getTask
            coEvery { getTask.await() } returns docSnapshot
            every { docSnapshot.exists() } returns true
            val initialUser =
                UserModel(
                    name = "Old Name",
                    email = "test@example.com",
                    uid = "test-uid",
                )
            every { docSnapshot.toObject(UserModel::class.java) } returns initialUser

            val viewModel = ProfileViewModel(auth, firestore)
            advanceUntilIdle()

            // --- Perform Change ---
            viewModel.onNameChange("New Name")
            viewModel.onPhoneChange("0912345678")
            viewModel.onStreetChange("New Street 1")
            viewModel.onCityChange("New City")
            viewModel.onZipChange("54321")

            // --- Mock Save ---
            every { docRef.update(any<Map<String, Any>>()) } returns updateTask
            coEvery { updateTask.await() } returns mockk()

            // Execute Save
            var successCalled = false
            viewModel.saveProfile { success, _ -> successCalled = success }
            advanceUntilIdle()

            // Verify
            assertTrue(successCalled)
            val state = viewModel.uiState.value
            assertTrue(state is ProfileUiState.Success)
            val updatedUser = (state as ProfileUiState.Success).user
            assertEquals("New Name", updatedUser.name)
            assertEquals("New Street 1", updatedUser.street)
            assertEquals("54321", updatedUser.postNumber)
        }

    @Test
    fun `saveProfile fails with validation error`() =
        runTest(testDispatcher) {
            // --- Setup Load ---
            every { docRef.get() } returns getTask
            coEvery { getTask.await() } returns docSnapshot
            every { docSnapshot.exists() } returns true
            val initialUser = UserModel(uid = "test-uid")
            every { docSnapshot.toObject(UserModel::class.java) } returns initialUser

            val viewModel = ProfileViewModel(auth, firestore)
            advanceUntilIdle()

            // --- Invalid Data ---
            viewModel.onNameChange("") // Empty Name

            var successCalled = true
            var errorMessage: String? = null

            viewModel.saveProfile { success, msg ->
                successCalled = success
                errorMessage = msg
            }
            advanceUntilIdle()

            // Verify failure
            assertEquals(false, successCalled)
            assertEquals("Molimo ispravno popunite sva polja.", errorMessage)

            // Verify Form State has error
            val form = viewModel.formState.value
            assertEquals(hr.foi.air.honnomachi.ValidationErrorType.EMPTY_NAME, form.nameError)
        }
}
