package hr.foi.air.image_uploader

import android.net.Uri
import hr.foi.air.image_uploader.model.Result

interface ImageUploader {
    suspend fun uploadImage(imageUri: Uri, uploadPath: String = "images"): Result<String>
    suspend fun deleteImage(imageUrl: String): Result<Unit>
    suspend fun uploadImages(imageUris: List<Uri>, uploadPath: String = "images"): Result<List<String>>
}
