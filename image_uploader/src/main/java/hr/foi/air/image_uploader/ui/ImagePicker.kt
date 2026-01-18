package hr.foi.air.image_uploader.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hr.foi.air.image_uploader.R
import hr.foi.air.image_uploader.model.ImageSource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImagePicker(
    onDismiss: () -> Unit,
    onSourceSelected: (ImageSource) -> Unit,
    imageSources: List<ImageSource>
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier =
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(stringResource(R.string.choose_image_source), fontSize = 20.sp, modifier = Modifier.padding(bottom = 16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                imageSources.forEach { source ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { onSourceSelected(source) },
                    ) {
                        Icon(
                            imageVector = source.icon,
                            contentDescription = stringResource(id = source.nameResId),
                            modifier = Modifier.size(48.dp),
                        )
                        Text(text = stringResource(id = source.nameResId))
                    }
                }
            }
        }
    }
}
