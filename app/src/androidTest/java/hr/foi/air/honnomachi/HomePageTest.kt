package hr.foi.air.honnomachi

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import hr.foi.air.honnomachi.model.BookModel
import hr.foi.air.honnomachi.model.Currency
import hr.foi.air.honnomachi.pages.BookListState
import hr.foi.air.honnomachi.pages.HomePage
import hr.foi.air.honnomachi.repository.BookRepository
import hr.foi.air.honnomachi.ui.theme.HonNoMachiTheme
import hr.foi.air.honnomachi.viewmodel.HomeViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

//Fake model
class FakeHomeViewModel : HomeViewModel(
    bookRepository = object : BookRepository {
        override fun getBooks(): Flow<BookListState> = flowOf(BookListState.Loading)
        override suspend fun getBookDetails(bookId: String): BookModel? = null
    }
){
    init {
        _bookListState.value = BookListState.Success(
            listOf(
                BookModel(
                    bookId = "1",
                    title = "IT",
                    authors = listOf("Stephen King"),
                    price = 15.0,
                    priceCurrency = Currency.EUR
                ),
                BookModel(
                    bookId = "2",
                    title = "Harry Potter",
                    authors = listOf("J.K. Rowling"),
                    price = 20.0,
                    priceCurrency = Currency.USD
                )
            )
        )
    }
    fun emitState(state: BookListState) {
        _bookListState.value = state
    }

    override fun getBooks() { }
}

@RunWith(AndroidJUnit4::class)
class HomePageTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // Pomocna funkcija za pokretanje ekrana
    private fun launchHomePage(): FakeHomeViewModel {
        val fakeVM = FakeHomeViewModel()
        composeTestRule.setContent {
            HonNoMachiTheme {
                HomePage(
                    paddingValues = PaddingValues(0.dp),
                    navController = rememberNavController(),
                    viewModel = fakeVM
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
        composeTestRule.onNodeWithText("No books found matching your search.", ignoreCase = true, substring = true).assertIsDisplayed()
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
        viewModel.emitState(BookListState.Error(errorMessage))
        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText(errorMessage, substring = true, ignoreCase = true)
            .assertIsDisplayed()
    }
}