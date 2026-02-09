package hr.foi.air.image_uploader.model

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

interface ImageSource {
    val id: String
    val nameResId: Int
    val icon: ImageVector

    @Composable
    fun rememberLauncher(
        onImageSelected: (List<Uri>) -> Unit
    ): () -> Unit
}
