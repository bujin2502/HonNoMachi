package hr.foi.air.honnomachi.ui.add

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import hr.foi.air.honnomachi.AppUtil
import hr.foi.air.honnomachi.R
import hr.foi.air.honnomachi.ui.suspension.LocalIsSuspended
import hr.foi.air.image_uploader.ui.ImageUploaderView
import kotlinx.coroutines.launch

private object AddPageDimensions {
    val HorizontalPadding = 16.dp
    val TopPadding = 12.dp
    val FieldSpacing = 12.dp
    val SmallSpacing = 4.dp
    val BottomSpacing = 16.dp
}

@Composable
fun AddPage(
    paddingValues: PaddingValues,
    @Suppress("DEPRECATION")
    viewModel: AddBookViewModel = hiltViewModel(),
) {
    val isSuspended = LocalIsSuspended.current
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val formState by viewModel.formState.collectAsState()
    val suspendedMessage = stringResource(R.string.suspended_cannot_add_book)
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val lastSnackbarTime = remember { mutableLongStateOf(0L) }
    val showSuspendedSnackbar: () -> Unit = {
        val now = System.currentTimeMillis()
        if (now - lastSnackbarTime.longValue > 4000L) {
            lastSnackbarTime.longValue = now
            scope.launch { snackbarHostState.showSnackbar(suspendedMessage) }
        }
    }

    LaunchedEffect(uiState) {
        when (val state = uiState) {
            AddBookUiState.Success -> {
                AppUtil.showToast(context, R.string.message_offer_created)
                viewModel.resetForm()
                viewModel.resetState()
            }
            is AddBookUiState.Error -> {
                AppUtil.showToast(context, state.message)
            }
            else -> Unit
        }
    }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .pointerInput(isSuspended) {
                    if (!isSuspended) return@pointerInput
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            if (event.changes.any { it.pressed && !it.previousPressed }) {
                                showSuspendedSnackbar()
                            }
                        }
                    }
                },
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = AddPageDimensions.HorizontalPadding)
                    .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(AddPageDimensions.FieldSpacing),
            horizontalAlignment = Alignment.Start,
        ) {
            Text(
                text = stringResource(R.string.add_page),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = AddPageDimensions.TopPadding),
            )

            TitleField(
                value = formState.title,
                onValueChange = viewModel::onTitleChange,
                error = formState.titleError,
            )

            AuthorsField(
                value = formState.authors,
                onValueChange = viewModel::onAuthorsChange,
                error = formState.authorsError,
            )

            PriceWithCurrencyRow(
                price = formState.price,
                onPriceChange = viewModel::onPriceChange,
                priceError = formState.priceError,
                selectedCurrency = formState.selectedCurrency,
                onCurrencyChange = viewModel::onCurrencyChange,
            )

            GenreField(
                selectedGenre = formState.selectedGenre,
                onGenreChange = viewModel::onGenreChange,
            )

            ConditionField(
                selectedCondition = formState.selectedCondition,
                onConditionChange = viewModel::onConditionChange,
            )

            LanguageField(
                selectedLanguage = formState.selectedLanguage,
                onLanguageChange = viewModel::onLanguageChange,
            )

            PublisherField(
                value = formState.publisher,
                onValueChange = viewModel::onPublisherChange,
            )

            PublicationYearField(
                value = formState.publicationYear,
                onValueChange = viewModel::onPublicationYearChange,
                error = formState.yearError,
            )

            IsbnField(
                value = formState.isbn,
                onValueChange = viewModel::onIsbnChange,
            )

            DescriptionField(
                value = formState.description,
                onValueChange = viewModel::onDescriptionChange,
            )

            Spacer(modifier = Modifier.height(AddPageDimensions.SmallSpacing))

            ImageUploaderView(onImagesSelected = viewModel::onImageUrisChange)

            Spacer(modifier = Modifier.height(AddPageDimensions.SmallSpacing))

            SubmitButton(
                isSubmitting = uiState == AddBookUiState.Submitting,
                onClick = {
                    if (!isSuspended) {
                        viewModel.submitForm()
                    }
                },
            )

            if (uiState is AddBookUiState.Error) {
                Text(
                    text = (uiState as AddBookUiState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = AddPageDimensions.BottomSpacing),
                )
            } else {
                Spacer(modifier = Modifier.height(AddPageDimensions.BottomSpacing))
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun SubmitButton(
    isSubmitting: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        enabled = !isSubmitting,
        modifier = modifier.fillMaxWidth(),
    ) {
        val buttonText =
            if (isSubmitting) {
                stringResource(R.string.adding_offer)
            } else {
                stringResource(R.string.button_publish_offer)
            }
        Text(text = buttonText)
    }
}
