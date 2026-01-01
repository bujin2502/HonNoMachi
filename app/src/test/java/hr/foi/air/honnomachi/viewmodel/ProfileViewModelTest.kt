package hr.foi.air.honnomachi.viewmodel

import hr.foi.air.honnomachi.model.UserModel
import hr.foi.air.honnomachi.ui.profile.ProfileUiState
import hr.foi.air.honnomachi.ui.profile.ProfileViewModel
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

    private lateinit var profileRepository: hr.foi.air.honnomachi.data.ProfileRepository

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        profileRepository = mockk()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `loadUserProfile success updates uiState`() =
        runTest(testDispatcher) {
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
            coEvery { profileRepository.getUserProfile() } returns
                hr.foi.air.honnomachi.util.Result
                    .Success(userModel)

            // Init ViewModel (triggers loadUserProfile)
            val viewModel = ProfileViewModel(profileRepository)
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
            val initialUser =
                UserModel(
                    name = "Old Name",
                    email = "test@example.com",
                    uid = "test-uid",
                )
            coEvery { profileRepository.getUserProfile() } returns
                hr.foi.air.honnomachi.util.Result
                    .Success(initialUser)

            val viewModel = ProfileViewModel(profileRepository)
            advanceUntilIdle()

            // --- Perform Change ---
            viewModel.onNameChange("New Name")
            viewModel.onPhoneChange("0912345678")
            viewModel.onStreetChange("New Street 1")
            viewModel.onCityChange("New City")
            viewModel.onZipChange("54321")

            // --- Mock Save ---
            val updatedUser =
                initialUser.copy(
                    name = "New Name",
                    phoneNumber = "0912345678",
                    street = "New Street 1",
                    city = "New City",
                    postNumber = "54321",
                )
            coEvery { profileRepository.updateUserProfile(any(), any(), any(), any(), any()) } returns
                hr.foi.air.honnomachi.util.Result
                    .Success(updatedUser)

            // Execute Save
            var successCalled = false
            viewModel.saveProfile { success, _ -> successCalled = success }
            advanceUntilIdle()

            // Verify
            assertTrue(successCalled)
            val state = viewModel.uiState.value
            assertTrue(state is ProfileUiState.Success)
            val returnedUser = (state as ProfileUiState.Success).user
            assertEquals("New Name", returnedUser.name)
            assertEquals("New Street 1", returnedUser.street)
            assertEquals("54321", returnedUser.postNumber)
        }

    @Test
    fun `saveProfile fails with validation error`() =
        runTest(testDispatcher) {
            // --- Setup Load ---
            val initialUser = UserModel(uid = "test-uid")
            coEvery { profileRepository.getUserProfile() } returns
                hr.foi.air.honnomachi.util.Result
                    .Success(initialUser)

            val viewModel = ProfileViewModel(profileRepository)
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
