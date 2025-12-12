package hr.foi.air.honnomachi.viewmodel

import hr.foi.air.honnomachi.model.BookModel
import hr.foi.air.honnomachi.repository.BookRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

    fun addBook(book: BookModel) {
        book.bookId?.let { books[it] = book }
    }

    override fun getBooks(): kotlinx.coroutines.flow.Flow<hr.foi.air.honnomachi.pages.BookListState> = throw NotImplementedError()

    override suspend fun getBookDetails(bookId: String): BookModel? = books[bookId]
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
            fakeRepository.addBook(book)

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
