package hr.foi.air.image_uploader.model

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Interface za izvore slika (kamera, galerija, itd.)
 */
interface ImageSource {
    /** Jedinstveni identifikator izvora */
    val id: String

    /** Resource ID za ime izvora */
    val nameResId: Int

    /** Ikona izvora */
    val icon: ImageVector

    /**
     * Pokreće odabir slike iz ovog izvora.
     * @param onImageSelected Callback koji se poziva kada je slika odabrana
     */
    @Composable
    fun rememberLauncher(
        onImageSelected: (List<Uri>) -> Unit
    ): () -> Unit
}
