package hr.foi.air.honnomachi.viewmodel

import androidx.lifecycle.SavedStateHandle
import hr.foi.air.honnomachi.model.UserModel
import hr.foi.air.honnomachi.ui.admin.AdminUserDetailUiState
import hr.foi.air.honnomachi.ui.admin.AdminUserDetailViewModel
import hr.foi.air.honnomachi.util.Result
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class UserDetailViewModelTest {
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is Loading`() =
        runTest(testDispatcher) {
            val fakeRepo = FakeAdminRepository()
            val savedStateHandle = SavedStateHandle(mapOf("userId" to "test-uid"))

            val viewModel = AdminUserDetailViewModel(savedStateHandle, fakeRepo)

            assertTrue(viewModel.uiState.value is AdminUserDetailUiState.Loading)
        }

    @Test
    fun `loadUser success sets Success state with user data`() =
        runTest(testDispatcher) {
            val expectedUser =
                UserModel(
                    uid = "test-uid",
                    name = "Test User",
                    email = "test@test.com",
                    admin = false,
                    suspended = false,
                    phoneNumber = "+385123456",
                    city = "Zagreb",
                )
            val fakeRepo = FakeAdminRepository()
            fakeRepo.userByIdResult = Result.Success(expectedUser)

            val savedStateHandle = SavedStateHandle(mapOf("userId" to "test-uid"))
            val viewModel = AdminUserDetailViewModel(savedStateHandle, fakeRepo)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state is AdminUserDetailUiState.Success)
            assertEquals("Test User", (state as AdminUserDetailUiState.Success).user.name)
            assertEquals("test@test.com", state.user.email)
        }

    @Test
    fun `loadUser error sets Error state`() =
        runTest(testDispatcher) {
            val fakeRepo = FakeAdminRepository()
            fakeRepo.userByIdResult = Result.Error(Exception("User not found."))

            val savedStateHandle = SavedStateHandle(mapOf("userId" to "nonexistent"))
            val viewModel = AdminUserDetailViewModel(savedStateHandle, fakeRepo)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state is AdminUserDetailUiState.Error)
            assertEquals("User not found.", (state as AdminUserDetailUiState.Error).message)
        }

    @Test
    fun `loadUser retry reloads data successfully`() =
        runTest(testDispatcher) {
            val fakeRepo = FakeAdminRepository()
            fakeRepo.userByIdResult = Result.Error(Exception("Network error"))

            val savedStateHandle = SavedStateHandle(mapOf("userId" to "test-uid"))
            val viewModel = AdminUserDetailViewModel(savedStateHandle, fakeRepo)
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value is AdminUserDetailUiState.Error)

            val expectedUser = UserModel(uid = "test-uid", name = "Recovered User", email = "ok@test.com")
            fakeRepo.userByIdResult = Result.Success(expectedUser)

            viewModel.loadUser()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state is AdminUserDetailUiState.Success)
            assertEquals("Recovered User", (state as AdminUserDetailUiState.Success).user.name)
        }

    @Test
    fun `onSuspendClick opens suspend dialog`() =
        runTest(testDispatcher) {
            val fakeRepo = FakeAdminRepository()
            fakeRepo.userByIdResult = Result.Success(UserModel(uid = "test-uid", name = "User"))

            val viewModel = AdminUserDetailViewModel(SavedStateHandle(mapOf("userId" to "test-uid")), fakeRepo)
            advanceUntilIdle()

            viewModel.onSuspendClick()

            val state = viewModel.uiState.value as AdminUserDetailUiState.Success
            assertTrue(state.showSuspendDialog)
            assertFalse(state.showReactivateDialog)
        }

    @Test
    fun `onReactivateClick opens reactivate dialog`() =
        runTest(testDispatcher) {
            val fakeRepo = FakeAdminRepository()
            fakeRepo.userByIdResult = Result.Success(UserModel(uid = "test-uid", name = "User"))

            val viewModel = AdminUserDetailViewModel(SavedStateHandle(mapOf("userId" to "test-uid")), fakeRepo)
            advanceUntilIdle()

            viewModel.onReactivateClick()

            val state = viewModel.uiState.value as AdminUserDetailUiState.Success
            assertTrue(state.showReactivateDialog)
            assertFalse(state.showSuspendDialog)
        }

    @Test
    fun `dismissDialog closes all dialogs`() =
        runTest(testDispatcher) {
            val fakeRepo = FakeAdminRepository()
            fakeRepo.userByIdResult = Result.Success(UserModel(uid = "test-uid", name = "User"))

            val viewModel = AdminUserDetailViewModel(SavedStateHandle(mapOf("userId" to "test-uid")), fakeRepo)
            advanceUntilIdle()

            viewModel.onSuspendClick()
            assertTrue((viewModel.uiState.value as AdminUserDetailUiState.Success).showSuspendDialog)

            viewModel.dismissDialog()

            val state = viewModel.uiState.value as AdminUserDetailUiState.Success
            assertFalse(state.showSuspendDialog)
            assertFalse(state.showReactivateDialog)
        }

    @Test
    fun `confirmSuspend success refreshes user with message`() =
        runTest(testDispatcher) {
            val fakeRepo = FakeAdminRepository()
            fakeRepo.userByIdResult = Result.Success(UserModel(uid = "test-uid", name = "User"))
            fakeRepo.suspendUserResult = Result.Success(Unit)

            val viewModel = AdminUserDetailViewModel(SavedStateHandle(mapOf("userId" to "test-uid")), fakeRepo)
            advanceUntilIdle()

            viewModel.confirmSuspend("Razlog", "Uspjeh", "Greška")
            advanceUntilIdle()

            val state = viewModel.uiState.value as AdminUserDetailUiState.Success
            assertEquals("Uspjeh", state.actionMessage)
            assertFalse(state.showSuspendDialog)
        }

    @Test
    fun `confirmSuspend error shows error message`() =
        runTest(testDispatcher) {
            val fakeRepo = FakeAdminRepository()
            fakeRepo.userByIdResult = Result.Success(UserModel(uid = "test-uid", name = "User"))
            fakeRepo.suspendUserResult = Result.Error(Exception("Već suspendiran"))

            val viewModel = AdminUserDetailViewModel(SavedStateHandle(mapOf("userId" to "test-uid")), fakeRepo)
            advanceUntilIdle()

            viewModel.confirmSuspend("Razlog", "Uspjeh", "Greška:")
            advanceUntilIdle()

            val state = viewModel.uiState.value as AdminUserDetailUiState.Success
            assertEquals("Greška: Već suspendiran", state.actionMessage)
            assertFalse(state.isActionLoading)
        }

    @Test
    fun `confirmReactivate success refreshes user with message`() =
        runTest(testDispatcher) {
            val fakeRepo = FakeAdminRepository()
            fakeRepo.userByIdResult = Result.Success(UserModel(uid = "test-uid", name = "User", suspended = true))
            fakeRepo.reactivateUserResult = Result.Success(Unit)

            val viewModel = AdminUserDetailViewModel(SavedStateHandle(mapOf("userId" to "test-uid")), fakeRepo)
            advanceUntilIdle()

            viewModel.confirmReactivate("Reaktivirano", "Greška")
            advanceUntilIdle()

            val state = viewModel.uiState.value as AdminUserDetailUiState.Success
            assertEquals("Reaktivirano", state.actionMessage)
            assertFalse(state.showReactivateDialog)
        }

    @Test
    fun `confirmReactivate error shows error message`() =
        runTest(testDispatcher) {
            val fakeRepo = FakeAdminRepository()
            fakeRepo.userByIdResult = Result.Success(UserModel(uid = "test-uid", name = "User", suspended = true))
            fakeRepo.reactivateUserResult = Result.Error(Exception("Nije suspendiran"))

            val viewModel = AdminUserDetailViewModel(SavedStateHandle(mapOf("userId" to "test-uid")), fakeRepo)
            advanceUntilIdle()

            viewModel.confirmReactivate("Reaktivirano", "Greška:")
            advanceUntilIdle()

            val state = viewModel.uiState.value as AdminUserDetailUiState.Success
            assertEquals("Greška: Nije suspendiran", state.actionMessage)
            assertFalse(state.isActionLoading)
        }

    @Test
    fun `consumeActionMessage clears message`() =
        runTest(testDispatcher) {
            val fakeRepo = FakeAdminRepository()
            fakeRepo.userByIdResult = Result.Success(UserModel(uid = "test-uid", name = "User"))
            fakeRepo.suspendUserResult = Result.Success(Unit)

            val viewModel = AdminUserDetailViewModel(SavedStateHandle(mapOf("userId" to "test-uid")), fakeRepo)
            advanceUntilIdle()

            viewModel.confirmSuspend("Razlog", "Uspjeh", "Greška")
            advanceUntilIdle()

            assertEquals("Uspjeh", (viewModel.uiState.value as AdminUserDetailUiState.Success).actionMessage)

            viewModel.consumeActionMessage()

            assertNull((viewModel.uiState.value as AdminUserDetailUiState.Success).actionMessage)
        }
}
