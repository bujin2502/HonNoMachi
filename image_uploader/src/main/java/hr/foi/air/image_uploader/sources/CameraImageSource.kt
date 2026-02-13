package hr.foi.air.image_uploader.sources

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import hr.foi.air.image_uploader.R
import hr.foi.air.image_uploader.model.ImageSource
import java.io.File

class CameraImageSource : ImageSource {
    override val id: String = "camera"
    override val nameResId: Int = R.string.camera
    override val icon: ImageVector = Icons.Default.CameraAlt

    @OptIn(ExperimentalPermissionsApi::class)
    @Composable
    override fun rememberLauncher(onImageSelected: (List<Uri>) -> Unit): () -> Unit {
        val context = LocalContext.current
        val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

        var tempImageUri by rememberSaveable { mutableStateOf<Uri?>(null) }

        val cameraLauncher =
            rememberLauncherForActivityResult(
                contract = ActivityResultContracts.TakePicture(),
                onResult = { success ->
                    if (success && tempImageUri != null) {
                        onImageSelected(listOf(tempImageUri!!))
                        tempImageUri = null
                    }
                },
            )

        return {
            if (cameraPermissionState.status.isGranted) {
                val tmpFile =
                    File.createTempFile("tmp_image_file", ".png", context.cacheDir).apply {
                        createNewFile()
                    }
                val uri = FileProvider.getUriForFile(context, "hr.foi.air.image_uploader.provider", tmpFile)
                tempImageUri = uri
                cameraLauncher.launch(uri)
            } else {
                cameraPermissionState.launchPermissionRequest()
            }
        }
    }
}
