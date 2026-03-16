package hr.foi.air.honnomachi.viewmodel

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import hr.foi.air.honnomachi.data.BookRepository
import hr.foi.air.honnomachi.model.BookModel
import hr.foi.air.honnomachi.model.ItemStatus
import hr.foi.air.honnomachi.ui.listings.ListingsFilter
import hr.foi.air.honnomachi.ui.listings.MyListingsViewModel
import hr.foi.air.honnomachi.util.Result
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class FakeListingsBookRepository : BookRepository {
    val listings = MutableStateFlow<List<BookModel>>(emptyList())

    override fun getBooks(): Flow<Result<List<BookModel>>> = MutableStateFlow(Result.Success(emptyList<BookModel>()))

    override suspend fun getBookDetails(bookId: String): Result<BookModel?> = Result.Success(null)

    override suspend fun addBook(book: BookModel): Result<String> = Result.Success("newId")

    override fun getSoldBooks(userId: String): Flow<Result<List<BookModel>>> = MutableStateFlow(Result.Success(emptyList<BookModel>()))

    override fun getPurchasedBooks(userId: String): Flow<Result<List<BookModel>>> = MutableStateFlow(Result.Success(emptyList<BookModel>()))

    override fun getMyListings(userId: String): Flow<Result<List<BookModel>>> = listings.map { Result.Success(it) }

    var updateStatusResult: Result<Unit> = Result.Success(Unit)

    override suspend fun updateListingStatus(
        bookId: String,
        newStatus: ItemStatus,
    ): Result<Unit> {
        if (updateStatusResult is Result.Success) {
            listings.value =
                listings.value.map {
                    if (it.bookId == bookId) it.copy(status = newStatus) else it
                }
        }
        return updateStatusResult
    }

    var deleteResult: Result<Unit> = Result.Success(Unit)

    override suspend fun deleteListing(bookId: String): Result<Unit> {
        if (deleteResult is Result.Success) {
            listings.value = listings.value.filter { it.bookId != bookId }
        }
        return deleteResult
    }

    fun setItems(items: List<BookModel>) {
        listings.value = items
    }
}

@ExperimentalCoroutinesApi
class MyListingsViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeBookRepository: FakeListingsBookRepository
    private lateinit var mockAuth: FirebaseAuth
    private lateinit var viewModel: MyListingsViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeBookRepository = FakeListingsBookRepository()
        mockAuth = mockk(relaxed = true)

        val mockUser = mockk<FirebaseUser>()
        every { mockUser.uid } returns "testUser123"
        every { mockAuth.currentUser } returns mockUser

        fakeBookRepository.setItems(
            listOf(
                BookModel(bookId = "1", title = "Active Book", status = ItemStatus.AVAILABLE),
                BookModel(bookId = "2", title = "Inactive Book", status = ItemStatus.INACTIVE),
                BookModel(bookId = "3", title = "Sold Book", status = ItemStatus.SOLD),
            ),
        )

        viewModel = MyListingsViewModel(fakeBookRepository, mockAuth)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init loads listings successfully`() =
        runTest(testDispatcher) {
            advanceTimeBy(500)
            runCurrent()

            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertEquals(3, state.listings.size)
            assertNull(state.errorMessage)
        }

    @Test
    fun `onFilterChange updates selected filter`() =
        runTest(testDispatcher) {
            advanceTimeBy(500)
            runCurrent()

            viewModel.onFilterChange(ListingsFilter.ACTIVE)
            assertEquals(ListingsFilter.ACTIVE, viewModel.uiState.value.selectedFilter)

            viewModel.onFilterChange(ListingsFilter.INACTIVE)
            assertEquals(ListingsFilter.INACTIVE, viewModel.uiState.value.selectedFilter)
        }

    @Test
    fun `onToggleStatus deactivates active listing`() =
        runTest(testDispatcher) {
            advanceTimeBy(500)
            runCurrent()

            viewModel.onToggleStatus("1", ItemStatus.AVAILABLE)
            advanceTimeBy(500)
            runCurrent()

            val updatedBook =
                viewModel.uiState.value.listings
                    .find { it.bookId == "1" }
            assertEquals(ItemStatus.INACTIVE, updatedBook?.status)
        }

    @Test
    fun `onToggleStatus activates inactive listing`() =
        runTest(testDispatcher) {
            advanceTimeBy(500)
            runCurrent()

            viewModel.onToggleStatus("2", ItemStatus.INACTIVE)
            advanceTimeBy(500)
            runCurrent()

            val updatedBook =
                viewModel.uiState.value.listings
                    .find { it.bookId == "2" }
            assertEquals(ItemStatus.AVAILABLE, updatedBook?.status)
        }

    @Test
    fun `onToggleStatus shows error on failure`() =
        runTest(testDispatcher) {
            advanceTimeBy(500)
            runCurrent()

            fakeBookRepository.updateStatusResult = Result.Error(Exception("Update failed"))

            viewModel.onToggleStatus("1", ItemStatus.AVAILABLE)
            advanceTimeBy(500)
            runCurrent()

            assertEquals("Update failed", viewModel.uiState.value.snackbarMessage)
        }

    @Test
    fun `onRequestDelete sets bookToDelete`() =
        runTest(testDispatcher) {
            advanceTimeBy(500)
            runCurrent()

            val book = viewModel.uiState.value.listings[0]
            viewModel.onRequestDelete(book)

            assertEquals(book, viewModel.uiState.value.bookToDelete)
        }

    @Test
    fun `onDismissDelete clears bookToDelete`() =
        runTest(testDispatcher) {
            advanceTimeBy(500)
            runCurrent()

            val book = viewModel.uiState.value.listings[0]
            viewModel.onRequestDelete(book)
            viewModel.onDismissDelete()

            assertNull(viewModel.uiState.value.bookToDelete)
        }

    @Test
    fun `onConfirmDelete removes listing`() =
        runTest(testDispatcher) {
            advanceTimeBy(500)
            runCurrent()

            val book =
                viewModel.uiState.value.listings
                    .find { it.bookId == "1" }!!
            viewModel.onRequestDelete(book)
            viewModel.onConfirmDelete()
            advanceTimeBy(500)
            runCurrent()

            val listings = viewModel.uiState.value.listings
            assertEquals(2, listings.size)
            assertNull(listings.find { it.bookId == "1" })
        }

    @Test
    fun `onConfirmDelete shows error on failure`() =
        runTest(testDispatcher) {
            advanceTimeBy(500)
            runCurrent()

            fakeBookRepository.deleteResult = Result.Error(Exception("Delete failed"))

            val book = viewModel.uiState.value.listings[0]
            viewModel.onRequestDelete(book)
            viewModel.onConfirmDelete()
            advanceTimeBy(500)
            runCurrent()

            assertEquals("Delete failed", viewModel.uiState.value.snackbarMessage)
        }

    @Test
    fun `onSnackbarDismissed clears snackbar message`() =
        runTest(testDispatcher) {
            advanceTimeBy(500)
            runCurrent()

            fakeBookRepository.updateStatusResult = Result.Error(Exception("Error"))
            viewModel.onToggleStatus("1", ItemStatus.AVAILABLE)
            advanceTimeBy(500)
            runCurrent()

            viewModel.onSnackbarDismissed()
            assertNull(viewModel.uiState.value.snackbarMessage)
        }
}
