package hr.foi.air.image_uploader

import android.net.Uri
import hr.foi.air.image_uploader.model.Result

interface ImageUploader {
    suspend fun uploadImage(imageUri: Uri): Result<String>
    suspend fun deleteImage(imageUrl: String): Result<Unit>
}
