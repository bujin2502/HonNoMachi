package hr.foi.air.honnomachi

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import hr.foi.air.honnomachi.model.BookCondition
import hr.foi.air.honnomachi.model.BookGenre
import hr.foi.air.honnomachi.model.BookModel
import hr.foi.air.honnomachi.model.Currency
import hr.foi.air.honnomachi.model.Language
import hr.foi.air.honnomachi.screen.BookDetailContent
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BookDetailScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun bookDetailContent_displaysCorrectData() {
        val book = BookModel(
            bookId = "1",
            title = "Test Book",
            authors = listOf("Test Author"),
            description = "This is a test book.",
            publisher = "Test Publisher",
            publicationYear = 2023,
            genre = BookGenre.FANTASY,
            imageUrls = emptyList(),
            price = 19.99,
            priceCurrency = Currency.USD,
            condition = BookCondition.NEW,
            language = Language.EN
        )

        composeTestRule.setContent {
            BookDetailContent(book = book)
        }

        composeTestRule.onNodeWithText("Test Book").assertExists()
        composeTestRule.onNodeWithText("Test Author").assertExists()
        composeTestRule.onNodeWithText("Test Publisher").assertExists()
        composeTestRule.onNodeWithText("2023").assertExists()
        composeTestRule.onNodeWithText("19.99 USD").assertExists()
        composeTestRule.onNodeWithText("This is a test book.").assertExists()
    }
}
