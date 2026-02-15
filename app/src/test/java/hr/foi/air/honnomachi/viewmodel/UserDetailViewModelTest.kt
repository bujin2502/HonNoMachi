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
}
