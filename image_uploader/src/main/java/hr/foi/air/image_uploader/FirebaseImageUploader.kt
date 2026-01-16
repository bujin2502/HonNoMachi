package hr.foi.air.image_uploader

import android.net.Uri
import android.util.Log
import com.google.firebase.storage.FirebaseStorage
import hr.foi.air.image_uploader.model.Result
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject

class FirebaseImageUploader @Inject constructor(
    private val storage: FirebaseStorage
) : ImageUploader {
    override suspend fun uploadImage(imageUri: Uri): Result<String> {
        return try {
            val imageId = UUID.randomUUID().toString()
            val storageRef = storage.reference.child("images/books/$imageId")
            Log.d("FirebaseImageUploader", "Uploading image to: images/books/$imageId")
            Log.d("FirebaseImageUploader", "Image URI: $imageUri")
            storageRef.putFile(imageUri).await()
            val downloadUrl = storageRef.downloadUrl.await().toString()
            Log.d("FirebaseImageUploader", "Image uploaded successfully: $downloadUrl")
            Result.Success(downloadUrl)
        } catch (e: Exception) {
            Log.e("FirebaseImageUploader", "Upload failed: ${e.message}", e)
            Log.e("FirebaseImageUploader", "Exception type: ${e.javaClass.simpleName}")
            if (e.cause != null) {
                Log.e("FirebaseImageUploader", "Cause: ${e.cause?.message}")
            }
            Result.Error(e)
        }
    }

    override suspend fun deleteImage(imageUrl: String): Result<Unit> {
        return try {
            val storageRef = storage.getReferenceFromUrl(imageUrl)
            storageRef.delete().await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
