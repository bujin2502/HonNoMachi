package hr.foi.air.honnomachi.viewmodel

import android.app.Application
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import hr.foi.air.honnomachi.data.BookRepository
import hr.foi.air.honnomachi.data.FirestoreUserDataSource
import hr.foi.air.honnomachi.model.BookCondition
import hr.foi.air.honnomachi.model.UserModel
import hr.foi.air.honnomachi.model.BookGenre
import hr.foi.air.honnomachi.model.Currency
import hr.foi.air.honnomachi.model.Language
import hr.foi.air.honnomachi.ui.add.AddBookUiState
import hr.foi.air.honnomachi.ui.add.AddBookViewModel
import hr.foi.air.honnomachi.util.Result
import hr.foi.air.image_uploader.ImageUploader
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class AddBookViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mockApplication: Application
    private lateinit var mockBookRepository: BookRepository
    private lateinit var mockAuth: FirebaseAuth
    private lateinit var mockImageUploader: ImageUploader
    private lateinit var mockUserDataSource: FirestoreUserDataSource
    private lateinit var viewModel: AddBookViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockApplication = mockk(relaxed = true)
        mockBookRepository = mockk(relaxed = true)
        mockAuth = mockk(relaxed = true)
        mockImageUploader = mockk(relaxed = true)
        mockUserDataSource = mockk(relaxed = true)

        every { mockApplication.getString(any()) } returns "Mock string"
        every { mockAuth.currentUser } returns mockk<FirebaseUser>(relaxed = true)
        coEvery { mockBookRepository.addBook(any()) } returns Result.Success("new-book-id")
        coEvery { mockUserDataSource.getUser(any()) } returns UserModel(suspended = false)

        viewModel = AddBookViewModel(mockApplication, mockBookRepository, mockAuth, mockImageUploader, mockUserDataSource)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial uiState is Idle`() {
        assertTrue(viewModel.uiState.value is AddBookUiState.Idle)
    }

    @Test
    fun `initial formState has default values`() {
        val form = viewModel.formState.value
        assertEquals("", form.title)
        assertEquals("", form.authors)
        assertEquals("", form.price)
        assertEquals("", form.publisher)
        assertEquals("", form.publicationYear)
        assertEquals("", form.isbn)
        assertEquals("", form.description)
        assertEquals(BookGenre.OTHER, form.selectedGenre)
        assertEquals(BookCondition.USED, form.selectedCondition)
        assertEquals(Language.HR, form.selectedLanguage)
        assertEquals(Currency.EUR, form.selectedCurrency)
    }

    @Test
    fun `onTitleChange updates title and clears error`() {
        viewModel.onTitleChange("New Title")

        assertEquals("New Title", viewModel.formState.value.title)
        assertNull(viewModel.formState.value.titleError)
    }

    @Test
    fun `onAuthorsChange updates authors and clears error`() {
        viewModel.onAuthorsChange("Author 1, Author 2")

        assertEquals("Author 1, Author 2", viewModel.formState.value.authors)
        assertNull(viewModel.formState.value.authorsError)
    }

    @Test
    fun `onPriceChange updates price and clears error`() {
        viewModel.onPriceChange("25.99")

        assertEquals("25.99", viewModel.formState.value.price)
        assertNull(viewModel.formState.value.priceError)
    }

    @Test
    fun `onGenreChange updates genre`() {
        viewModel.onGenreChange(BookGenre.FANTASY)

        assertEquals(BookGenre.FANTASY, viewModel.formState.value.selectedGenre)
    }

    @Test
    fun `onConditionChange updates condition`() {
        viewModel.onConditionChange(BookCondition.LIKE_NEW)

        assertEquals(BookCondition.LIKE_NEW, viewModel.formState.value.selectedCondition)
    }

    @Test
    fun `onLanguageChange updates language`() {
        viewModel.onLanguageChange(Language.EN)

        assertEquals(Language.EN, viewModel.formState.value.selectedLanguage)
    }

    @Test
    fun `onCurrencyChange updates currency`() {
        viewModel.onCurrencyChange(Currency.USD)

        assertEquals(Currency.USD, viewModel.formState.value.selectedCurrency)
    }

    @Test
    fun `onPublisherChange updates publisher`() {
        viewModel.onPublisherChange("Publisher Name")

        assertEquals("Publisher Name", viewModel.formState.value.publisher)
    }

    @Test
    fun `onPublicationYearChange updates year and clears error`() {
        viewModel.onPublicationYearChange("2024")

        assertEquals("2024", viewModel.formState.value.publicationYear)
        assertNull(viewModel.formState.value.yearError)
    }

    @Test
    fun `onIsbnChange updates isbn`() {
        viewModel.onIsbnChange("978-3-16-148410-0")

        assertEquals("978-3-16-148410-0", viewModel.formState.value.isbn)
    }

    @Test
    fun `onDescriptionChange updates description`() {
        viewModel.onDescriptionChange("A great book")

        assertEquals("A great book", viewModel.formState.value.description)
    }

    @Test
    fun `submitForm with empty title sets title error`() {
        viewModel.onAuthorsChange("Author")
        viewModel.onPriceChange("10.0")

        viewModel.submitForm()

        assertNotNull(viewModel.formState.value.titleError)
    }

    @Test
    fun `submitForm with empty authors sets authors error`() {
        viewModel.onTitleChange("Title")
        viewModel.onPriceChange("10.0")

        viewModel.submitForm()

        assertNotNull(viewModel.formState.value.authorsError)
    }

    @Test
    fun `submitForm with empty price sets price error`() {
        viewModel.onTitleChange("Title")
        viewModel.onAuthorsChange("Author")

        viewModel.submitForm()

        assertNotNull(viewModel.formState.value.priceError)
    }

    @Test
    fun `submitForm with invalid price sets price error`() {
        viewModel.onTitleChange("Title")
        viewModel.onAuthorsChange("Author")
        viewModel.onPriceChange("abc")

        viewModel.submitForm()

        assertNotNull(viewModel.formState.value.priceError)
    }

    @Test
    fun `submitForm with negative price sets price error`() {
        viewModel.onTitleChange("Title")
        viewModel.onAuthorsChange("Author")
        viewModel.onPriceChange("-5.0")

        viewModel.submitForm()

        assertNotNull(viewModel.formState.value.priceError)
    }

    @Test
    fun `submitForm with invalid year sets year error`() {
        viewModel.onTitleChange("Title")
        viewModel.onAuthorsChange("Author")
        viewModel.onPriceChange("10.0")
        viewModel.onPublicationYearChange("abc")

        viewModel.submitForm()

        assertNotNull(viewModel.formState.value.yearError)
    }

    @Test
    fun `submitForm with valid data transitions to Submitting`() =
        runTest(testDispatcher) {
            viewModel.onTitleChange("Valid Title")
            viewModel.onAuthorsChange("Valid Author")
            viewModel.onPriceChange("10.0")

            viewModel.submitForm()

            assertTrue(viewModel.uiState.value is AddBookUiState.Submitting)
        }

    @Test
    fun `submitForm with valid data and successful save transitions to Success`() =
        runTest(testDispatcher) {
            viewModel.onTitleChange("Valid Title")
            viewModel.onAuthorsChange("Valid Author")
            viewModel.onPriceChange("10.0")

            viewModel.submitForm()
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value is AddBookUiState.Success)
        }

    @Test
    fun `submitForm with valid data but repository error transitions to Error`() =
        runTest(testDispatcher) {
            coEvery { mockBookRepository.addBook(any()) } returns Result.Error(Exception("Save failed"))

            viewModel.onTitleChange("Valid Title")
            viewModel.onAuthorsChange("Valid Author")
            viewModel.onPriceChange("10.0")

            viewModel.submitForm()
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value is AddBookUiState.Error)
        }

    @Test
    fun `submitForm with no authenticated user transitions to Error`() =
        runTest(testDispatcher) {
            every { mockAuth.currentUser } returns null

            viewModel.onTitleChange("Valid Title")
            viewModel.onAuthorsChange("Valid Author")
            viewModel.onPriceChange("10.0")

            viewModel.submitForm()

            assertTrue(viewModel.uiState.value is AddBookUiState.Error)
        }

    @Test
    fun `resetForm clears all form fields`() {
        viewModel.onTitleChange("Title")
        viewModel.onAuthorsChange("Author")
        viewModel.onPriceChange("10.0")
        viewModel.onPublisherChange("Publisher")

        viewModel.resetForm()

        val form = viewModel.formState.value
        assertEquals("", form.title)
        assertEquals("", form.authors)
        assertEquals("", form.price)
        assertEquals("", form.publisher)
    }

    @Test
    fun `resetState returns to Idle`() =
        runTest(testDispatcher) {
            viewModel.onTitleChange("Valid Title")
            viewModel.onAuthorsChange("Valid Author")
            viewModel.onPriceChange("10.0")
            viewModel.submitForm()
            advanceUntilIdle()

            viewModel.resetState()

            assertTrue(viewModel.uiState.value is AddBookUiState.Idle)
        }

    @Test
    fun `submitForm with suspended user transitions to Error`() =
        runTest(testDispatcher) {
            coEvery { mockUserDataSource.getUser(any()) } returns UserModel(suspended = true)

            viewModel.onTitleChange("Valid Title")
            viewModel.onAuthorsChange("Valid Author")
            viewModel.onPriceChange("10.0")

            viewModel.submitForm()
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value is AddBookUiState.Error)
        }
}
