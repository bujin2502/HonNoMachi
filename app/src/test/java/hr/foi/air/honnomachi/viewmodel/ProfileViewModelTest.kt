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
import org.junit.Assert.assertFalse
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

            val viewModel = ProfileViewModel(profileRepository)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state is ProfileUiState.Success)
            assertEquals(userModel, (state as ProfileUiState.Success).user)

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

            viewModel.onNameChange("New Name")
            viewModel.onPhoneChange("0912345678")
            viewModel.onStreetChange("New Street 1")
            viewModel.onCityChange("New City")
            viewModel.onZipChange("54321")

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

            var successCalled = false
            viewModel.saveProfile { success, _ -> successCalled = success }
            advanceUntilIdle()

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
            val initialUser = UserModel(uid = "test-uid")
            coEvery { profileRepository.getUserProfile() } returns
                hr.foi.air.honnomachi.util.Result
                    .Success(initialUser)

            val viewModel = ProfileViewModel(profileRepository)
            advanceUntilIdle()

            viewModel.onNameChange("")

            var successCalled = true
            var errorMessage: String? = null

            viewModel.saveProfile { success, msg ->
                successCalled = success
                errorMessage = msg
            }
            advanceUntilIdle()

            assertEquals(false, successCalled)
            assertEquals("Molimo ispravno popunite sva polja.", errorMessage)

            val form = viewModel.formState.value
            assertEquals(hr.foi.air.honnomachi.ValidationErrorType.EMPTY_NAME, form.nameError)
        }

    @Test
    fun `loadUserProfile error sets error uiState`() =
        runTest(testDispatcher) {
            coEvery { profileRepository.getUserProfile() } returns
                hr.foi.air.honnomachi.util.Result
                    .Error(Exception("Network error"))

            val viewModel = ProfileViewModel(profileRepository)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state is ProfileUiState.Error)
            assertTrue((state as ProfileUiState.Error).message.contains("Network error"))
        }

    @Test
    fun `saveProfile repository error calls onResult with false`() =
        runTest(testDispatcher) {
            val initialUser =
                UserModel(
                    name = "Test",
                    email = "test@example.com",
                    uid = "test-uid",
                    phoneNumber = "0911111111",
                    street = "Street 1",
                    city = "City",
                    postNumber = "10000",
                )
            coEvery { profileRepository.getUserProfile() } returns
                hr.foi.air.honnomachi.util.Result.Success(initialUser)

            val viewModel = ProfileViewModel(profileRepository)
            advanceUntilIdle()

            coEvery { profileRepository.updateUserProfile(any(), any(), any(), any(), any()) } returns
                hr.foi.air.honnomachi.util.Result.Error(Exception("Save failed"))

            var successCalled = true
            var errorMessage: String? = null
            viewModel.saveProfile { success, msg ->
                successCalled = success
                errorMessage = msg
            }
            advanceUntilIdle()

            assertFalse(successCalled)
            assertEquals("Save failed", errorMessage)
        }

    @Test
    fun `onAnalyticsToggled success updates user in uiState`() =
        runTest(testDispatcher) {
            val user = UserModel(uid = "test-uid", analyticsEnabled = false)
            coEvery { profileRepository.getUserProfile() } returns
                hr.foi.air.honnomachi.util.Result.Success(user)
            coEvery { profileRepository.updateAnalyticsSetting(true) } returns
                hr.foi.air.honnomachi.util.Result.Success(Unit)

            val viewModel = ProfileViewModel(profileRepository)
            advanceUntilIdle()

            viewModel.onAnalyticsToggled(true)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state is ProfileUiState.Success)
            assertTrue((state as ProfileUiState.Success).user.analyticsEnabled)
        }

    @Test
    fun `onAnalyticsToggled error reverts form state`() =
        runTest(testDispatcher) {
            val user = UserModel(uid = "test-uid", analyticsEnabled = false)
            coEvery { profileRepository.getUserProfile() } returns
                hr.foi.air.honnomachi.util.Result.Success(user)
            coEvery { profileRepository.updateAnalyticsSetting(true) } returns
                hr.foi.air.honnomachi.util.Result.Error(Exception("Update failed"))

            val viewModel = ProfileViewModel(profileRepository)
            advanceUntilIdle()

            viewModel.onAnalyticsToggled(true)
            advanceUntilIdle()

            assertFalse(viewModel.formState.value.analyticsEnabled)
        }

    @Test
    fun `changePassword validation failure sets errors in changePasswordState`() =
        runTest(testDispatcher) {
            val user = UserModel(uid = "test-uid")
            coEvery { profileRepository.getUserProfile() } returns
                hr.foi.air.honnomachi.util.Result.Success(user)

            val viewModel = ProfileViewModel(profileRepository)
            advanceUntilIdle()

            viewModel.onOldPasswordChange("")
            viewModel.onNewPasswordChange("")
            viewModel.onConfirmPasswordChange("")

            var resultCalled = false
            viewModel.changePassword { _, _ -> resultCalled = true }
            advanceUntilIdle()

            assertFalse(resultCalled)
            val pwState = viewModel.changePasswordState.value
            assertTrue(
                pwState.oldPasswordError != null ||
                    pwState.newPasswordError != null ||
                    pwState.confirmPasswordError != null,
            )
        }

    @Test
    fun `changePassword success calls onResult true and resets form`() =
        runTest(testDispatcher) {
            val user = UserModel(uid = "test-uid")
            coEvery { profileRepository.getUserProfile() } returns
                hr.foi.air.honnomachi.util.Result.Success(user)
            coEvery { profileRepository.changePassword(any(), any()) } returns
                hr.foi.air.honnomachi.util.Result.Success(Unit)

            val viewModel = ProfileViewModel(profileRepository)
            advanceUntilIdle()

            viewModel.onOldPasswordChange("OldPass1!")
            viewModel.onNewPasswordChange("NewPass1!")
            viewModel.onConfirmPasswordChange("NewPass1!")

            var successCalled = false
            viewModel.changePassword { success, _ -> successCalled = success }
            advanceUntilIdle()

            assertTrue(successCalled)
            assertEquals("", viewModel.changePasswordState.value.oldPassword)
        }

    @Test
    fun `changePassword error calls onResult false with message`() =
        runTest(testDispatcher) {
            val user = UserModel(uid = "test-uid")
            coEvery { profileRepository.getUserProfile() } returns
                hr.foi.air.honnomachi.util.Result.Success(user)
            coEvery { profileRepository.changePassword(any(), any()) } returns
                hr.foi.air.honnomachi.util.Result.Error(Exception("Wrong password"))

            val viewModel = ProfileViewModel(profileRepository)
            advanceUntilIdle()

            viewModel.onOldPasswordChange("OldPass1!")
            viewModel.onNewPasswordChange("NewPass1!")
            viewModel.onConfirmPasswordChange("NewPass1!")

            var successCalled = true
            var errorMessage: String? = null
            viewModel.changePassword { success, msg ->
                successCalled = success
                errorMessage = msg
            }
            advanceUntilIdle()

            assertFalse(successCalled)
            assertEquals("Wrong password", errorMessage)
        }
}
