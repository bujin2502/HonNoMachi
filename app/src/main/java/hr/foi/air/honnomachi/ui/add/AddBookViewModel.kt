package hr.foi.air.honnomachi.ui.add

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import hr.foi.air.honnomachi.R
import hr.foi.air.honnomachi.data.BookRepository
import hr.foi.air.honnomachi.model.BookModel
import hr.foi.air.honnomachi.util.Result
import hr.foi.air.image_uploader.ImageUploader
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    ) : AndroidViewModel(application) {
        private val _uiState = MutableStateFlow<AddBookUiState>(AddBookUiState.Idle)
        val uiState: StateFlow<AddBookUiState> = _uiState.asStateFlow()

        fun createListing(
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
                // First, upload images if there are any
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

                // Then create the book with uploaded image URLs
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

        fun resetState() {
            _uiState.value = AddBookUiState.Idle
        }
    }
