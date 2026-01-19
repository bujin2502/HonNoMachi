package hr.foi.air.image_uploader.model

import androidx.compose.ui.graphics.vector.ImageVector

data class ImageSource(
    val id: String,
    val nameResId: Int,
    val icon: ImageVector,
)
