package hr.foi.air.image_uploader.sources

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import hr.foi.air.image_uploader.R
import hr.foi.air.image_uploader.model.ImageSource

/**
 * Izvor slika iz galerije uređaja.
 */
class GalleryImageSource : ImageSource {
    override val id: String = "gallery"
    override val nameResId: Int = R.string.gallery
    override val icon: ImageVector = Icons.Default.PhotoLibrary

    @Composable
    override fun rememberLauncher(
        onImageSelected: (List<Uri>) -> Unit
    ): () -> Unit {
        val launcher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetMultipleContents(),
            onResult = { uris ->
                if (uris.isNotEmpty()) {
                    onImageSelected(uris)
                }
            }
        )

        return { launcher.launch("image/*") }
    }
}
