package hr.foi.air.honnomachi.ui.add

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import hr.foi.air.honnomachi.R
import hr.foi.air.honnomachi.data.BookRepository
import hr.foi.air.honnomachi.data.FirestoreUserDataSource
import hr.foi.air.honnomachi.model.BookCondition
import hr.foi.air.honnomachi.model.BookGenre
import hr.foi.air.honnomachi.model.BookModel
import hr.foi.air.honnomachi.model.Currency
import hr.foi.air.honnomachi.model.Language
import hr.foi.air.honnomachi.util.Result
import hr.foi.air.image_uploader.ImageUploader
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddBookViewModel
    @Inject
    constructor(
        private val application: Application,
        private val bookRepository: BookRepository,
        private val auth: FirebaseAuth,
        private val imageUploader: ImageUploader,
        private val userDataSource: FirestoreUserDataSource,
    ) : AndroidViewModel(application) {
        private val _uiState = MutableStateFlow<AddBookUiState>(AddBookUiState.Idle)
        val uiState: StateFlow<AddBookUiState> = _uiState.asStateFlow()

        private val _formState = MutableStateFlow(AddBookFormState())
        val formState: StateFlow<AddBookFormState> = _formState.asStateFlow()

        fun onTitleChange(value: String) {
            _formState.update { it.copy(title = value, titleError = null) }
        }

        fun onAuthorsChange(value: String) {
            _formState.update { it.copy(authors = value, authorsError = null) }
        }

        fun onPriceChange(value: String) {
            _formState.update { it.copy(price = value, priceError = null) }
        }

        fun onPublisherChange(value: String) {
            _formState.update { it.copy(publisher = value) }
        }

        fun onPublicationYearChange(value: String) {
            _formState.update { it.copy(publicationYear = value, yearError = null) }
        }

        fun onIsbnChange(value: String) {
            _formState.update { it.copy(isbn = value) }
        }

        fun onDescriptionChange(value: String) {
            _formState.update { it.copy(description = value) }
        }

        fun onImageUrisChange(uris: List<Uri>) {
            _formState.update { it.copy(imageUris = uris) }
        }

        fun onGenreChange(genre: BookGenre) {
            _formState.update { it.copy(selectedGenre = genre) }
        }

        fun onConditionChange(condition: BookCondition) {
            _formState.update { it.copy(selectedCondition = condition) }
        }

        fun onLanguageChange(language: Language) {
            _formState.update { it.copy(selectedLanguage = language) }
        }

        fun onCurrencyChange(currency: Currency) {
            _formState.update { it.copy(selectedCurrency = currency) }
        }

        fun submitForm() {
            val form = _formState.value
            val validationResult = validateForm(form)

            if (!validationResult.isValid) {
                _formState.update {
                    it.copy(
                        titleError = validationResult.titleError,
                        authorsError = validationResult.authorsError,
                        priceError = validationResult.priceError,
                        yearError = validationResult.yearError,
                    )
                }
                return
            }

            createListing(validationResult.book!!, form.imageUris)
        }

        private fun validateForm(form: AddBookFormState): ValidationResult {
            val cleanedTitle = form.title.trim()
            val cleanedAuthors =
                form.authors
                    .split(",")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
            val priceValue =
                form.price
                    .trim()
                    .replace(',', '.')
                    .toDoubleOrNull()
            val yearValue = form.publicationYear.trim().toIntOrNull()

            var titleError: Int? = null
            var authorsError: Int? = null
            var priceError: Int? = null
            var yearError: Int? = null
            var isValid = true

            if (cleanedTitle.isBlank()) {
                titleError = R.string.error_title_required
                isValid = false
            }
            if (cleanedAuthors.isEmpty()) {
                authorsError = R.string.error_author_required
                isValid = false
            }
            if (form.price.isBlank()) {
                priceError = R.string.error_price_required
                isValid = false
            } else if (priceValue == null || priceValue <= 0.0) {
                priceError = R.string.error_price_invalid
                isValid = false
            }
            if (form.publicationYear.isNotBlank() && yearValue == null) {
                yearError = R.string.error_year_invalid
                isValid = false
            }

            if (!isValid || priceValue == null) {
                return ValidationResult(
                    isValid = false,
                    titleError = titleError,
                    authorsError = authorsError,
                    priceError = priceError,
                    yearError = yearError,
                    book = null,
                )
            }

            val book =
                BookModel(
                    title = cleanedTitle,
                    authors = cleanedAuthors,
                    price = priceValue,
                    priceCurrency = form.selectedCurrency,
                    description = form.description.trim(),
                    publisher = form.publisher.trim(),
                    publicationYear = yearValue ?: 0,
                    isbn13 = form.isbn.trim(),
                    genre = form.selectedGenre,
                    condition = form.selectedCondition,
                    language = form.selectedLanguage,
                    imageUrls = emptyList(),
                )

            return ValidationResult(isValid = true, book = book)
        }

        private fun createListing(
            book: BookModel,
            imageUris: List<Uri>,
        ) {
            if (_uiState.value == AddBookUiState.Submitting) {
                return
            }

            val currentUser = auth.currentUser
            if (currentUser == null) {
                _uiState.value = AddBookUiState.Error(application.getString(R.string.error_user_not_authenticated))
                return
            }

            _uiState.value = AddBookUiState.Submitting

            viewModelScope.launch {
                val userData = userDataSource.getUser(currentUser.uid)
                if (userData?.suspended == true) {
                    _uiState.value = AddBookUiState.Error(application.getString(R.string.error_suspended_cannot_add_book))
                    return@launch
                }

                val imageUrls =
                    if (imageUris.isNotEmpty()) {
                        when (val uploadResult = imageUploader.uploadImages(imageUris, "images/books")) {
                            is hr.foi.air.image_uploader.model.Result.Success -> {
                                uploadResult.data
                            }
                            is hr.foi.air.image_uploader.model.Result.Error -> {
                                _uiState.value =
                                    AddBookUiState.Error(
                                        uploadResult.exception.message ?: application.getString(R.string.error_offer_save_failed),
                                    )
                                return@launch
                            }
                        }
                    } else {
                        emptyList()
                    }

                val updatedBook = book.copy(imageUrls = imageUrls)
                when (val result = bookRepository.addBook(updatedBook)) {
                    is Result.Success -> {
                        _uiState.value = AddBookUiState.Success
                    }
                    is Result.Error -> {
                        _uiState.value =
                            AddBookUiState.Error(result.exception.message ?: application.getString(R.string.error_offer_save_failed))
                    }
                }
            }
        }

        fun resetForm() {
            _formState.value = AddBookFormState()
        }

        fun resetState() {
            _uiState.value = AddBookUiState.Idle
        }

        private data class ValidationResult(
            val isValid: Boolean,
            val titleError: Int? = null,
            val authorsError: Int? = null,
            val priceError: Int? = null,
            val yearError: Int? = null,
            val book: BookModel? = null,
        )
    }
