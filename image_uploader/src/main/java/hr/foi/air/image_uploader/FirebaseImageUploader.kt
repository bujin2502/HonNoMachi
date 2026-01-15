package hr.foi.air.image_uploader

import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import hr.foi.air.image_uploader.model.Result
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class FirebaseImageUploader @Inject constructor(
    private val storage: FirebaseStorage
) : ImageUploader {
    override suspend fun uploadImage(imageUri: Uri, bookId: String): Result<String> {
        return try {
            val storageRef = storage.reference.child("images/books/$bookId")
            storageRef.putFile(imageUri).await()
            val downloadUrl = storageRef.downloadUrl.await().toString()
            Result.Success(downloadUrl)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
