package hr.foi.air.honnomachi

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import hr.foi.air.honnomachi.data.BookRepository
import hr.foi.air.honnomachi.model.BookModel
import hr.foi.air.honnomachi.model.Currency
import hr.foi.air.honnomachi.ui.home.HomePage
import hr.foi.air.honnomachi.ui.home.HomeUiState
import hr.foi.air.honnomachi.ui.home.HomeViewModel
import hr.foi.air.honnomachi.ui.theme.HonNoMachiTheme
import hr.foi.air.honnomachi.util.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// Fake model
class FakeHomeViewModel(
    bookRepository: BookRepository,
) : HomeViewModel(bookRepository) {
    private val _uiState = MutableStateFlow(HomeUiState(isLoading = true))
    override val uiState: StateFlow<HomeUiState> = _uiState

    init {
        _uiState.update {
            it.copy(
                isLoading = false,
                books =
                    listOf(
                        BookModel(
                            bookId = "1",
                            title = "IT",
                            authors = listOf("Stephen King"),
                            price = 15.0,
                            priceCurrency = Currency.EUR,
                        ),
                        BookModel(
                            bookId = "2",
                            title = "Harry Potter",
                            authors = listOf("J.K. Rowling"),
                            price = 20.0,
                            priceCurrency = Currency.USD,
                        ),
                    ),
            )
        }
    }

    fun emitState(state: HomeUiState) {
        _uiState.value = state
    }

    override fun onSearchQueryChange(newQuery: String) {
        _uiState.update { it.copy(searchQuery = newQuery) }
    }

    override fun getBooks() {} // Override to prevent actual data fetching
}

@RunWith(AndroidJUnit4::class)
class HomePageTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    // Pomocna funkcija za pokretanje ekrana
    private fun launchHomePage(): FakeHomeViewModel {
        val fakeBookRepository =
            object : BookRepository {
                override fun getBooks(): Flow<Result<List<BookModel>>> = flowOf(Result.Success(emptyList()))

                override suspend fun getBookDetails(bookId: String): Result<BookModel?> = Result.Success(null)

                override suspend fun addBook(book: BookModel): Result<String> = Result.Success("test-id")
            }
        val fakeVM = FakeHomeViewModel(fakeBookRepository)
        composeTestRule.setContent {
            HonNoMachiTheme {
                HomePage(
                    paddingValues = PaddingValues(0.dp),
                    navController = rememberNavController(),
                    viewModel = fakeVM,
                )
            }
        }
        return fakeVM
    }

    @Test
    fun initialDisplay_allBooksShouldBeVisible() {
        launchHomePage()

        composeTestRule.onNodeWithText("IT", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Stephen King", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("15.0", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("EUR", substring = true).assertIsDisplayed()

        composeTestRule.onNodeWithText("Harry Potter", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("J.K. Rowling", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("20.0", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("USD", substring = true).assertIsDisplayed()
    }

    @Test
    fun searchFiltering_resultsShouldMatchQuery() {
        launchHomePage()

        composeTestRule.onNodeWithTag("search_field").performTextInput("IT")
        composeTestRule.onNodeWithText("Stephen King", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("J.K. Rowling").assertDoesNotExist()
    }

    @Test
    fun emptySearch_shouldShowNoResultsMessage() {
        launchHomePage()

        composeTestRule.onNodeWithTag("search_field").performTextInput("Superman")

        val expectedText = "No books found matching your search."
        val expectedTextHR = "Nema knjiga koje odgovaraju pretrazi."
        try {
            composeTestRule.onNodeWithText(expectedText, ignoreCase = true, substring = true).assertIsDisplayed()
        } catch (e: AssertionError) {
            composeTestRule.onNodeWithText(expectedTextHR, ignoreCase = true, substring = true).assertIsDisplayed()
        }
    }

    @Test
    fun clearingSearch_shouldRestoreFullList() {
        launchHomePage()

        val searchNode = composeTestRule.onNodeWithTag("search_field")

        searchNode.performTextInput("IT")
        searchNode.performTextClearance()

        composeTestRule.onNodeWithText("IT").assertIsDisplayed()
        composeTestRule.onNodeWithText("Harry Potter").assertIsDisplayed()
    }

    @Test
    fun errorState_shouldShowErrorMessage() {
        val viewModel = launchHomePage()

        val errorMessage = "Failed to connect to server"
        viewModel.emitState(HomeUiState(errorMessage = errorMessage))
        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText(errorMessage, substring = true, ignoreCase = true)
            .assertIsDisplayed()
    }
}
