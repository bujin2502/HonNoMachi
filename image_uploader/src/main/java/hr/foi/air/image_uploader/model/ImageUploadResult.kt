package hr.foi.air.image_uploader.model

sealed class ImageUploadResult<out T> {
    object Idle : ImageUploadResult<Nothing>()
    object Loading : ImageUploadResult<Nothing>()
    data class Success<out T>(val data: T) : ImageUploadResult<T>()
    data class Error(val message: String) : ImageUploadResult<Nothing>()
}
