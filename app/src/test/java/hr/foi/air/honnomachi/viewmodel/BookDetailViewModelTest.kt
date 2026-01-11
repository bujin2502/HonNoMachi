package hr.foi.air.honnomachi.viewmodel

import hr.foi.air.honnomachi.data.BookRepository
import hr.foi.air.honnomachi.model.BookModel
import hr.foi.air.honnomachi.ui.book.BookDetailViewModel
import hr.foi.air.honnomachi.ui.book.BookUiState
import hr.foi.air.honnomachi.util.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeBookRepository : BookRepository {
    private val books = mutableMapOf<String, BookModel>()

    fun seedBook(book: BookModel) {
        book.bookId?.let { books[it] = book }
    }

    override fun getBooks(): Flow<Result<List<BookModel>>> = flowOf(Result.Success(emptyList()))

    override suspend fun getBookDetails(bookId: String): Result<BookModel?> =
        books[bookId]?.let { Result.Success(it) } ?: Result.Success(null)

    override suspend fun addBook(book: BookModel): Result<String> = Result.Success("test-id")
}

@ExperimentalCoroutinesApi
class BookDetailViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeBookRepository
    private lateinit var viewModel: BookDetailViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeBookRepository()
        viewModel = BookDetailViewModel(fakeRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadBookDetails with valid ID returns Success state`() =
        runTest {
            val book = BookModel(bookId = "123", title = "Test Title")
            fakeRepository.seedBook(book)

            viewModel.loadBookDetails("123")
            testDispatcher.scheduler.advanceUntilIdle()

            val currentState = viewModel.uiState.value
            assertTrue(currentState is BookUiState.Success)
            assertEquals(book, (currentState as BookUiState.Success).book)
        }

    @Test
    fun `loadBookDetails with invalid ID returns BookNotFound state`() =
        runTest {
            viewModel.loadBookDetails("404")
            testDispatcher.scheduler.advanceUntilIdle()

            val currentState = viewModel.uiState.value
            assertTrue(currentState is BookUiState.BookNotFound)
        }
}
