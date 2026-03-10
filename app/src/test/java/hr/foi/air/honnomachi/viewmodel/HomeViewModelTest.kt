package hr.foi.air.honnomachi.viewmodel

import hr.foi.air.honnomachi.data.BookRepository
import hr.foi.air.honnomachi.model.BookModel
import hr.foi.air.honnomachi.ui.home.HomeViewModel
import hr.foi.air.honnomachi.util.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
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

class HomeTestBookRepository(
    private val books: List<BookModel> = emptyList(),
    private val shouldFail: Boolean = false,
) : BookRepository {
    override fun getBooks(): Flow<Result<List<BookModel>>> =
        flowOf(
            if (shouldFail) {
                Result.Error(Exception("Test error"))
            } else {
                Result.Success(books)
            },
        )

    override suspend fun getBookDetails(bookId: String): Result<BookModel?> {
        val book = books.find { it.bookId == bookId }
        return if (book != null) {
            Result.Success(book)
        } else {
            Result.Error(Exception("Book not found"))
        }
    }

    override suspend fun addBook(book: BookModel): Result<String> = Result.Success("testId")

    override fun getSoldBooks(userId: String): Flow<Result<List<BookModel>>> = flowOf(Result.Success(emptyList()))

    override fun getPurchasedBooks(userId: String): Flow<Result<List<BookModel>>> = flowOf(Result.Success(emptyList()))
}

@ExperimentalCoroutinesApi
class HomeViewModelTest {
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
            val viewModel = HomeViewModel(HomeTestBookRepository())
            assertTrue(viewModel.uiState.value.isLoading)
        }

    @Test
    fun `getBooks loads books successfully`() =
        runTest(testDispatcher) {
            val testBooks =
                listOf(
                    BookModel(bookId = "1", title = "Test Book 1", price = 10.0),
                    BookModel(bookId = "2", title = "Test Book 2", price = 20.0),
                )
            val viewModel = HomeViewModel(HomeTestBookRepository(testBooks))
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertEquals(2, state.books.size)
            assertEquals("Test Book 1", state.books[0].title)
            assertEquals("Test Book 2", state.books[1].title)
            assertNull(state.errorMessage)
        }

    @Test
    fun `getBooks handles error`() =
        runTest(testDispatcher) {
            val viewModel = HomeViewModel(HomeTestBookRepository(shouldFail = true))
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertTrue(state.books.isEmpty())
            assertEquals("Test error", state.errorMessage)
        }

    @Test
    fun `getBooks with empty list returns empty books`() =
        runTest(testDispatcher) {
            val viewModel = HomeViewModel(HomeTestBookRepository(emptyList()))
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertTrue(state.books.isEmpty())
            assertNull(state.errorMessage)
        }

    @Test
    fun `onSearchQueryChange updates query`() =
        runTest(testDispatcher) {
            val viewModel = HomeViewModel(HomeTestBookRepository())
            advanceUntilIdle()

            viewModel.onSearchQueryChange("kotlin")
            assertEquals("kotlin", viewModel.uiState.value.searchQuery)
        }

    @Test
    fun `onSearchQueryChange with empty string clears query`() =
        runTest(testDispatcher) {
            val viewModel = HomeViewModel(HomeTestBookRepository())
            advanceUntilIdle()

            viewModel.onSearchQueryChange("test")
            viewModel.onSearchQueryChange("")
            assertEquals("", viewModel.uiState.value.searchQuery)
        }
}
