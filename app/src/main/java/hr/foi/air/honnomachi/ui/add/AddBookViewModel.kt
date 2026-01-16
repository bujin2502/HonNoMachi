package hr.foi.air.honnomachi.ui.add

import android.app.Application
import android.net.Uri
import android.util.Log
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
import hr.foi.air.image_uploader.model.Result as ImageUploadResult

@HiltViewModel
class AddBookViewModel
    @Inject
    constructor(
        private val application: Application,
        private val bookRepository: BookRepository,
        private val imageUploader: ImageUploader,
        private val auth: FirebaseAuth,
    ) : AndroidViewModel(application) {
        private val _uiState = MutableStateFlow<AddBookUiState>(AddBookUiState.Idle)
        val uiState: StateFlow<AddBookUiState> = _uiState.asStateFlow()

        private fun createListing(book: BookModel) {
            viewModelScope.launch {
                when (val result = bookRepository.addBook(book)) {
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

        fun uploadImagesAndCreateListing(
            imageUris: List<Uri>,
            book: BookModel,
        ) {
            if (_uiState.value == AddBookUiState.Submitting) {
                return
            }

            // Check if user is authenticated
            val currentUser = auth.currentUser
            Log.d("AddBookViewModel", "Current user: ${currentUser?.uid}")
            Log.d("AddBookViewModel", "Email verified: ${currentUser?.isEmailVerified}")
            Log.d("AddBookViewModel", "Number of images: ${imageUris.size}")

            if (currentUser == null) {
                Log.e("AddBookViewModel", "User not authenticated")
                _uiState.value = AddBookUiState.Error(application.getString(R.string.error_user_not_authenticated))
                return
            }

            _uiState.value = AddBookUiState.Submitting

            viewModelScope.launch {
                val imageUrls = mutableListOf<String>()
                var uploadFailed = false

                if (imageUris.isNotEmpty()) {
                    for (uri in imageUris) {
                        when (val result = imageUploader.uploadImage(uri)) {
                            is ImageUploadResult.Success -> imageUrls.add(result.data)
                            is ImageUploadResult.Error -> {
                                // Rollback: delete all previously uploaded images
                                for (uploadedUrl in imageUrls) {
                                    imageUploader.deleteImage(uploadedUrl)
                                }
                                _uiState.value =
                                    AddBookUiState.Error(
                                        result.exception.message
                                            ?: application.getString(R.string.error_image_upload_failed),
                                    )
                                uploadFailed = true
                                break
                            }
                        }
                    }
                }

                if (!uploadFailed) {
                    val updatedBook = book.copy(imageUrls = imageUrls)
                    createListing(updatedBook)
                }
            }
        }

        fun resetState() {
            _uiState.value = AddBookUiState.Idle
        }
    }
