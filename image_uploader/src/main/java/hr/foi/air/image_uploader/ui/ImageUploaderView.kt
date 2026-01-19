package hr.foi.air.image_uploader.ui

import android.Manifest
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoLibrary
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import hr.foi.air.image_uploader.R
import hr.foi.air.image_uploader.model.ImageSource
import java.io.File

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ImageUploaderView(
    onImagesSelected: (List<Uri>) -> Unit
) {
    val context = LocalContext.current

    val uriListSaver = listSaver<List<Uri>, String>(
        save = { uris -> uris.map { it.toString() } },
        restore = { strings -> strings.map { Uri.parse(it) } }
    )

    val nullableUriSaver = listSaver<Uri?, String>(
        save = { uri -> listOfNotNull(uri?.toString()) },
        restore = { list -> list.firstOrNull()?.let { Uri.parse(it) } }
    )

    var imageUris by rememberSaveable(stateSaver = uriListSaver) { mutableStateOf(emptyList()) }
    var showImagePicker by rememberSaveable { mutableStateOf(false) }
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    var tempImageUri by rememberSaveable(stateSaver = nullableUriSaver) { mutableStateOf(null) }

    // Sync restored images to parent when activity is recreated
    LaunchedEffect(Unit) {
        if (imageUris.isNotEmpty()) {
            onImagesSelected(imageUris)
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents(),
        onResult = { uris ->
            val updatedUris = imageUris + uris
            imageUris = updatedUris
            onImagesSelected(updatedUris)
        }
    )

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { success ->
            if (success) {
                tempImageUri?.let {
                    val updatedUris = imageUris + it
                    imageUris = updatedUris
                    onImagesSelected(updatedUris)
                    tempImageUri = null
                }
            }
        }
    )

    fun getTmpFileUri(context: Context): Uri {
        val tmpFile = File.createTempFile("tmp_image_file", ".png", context.cacheDir).apply {
            createNewFile()
        }
        return FileProvider.getUriForFile(context, "hr.foi.air.image_uploader.provider", tmpFile)
    }

    if (showImagePicker) {
        val imageSources = listOf(
            ImageSource("camera", R.string.camera, Icons.Default.CameraAlt),
            ImageSource("gallery", R.string.gallery, Icons.Default.PhotoLibrary)
        )
        ImagePicker(
            onDismiss = { showImagePicker = false },
            onSourceSelected = { source ->
                showImagePicker = false
                when (source.id) {
                    "gallery" -> galleryLauncher.launch("image/*")
                    "camera" -> {
                        if (cameraPermissionState.status.isGranted) {
                            val uri = getTmpFileUri(context)
                            tempImageUri = uri
                            cameraLauncher.launch(uri)
                        } else {
                            cameraPermissionState.launchPermissionRequest()
                        }
                    }
                }
            },
            imageSources = imageSources
        )
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = { showImagePicker = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Add Images")
        }

        if (imageUris.isNotEmpty()) {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(imageUris.size) { index ->
                    Box(modifier = Modifier.size(100.dp)) {
                        AsyncImage(
                            model = imageUris[index],
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize()
                        )
                        IconButton(
                            onClick = {
                                val updatedUris = imageUris.toMutableList().apply { removeAt(index) }
                                imageUris = updatedUris
                                onImagesSelected(updatedUris)
                            },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove image",
                                tint = Color.White,
                                modifier = Modifier
                                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                    .padding(4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
