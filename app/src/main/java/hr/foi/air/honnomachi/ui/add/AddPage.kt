package hr.foi.air.honnomachi.ui.add

import android.Manifest
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import hr.foi.air.honnomachi.AppUtil
import hr.foi.air.honnomachi.R
import hr.foi.air.honnomachi.model.BookCondition
import hr.foi.air.honnomachi.model.BookGenre
import hr.foi.air.honnomachi.model.BookModel
import hr.foi.air.honnomachi.model.Currency
import hr.foi.air.honnomachi.model.Language
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun AddPage(
    paddingValues: PaddingValues,
    viewModel: AddBookViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    var title by rememberSaveable { mutableStateOf("") }
    var authors by rememberSaveable { mutableStateOf("") }
    var price by rememberSaveable { mutableStateOf("") }
    var publisher by rememberSaveable { mutableStateOf("") }
    var publicationYear by rememberSaveable { mutableStateOf("") }
    var isbn by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    // Note: imageUris uses remember() instead of rememberSaveable() because Uri objects
    // are not easily serializable. Images are temporary until form submission.
    var imageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }

    var selectedGenre by rememberSaveable { mutableStateOf(BookGenre.OTHER) }
    var selectedCondition by rememberSaveable { mutableStateOf(BookCondition.USED) }
    var selectedLanguage by rememberSaveable { mutableStateOf(Language.HR) }
    var selectedCurrency by rememberSaveable { mutableStateOf(Currency.EUR) }

    var titleError by remember { mutableStateOf<Int?>(null) }
    var authorsError by remember { mutableStateOf<Int?>(null) }
    var priceError by remember { mutableStateOf<Int?>(null) }
    var yearError by remember { mutableStateOf<Int?>(null) }

    var showImagePicker by remember { mutableStateOf(false) }

    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    var tempImageUri by remember { mutableStateOf<Uri?>(null) }

    val galleryLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetMultipleContents(),
            onResult = { uris ->
                imageUris = imageUris + uris
            },
        )

    val cameraLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.TakePicture(),
            onResult = { success ->
                if (success) {
                    val uri = tempImageUri
                    if (uri != null) {
                        imageUris = imageUris + uri
                        tempImageUri = null
                    }
                }
            },
        )

    fun getTmpFileUri(context: Context): Uri {
        val tmpFile =
            File.createTempFile("tmp_image_file", ".png", context.cacheDir).apply {
                createNewFile()
                deleteOnExit()
            }

        return FileProvider.getUriForFile(context, "hr.foi.air.honnomachi.provider", tmpFile)
    }

    fun resetForm() {
        title = ""
        authors = ""
        price = ""
        publisher = ""
        publicationYear = ""
        isbn = ""
        description = ""
        imageUris = emptyList()
        selectedGenre = BookGenre.OTHER
        selectedCondition = BookCondition.USED
        selectedLanguage = Language.HR
        selectedCurrency = Currency.EUR
        titleError = null
        authorsError = null
        priceError = null
        yearError = null
    }

    LaunchedEffect(uiState) {
        when (val state = uiState) {
            AddBookUiState.Success -> {
                AppUtil.showToast(context, R.string.message_offer_created)
                resetForm()
                viewModel.resetState()
            }
            is AddBookUiState.Error -> {
                AppUtil.showToast(context, state.message)
            }
            else -> Unit
        }
    }

    if (showImagePicker) {
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
        )
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        Text(
            text = stringResource(R.string.add_page),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 12.dp),
        )

        OutlinedTextField(
            value = title,
            onValueChange = {
                title = it
                titleError = null
            },
            label = { Text(stringResource(R.string.field_title)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = titleError != null,
            supportingText = {
                titleError?.let {
                    Text(
                        text = stringResource(it),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
        )

        OutlinedTextField(
            value = authors,
            onValueChange = {
                authors = it
                authorsError = null
            },
            label = { Text(stringResource(R.string.field_authors)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            isError = authorsError != null,
            supportingText = {
                val error = authorsError
                if (error != null) {
                    Text(
                        text = stringResource(error),
                        color = MaterialTheme.colorScheme.error,
                    )
                } else {
                    Text(text = stringResource(R.string.hint_authors))
                }
            },
        )

        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = price,
                onValueChange = {
                    price = it
                    priceError = null
                },
                label = { Text(stringResource(R.string.field_price)) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                keyboardOptions =
                    KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next,
                    ),
                isError = priceError != null,
                supportingText = {
                    priceError?.let {
                        Text(
                            text = stringResource(it),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                },
            )
            Spacer(modifier = Modifier.width(12.dp))
            DropdownField(
                label = stringResource(R.string.field_currency),
                options = Currency.entries,
                selectedOption = selectedCurrency,
                optionLabel = { stringResource(it.resourceId) },
                onOptionSelected = { selectedCurrency = it },
                modifier = Modifier.weight(1f),
            )
        }

        DropdownField(
            label = stringResource(R.string.field_genre),
            options = BookGenre.entries,
            selectedOption = selectedGenre,
            optionLabel = { stringResource(it.resourceId) },
            onOptionSelected = { selectedGenre = it },
        )

        DropdownField(
            label = stringResource(R.string.field_condition),
            options = BookCondition.entries,
            selectedOption = selectedCondition,
            optionLabel = { stringResource(it.resourceId) },
            onOptionSelected = { selectedCondition = it },
        )

        DropdownField(
            label = stringResource(R.string.field_language),
            options = Language.entries,
            selectedOption = selectedLanguage,
            optionLabel = { stringResource(it.resourceId) },
            onOptionSelected = { selectedLanguage = it },
        )

        OutlinedTextField(
            value = publisher,
            onValueChange = { publisher = it },
            label = { Text(stringResource(R.string.field_publisher)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        OutlinedTextField(
            value = publicationYear,
            onValueChange = {
                publicationYear = it
                yearError = null
            },
            label = { Text(stringResource(R.string.field_publication_year)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions =
                KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next,
                ),
            isError = yearError != null,
            supportingText = {
                yearError?.let {
                    Text(
                        text = stringResource(it),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
        )

        OutlinedTextField(
            value = isbn,
            onValueChange = { isbn = it },
            label = { Text(stringResource(R.string.field_isbn)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text(stringResource(R.string.field_description)) },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(120.dp),
        )

        Spacer(modifier = Modifier.height(4.dp))

        OutlinedButton(
            onClick = { showImagePicker = true },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = stringResource(R.string.add_images))
        }

        if (imageUris.isNotEmpty()) {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(imageUris) { uri ->
                    AsyncImage(
                        model = uri,
                        contentDescription = null,
                        modifier = Modifier.size(100.dp),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        Button(
            onClick = {
                val cleanedTitle = title.trim()
                val cleanedAuthors =
                    authors
                        .split(",")
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                val priceValue = price.trim().replace(',', '.').toDoubleOrNull()
                val yearValue = publicationYear.trim().toIntOrNull()

                var isValid = true
                if (cleanedTitle.isBlank()) {
                    titleError = R.string.error_title_required
                    isValid = false
                }
                if (cleanedAuthors.isEmpty()) {
                    authorsError = R.string.error_author_required
                    isValid = false
                }
                if (price.isBlank()) {
                    priceError = R.string.error_price_required
                    isValid = false
                } else if (priceValue == null || priceValue <= 0.0) {
                    priceError = R.string.error_price_invalid
                    isValid = false
                }
                if (publicationYear.isNotBlank() && yearValue == null) {
                    yearError = R.string.error_year_invalid
                    isValid = false
                }

                if (!isValid || priceValue == null) {
                    return@Button
                }

                val newBook =
                    BookModel(
                        title = cleanedTitle,
                        authors = cleanedAuthors,
                        price = priceValue,
                        priceCurrency = selectedCurrency,
                        description = description.trim(),
                        publisher = publisher.trim(),
                        publicationYear = yearValue ?: 0,
                        isbn13 = isbn.trim(),
                        genre = selectedGenre,
                        condition = selectedCondition,
                        language = selectedLanguage,
                        imageUrls = emptyList(),
                    )
                viewModel.uploadImagesAndCreateListing(imageUris, newBook)
            },
            enabled = uiState != AddBookUiState.Submitting,
            modifier = Modifier.fillMaxWidth(),
        ) {
            val buttonText =
                if (uiState == AddBookUiState.Submitting) {
                    stringResource(R.string.adding_offer)
                } else {
                    stringResource(R.string.button_publish_offer)
                }
            Text(text = buttonText)
        }

        if (uiState is AddBookUiState.Error) {
            Text(
                text = (uiState as AddBookUiState.Error).message,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = 16.dp),
            )
        } else {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> DropdownField(
    label: String,
    options: List<T>,
    selectedOption: T,
    optionLabel: @Composable (T) -> String,
    onOptionSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = optionLabel(selectedOption),
            onValueChange = {},
            label = { Text(label) },
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true),
            singleLine = true,
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(optionLabel(option)) },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    },
                )
            }
        }
    }
}
