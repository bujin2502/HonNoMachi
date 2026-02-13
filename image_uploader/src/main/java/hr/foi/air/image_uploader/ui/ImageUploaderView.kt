package hr.foi.air.image_uploader.ui

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import hr.foi.air.image_uploader.ImageSourceInitializer
import hr.foi.air.image_uploader.model.ImageSourceRegistry

@Composable
fun ImageUploaderView(onImagesSelected: (List<Uri>) -> Unit) {
    LaunchedEffect(Unit) {
        ImageSourceInitializer.initialize()
    }

    val uriListSaver =
        listSaver<List<Uri>, String>(
            save = { uris -> uris.map { it.toString() } },
            restore = { strings -> strings.map { Uri.parse(it) } },
        )

    var imageUris by rememberSaveable(stateSaver = uriListSaver) { mutableStateOf(emptyList()) }
    var showImagePicker by rememberSaveable { mutableStateOf(false) }
    var selectedSourceId by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        if (imageUris.isNotEmpty()) {
            onImagesSelected(imageUris)
        }
    }

    val imageSources = ImageSourceRegistry.getAllSources()

    val launchers =
        imageSources.associate { source ->
            source.id to
                source.rememberLauncher { uris ->
                    val updatedUris = imageUris + uris
                    imageUris = updatedUris
                    onImagesSelected(updatedUris)
                }
        }

    LaunchedEffect(selectedSourceId) {
        selectedSourceId?.let { id ->
            launchers[id]?.invoke()
            selectedSourceId = null
        }
    }

    if (showImagePicker) {
        ImagePicker(
            onDismiss = { showImagePicker = false },
            onSourceSelected = { source ->
                showImagePicker = false
                selectedSourceId = source.id
            },
            imageSources = imageSources,
        )
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = { showImagePicker = true },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = "Add Images")
        }

        if (imageUris.isNotEmpty()) {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(imageUris.size) { index ->
                    Box(modifier = Modifier.size(100.dp)) {
                        AsyncImage(
                            model = imageUris[index],
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                        )
                        IconButton(
                            onClick = {
                                val updatedUris = imageUris.toMutableList().apply { removeAt(index) }
                                imageUris = updatedUris
                                onImagesSelected(updatedUris)
                            },
                            modifier =
                                Modifier
                                    .align(Alignment.TopEnd)
                                    .size(24.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove image",
                                tint = Color.White,
                                modifier =
                                    Modifier
                                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                        .padding(4.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
