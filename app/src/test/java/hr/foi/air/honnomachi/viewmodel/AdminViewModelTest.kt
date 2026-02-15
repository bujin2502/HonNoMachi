package hr.foi.air.honnomachi.viewmodel

import com.google.firebase.firestore.DocumentSnapshot
import hr.foi.air.honnomachi.data.AdminRepository
import hr.foi.air.honnomachi.data.UserPage
import hr.foi.air.honnomachi.model.UserModel
import hr.foi.air.honnomachi.ui.admin.AdminUserListViewModel
import hr.foi.air.honnomachi.ui.admin.AdminViewModel
import hr.foi.air.honnomachi.ui.admin.UserFilter
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

class FakeAdminRepository : AdminRepository {
    var isAdminResult: Result<Boolean> = Result.Success(true)
    var allUsersResult: Result<UserPage> = Result.Success(UserPage(emptyList(), null))
    var userByIdResult: Result<UserModel> = Result.Success(UserModel())
    var searchResult: Result<List<UserModel>> = Result.Success(emptyList())
    var statusResult: Result<List<UserModel>> = Result.Success(emptyList())

    override suspend fun isCurrentUserAdmin(): Result<Boolean> = isAdminResult

    override suspend fun getAllUsers(
        pageSize: Int,
        lastDocSnapshot: DocumentSnapshot?,
    ): Result<UserPage> = allUsersResult

    override suspend fun getUserById(userId: String): Result<UserModel> = userByIdResult

    override suspend fun searchUsers(query: String): Result<List<UserModel>> = searchResult

    override suspend fun getUsersByStatus(isSuspended: Boolean): Result<List<UserModel>> = statusResult
}

@ExperimentalCoroutinesApi
class AdminViewModelTest {
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
    fun `admin user sets isAdminChecked to true`() =
        runTest(testDispatcher) {
            val fakeRepo = FakeAdminRepository()
            fakeRepo.isAdminResult = Result.Success(true)

            val viewModel = AdminViewModel(fakeRepo)
            advanceUntilIdle()

            assertEquals(true, viewModel.isAdminChecked.value)
        }

    @Test
    fun `non-admin user sets isAdminChecked to false`() =
        runTest(testDispatcher) {
            val fakeRepo = FakeAdminRepository()
            fakeRepo.isAdminResult = Result.Success(false)

            val viewModel = AdminViewModel(fakeRepo)
            advanceUntilIdle()

            assertEquals(false, viewModel.isAdminChecked.value)
        }

    @Test
    fun `error sets isAdminChecked to false`() =
        runTest(testDispatcher) {
            val fakeRepo = FakeAdminRepository()
            fakeRepo.isAdminResult = Result.Error(Exception("Network error"))

            val viewModel = AdminViewModel(fakeRepo)
            advanceUntilIdle()

            assertEquals(false, viewModel.isAdminChecked.value)
        }
}

@ExperimentalCoroutinesApi
class AdminUserListViewModelTest {
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
    fun `initial state is loading`() =
        runTest(testDispatcher) {
            val fakeRepo = FakeAdminRepository()
            val viewModel = AdminUserListViewModel(fakeRepo)

            assertTrue(viewModel.uiState.value.isLoading)
        }

    @Test
    fun `loadUsers success populates users`() =
        runTest(testDispatcher) {
            val testUsers =
                listOf(
                    UserModel(uid = "1", name = "Alice", email = "alice@test.com"),
                    UserModel(uid = "2", name = "Bob", email = "bob@test.com"),
                )
            val fakeRepo = FakeAdminRepository()
            fakeRepo.allUsersResult = Result.Success(UserPage(testUsers, null))

            val viewModel = AdminUserListViewModel(fakeRepo)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertEquals(2, state.users.size)
            assertEquals("Alice", state.users[0].name)
            assertEquals("Bob", state.users[1].name)
            assertNull(state.errorMessage)
        }

    @Test
    fun `loadUsers error sets errorMessage`() =
        runTest(testDispatcher) {
            val fakeRepo = FakeAdminRepository()
            fakeRepo.allUsersResult = Result.Error(Exception("Firestore error"))

            val viewModel = AdminUserListViewModel(fakeRepo)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertTrue(state.users.isEmpty())
            assertEquals("Firestore error", state.errorMessage)
        }

    @Test
    fun `loadUsers with empty list returns empty users`() =
        runTest(testDispatcher) {
            val fakeRepo = FakeAdminRepository()
            fakeRepo.allUsersResult = Result.Success(UserPage(emptyList(), null))

            val viewModel = AdminUserListViewModel(fakeRepo)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertTrue(state.users.isEmpty())
            assertNull(state.errorMessage)
        }

    @Test
    fun `onSearchQueryChanged updates query in uiState`() =
        runTest(testDispatcher) {
            val fakeRepo = FakeAdminRepository()
            fakeRepo.allUsersResult = Result.Success(UserPage(emptyList(), null))

            val viewModel = AdminUserListViewModel(fakeRepo)
            advanceUntilIdle()

            viewModel.onSearchQueryChanged("ivan")

            assertEquals("ivan", viewModel.uiState.value.searchQuery)
        }

    @Test
    fun `onFilterSelected updates selected filter`() =
        runTest(testDispatcher) {
            val fakeRepo = FakeAdminRepository()
            fakeRepo.allUsersResult = Result.Success(UserPage(emptyList(), null))
            fakeRepo.searchResult = Result.Success(emptyList())

            val viewModel = AdminUserListViewModel(fakeRepo)
            advanceUntilIdle()

            viewModel.onFilterSelected(UserFilter.SUSPENDED)
            advanceUntilIdle()

            assertEquals(UserFilter.SUSPENDED, viewModel.uiState.value.selectedFilter)
        }

    @Test
    fun `loadMoreUsers does nothing when search is active`() =
        runTest(testDispatcher) {
            val testUsers =
                listOf(
                    UserModel(uid = "1", name = "Alice", email = "alice@test.com"),
                )
            val fakeRepo = FakeAdminRepository()
            fakeRepo.allUsersResult = Result.Success(UserPage(testUsers, null))
            fakeRepo.searchResult = Result.Success(testUsers)

            val viewModel = AdminUserListViewModel(fakeRepo)
            advanceUntilIdle()

            viewModel.onSearchQueryChanged("alice")
            advanceUntilIdle()

            val usersBeforeLoadMore = viewModel.uiState.value.users.size
            viewModel.loadMoreUsers()
            advanceUntilIdle()

            assertEquals(usersBeforeLoadMore, viewModel.uiState.value.users.size)
        }

    @Test
    fun `refreshUsers resets and reloads list`() =
        runTest(testDispatcher) {
            val testUsers =
                listOf(
                    UserModel(uid = "1", name = "Alice", email = "alice@test.com"),
                )
            val fakeRepo = FakeAdminRepository()
            fakeRepo.allUsersResult = Result.Success(UserPage(testUsers, null))

            val viewModel = AdminUserListViewModel(fakeRepo)
            advanceUntilIdle()

            assertEquals(1, viewModel.uiState.value.users.size)

            val updatedUsers =
                listOf(
                    UserModel(uid = "1", name = "Alice", email = "alice@test.com"),
                    UserModel(uid = "2", name = "Bob", email = "bob@test.com"),
                )
            fakeRepo.allUsersResult = Result.Success(UserPage(updatedUsers, null))

            viewModel.refreshUsers()
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isRefreshing)
            assertEquals(2, viewModel.uiState.value.users.size)
        }
}
