package hr.foi.air.honnomachi.viewmodel

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import hr.foi.air.honnomachi.data.BookRepository
import hr.foi.air.honnomachi.model.BookModel
import hr.foi.air.honnomachi.model.ItemStatus
import hr.foi.air.honnomachi.ui.shelf.ShelfTab
import hr.foi.air.honnomachi.ui.shelf.ShelfViewModel
import hr.foi.air.honnomachi.util.Result
import io.mockk.every
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

class ShelfTestBookRepository(
    private val soldBooks: List<BookModel> = emptyList(),
    private val purchasedBooks: List<BookModel> = emptyList(),
    private val shouldFailSold: Boolean = false,
    private val shouldFailPurchased: Boolean = false,
) : BookRepository {
    override fun getBooks(): Flow<Result<List<BookModel>>> = flowOf(Result.Success(emptyList()))

    override suspend fun getBookDetails(bookId: String): Result<BookModel?> = Result.Success(null)

    override suspend fun addBook(book: BookModel): Result<String> = Result.Success("id")

    override fun getSoldBooks(userId: String): Flow<Result<List<BookModel>>> =
        flowOf(
            if (shouldFailSold) {
                Result.Error(Exception("Greška pri dohvaćanju prodanih knjiga."))
            } else {
                Result.Success(soldBooks)
            },
        )

    override fun getPurchasedBooks(userId: String): Flow<Result<List<BookModel>>> =
        flowOf(
            if (shouldFailPurchased) {
                Result.Error(Exception("Greška pri dohvaćanju kupljenih knjiga."))
            } else {
                Result.Success(purchasedBooks)
            },
        )

    override fun getMyListings(userId: String): Flow<Result<List<BookModel>>> = flowOf(Result.Success(emptyList()))

    override suspend fun updateListingStatus(
        bookId: String,
        newStatus: ItemStatus,
    ): Result<Unit> = Result.Success(Unit)

    override suspend fun deleteListing(bookId: String): Result<Unit> = Result.Success(Unit)
}

@ExperimentalCoroutinesApi
class ShelfViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var auth: FirebaseAuth
    private lateinit var mockUser: FirebaseUser

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        auth = mockk()
        mockUser = mockk()
        every { mockUser.uid } returns "user123"
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadBooks no user sets error message`() =
        runTest(testDispatcher) {
            every { auth.currentUser } returns null

            val viewModel = ShelfViewModel(ShelfTestBookRepository(), auth)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals("Korisnik nije prijavljen.", state.errorMessage)
            assertFalse(state.isLoading)
        }

    @Test
    fun `initial tab is PURCHASED`() =
        runTest(testDispatcher) {
            every { auth.currentUser } returns mockUser

            val viewModel = ShelfViewModel(ShelfTestBookRepository(), auth)

            assertEquals(ShelfTab.PURCHASED, viewModel.uiState.value.selectedTab)
        }

    @Test
    fun `loadBooks purchased tab loads purchased books`() =
        runTest(testDispatcher) {
            every { auth.currentUser } returns mockUser

            val books = listOf(BookModel(bookId = "1", title = "Kupljena knjiga"))
            val viewModel = ShelfViewModel(ShelfTestBookRepository(purchasedBooks = books), auth)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertEquals(1, state.books.size)
            assertEquals("Kupljena knjiga", state.books[0].title)
            assertNull(state.errorMessage)
        }

    @Test
    fun `loadBooks sold tab loads sold books`() =
        runTest(testDispatcher) {
            every { auth.currentUser } returns mockUser

            val soldBooks = listOf(BookModel(bookId = "2", title = "Prodana knjiga"))
            val viewModel = ShelfViewModel(ShelfTestBookRepository(soldBooks = soldBooks), auth)
            viewModel.selectTab(ShelfTab.SOLD)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertEquals(1, state.books.size)
            assertEquals("Prodana knjiga", state.books[0].title)
        }

    @Test
    fun `selectTab changes selected tab`() =
        runTest(testDispatcher) {
            every { auth.currentUser } returns mockUser

            val viewModel = ShelfViewModel(ShelfTestBookRepository(), auth)
            viewModel.selectTab(ShelfTab.SOLD)

            assertEquals(ShelfTab.SOLD, viewModel.uiState.value.selectedTab)
        }

    @Test
    fun `loadBooks purchased tab error sets error message`() =
        runTest(testDispatcher) {
            every { auth.currentUser } returns mockUser

            val viewModel =
                ShelfViewModel(ShelfTestBookRepository(shouldFailPurchased = true), auth)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertEquals("Greška pri dohvaćanju kupljenih knjiga.", state.errorMessage)
            assertTrue(state.books.isEmpty())
        }

    @Test
    fun `loadBooks sold tab error sets error message`() =
        runTest(testDispatcher) {
            every { auth.currentUser } returns mockUser

            val viewModel = ShelfViewModel(ShelfTestBookRepository(shouldFailSold = true), auth)
            viewModel.selectTab(ShelfTab.SOLD)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertEquals("Greška pri dohvaćanju prodanih knjiga.", state.errorMessage)
            assertTrue(state.books.isEmpty())
        }

    @Test
    fun `loadBooks empty purchased books returns empty list without error`() =
        runTest(testDispatcher) {
            every { auth.currentUser } returns mockUser

            val viewModel = ShelfViewModel(ShelfTestBookRepository(), auth)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertTrue(state.books.isEmpty())
            assertNull(state.errorMessage)
        }

    @Test
    fun `selectTab MY_LISTINGS skips loadBooks and sets isLoading false`() =
        runTest(testDispatcher) {
            every { auth.currentUser } returns mockUser

            val viewModel = ShelfViewModel(ShelfTestBookRepository(), auth)
            viewModel.selectTab(ShelfTab.MY_LISTINGS)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(ShelfTab.MY_LISTINGS, state.selectedTab)
            assertFalse(state.isLoading)
            assertTrue(state.books.isEmpty())
            assertNull(state.errorMessage)
        }

    @Test
    fun `selectTab to PURCHASED after SOLD loads purchased books`() =
        runTest(testDispatcher) {
            every { auth.currentUser } returns mockUser

            val purchased = listOf(BookModel(bookId = "p1", title = "Purchased"))
            val sold = listOf(BookModel(bookId = "s1", title = "Sold"))
            val viewModel =
                ShelfViewModel(ShelfTestBookRepository(soldBooks = sold, purchasedBooks = purchased), auth)

            viewModel.selectTab(ShelfTab.SOLD)
            advanceUntilIdle()
            assertEquals(
                "Sold",
                viewModel.uiState.value.books[0]
                    .title,
            )

            viewModel.selectTab(ShelfTab.PURCHASED)
            advanceUntilIdle()
            assertEquals(
                "Purchased",
                viewModel.uiState.value.books[0]
                    .title,
            )
        }
}
