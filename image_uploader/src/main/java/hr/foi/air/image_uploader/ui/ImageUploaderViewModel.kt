package hr.foi.air.image_uploader.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import hr.foi.air.image_uploader.ImageUploader
import hr.foi.air.image_uploader.model.ImageUploadResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ImageUploaderViewModel @Inject constructor(
    private val imageUploader: ImageUploader
) : ViewModel() {

    private val _uploadState = MutableStateFlow<ImageUploadResult<List<String>>>(ImageUploadResult.Idle)
    val uploadState: StateFlow<ImageUploadResult<List<String>>> = _uploadState.asStateFlow()

    fun uploadImages(imageUris: List<Uri>) {
        if (imageUris.isEmpty()) {
            _uploadState.value = ImageUploadResult.Success(emptyList())
            return
        }

        _uploadState.value = ImageUploadResult.Loading

        viewModelScope.launch {
            when (val result = imageUploader.uploadImages(imageUris)) {
                is hr.foi.air.image_uploader.model.Result.Success -> {
                    _uploadState.value = ImageUploadResult.Success(result.data)
                }
                is hr.foi.air.image_uploader.model.Result.Error -> {
                    _uploadState.value = ImageUploadResult.Error(result.exception.message ?: "Unknown upload error")
                }
            }
        }
    }

    fun resetState() {
        _uploadState.value = ImageUploadResult.Idle
    }
}
