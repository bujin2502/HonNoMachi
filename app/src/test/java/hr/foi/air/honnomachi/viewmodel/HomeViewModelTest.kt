package hr.foi.air.honnomachi.viewmodel

import android.content.Context
import com.google.android.gms.ads.AdView
import hr.foi.air.honnomachi.ads.AdBannerState
import hr.foi.air.honnomachi.ads.AdFrequencyTracker
import hr.foi.air.honnomachi.ads.AdManager
import hr.foi.air.honnomachi.data.BookRepository
import hr.foi.air.honnomachi.model.BookModel
import hr.foi.air.honnomachi.model.ItemStatus
import hr.foi.air.honnomachi.ui.home.HomeViewModel
import hr.foi.air.honnomachi.util.Result
import io.mockk.mockk
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

    override fun getMyListings(userId: String): Flow<Result<List<BookModel>>> = flowOf(Result.Success(emptyList()))

    override suspend fun updateListingStatus(
        bookId: String,
        newStatus: ItemStatus,
    ): Result<Unit> = Result.Success(Unit)

    override suspend fun deleteListing(bookId: String): Result<Unit> = Result.Success(Unit)
}

class FakeAdFrequencyTracker(
    private var allowed: Boolean = true,
) : AdFrequencyTracker {
    private var impressions = 0

    override fun canShowAd(): Boolean = allowed

    override fun recordImpression() {
        impressions++
    }

    override fun getImpressionCount(): Int = impressions
}

class FakeAdManager : AdManager {
    override fun createBannerAdView(
        context: Context,
        onAdLoaded: () -> Unit,
        onAdFailed: (String) -> Unit,
    ): AdView = mockk(relaxed = true)
}

@ExperimentalCoroutinesApi
class HomeViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private val fakeAdManager = FakeAdManager()
    private val fakeAdTracker = FakeAdFrequencyTracker()

    private fun createViewModel(
        bookRepository: BookRepository = HomeTestBookRepository(),
        adManager: AdManager = fakeAdManager,
        adFrequencyTracker: AdFrequencyTracker = fakeAdTracker,
    ) = HomeViewModel(bookRepository, adManager, adFrequencyTracker)

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
            val viewModel = createViewModel()
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
            val viewModel = createViewModel(bookRepository = HomeTestBookRepository(testBooks))
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
            val viewModel = createViewModel(bookRepository = HomeTestBookRepository(shouldFail = true))
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertTrue(state.books.isEmpty())
            assertEquals("Test error", state.errorMessage)
        }

    @Test
    fun `getBooks with empty list returns empty books`() =
        runTest(testDispatcher) {
            val viewModel = createViewModel(bookRepository = HomeTestBookRepository(emptyList()))
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertTrue(state.books.isEmpty())
            assertNull(state.errorMessage)
        }

    @Test
    fun `onSearchQueryChange updates query`() =
        runTest(testDispatcher) {
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onSearchQueryChange("kotlin")
            assertEquals("kotlin", viewModel.uiState.value.searchQuery)
        }

    @Test
    fun `onSearchQueryChange with empty string clears query`() =
        runTest(testDispatcher) {
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onSearchQueryChange("test")
            viewModel.onSearchQueryChange("")
            assertEquals("", viewModel.uiState.value.searchQuery)
        }

    @Test
    fun `ad state is Loading when frequency cap not reached`() =
        runTest(testDispatcher) {
            val viewModel = createViewModel(adFrequencyTracker = FakeAdFrequencyTracker(allowed = true))
            advanceUntilIdle()

            assertEquals(AdBannerState.Loading, viewModel.uiState.value.adState)
        }

    @Test
    fun `ad state is Capped when frequency limit reached`() =
        runTest(testDispatcher) {
            val viewModel = createViewModel(adFrequencyTracker = FakeAdFrequencyTracker(allowed = false))
            advanceUntilIdle()

            assertEquals(AdBannerState.Capped, viewModel.uiState.value.adState)
        }

    @Test
    fun `onAdLoaded updates state to Loaded and records impression`() =
        runTest(testDispatcher) {
            val tracker = FakeAdFrequencyTracker(allowed = true)
            val viewModel = createViewModel(adFrequencyTracker = tracker)
            advanceUntilIdle()

            viewModel.onAdLoaded()

            assertEquals(AdBannerState.Loaded, viewModel.uiState.value.adState)
            assertEquals(1, tracker.getImpressionCount())
        }

    @Test
    fun `onAdFailed updates state to Failed with reason`() =
        runTest(testDispatcher) {
            val viewModel = createViewModel()
            advanceUntilIdle()

            viewModel.onAdFailed("Network error")

            val state = viewModel.uiState.value.adState
            assertTrue(state is AdBannerState.Failed)
            assertEquals("Network error", (state as AdBannerState.Failed).reason)
        }
}
