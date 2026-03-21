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
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import hr.foi.air.honnomachi.ui.theme.StatusSuspended
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

    val suspendedFieldColors =
        if (isSuspended) {
            OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = StatusSuspended,
                focusedBorderColor = StatusSuspended,
            )
        } else {
            null
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
                onValueChange = { if (isSuspended) showSuspendedSnackbar() else viewModel.onTitleChange(it) },
                error = formState.titleError,
                colors = suspendedFieldColors,
            )

            AuthorsField(
                value = formState.authors,
                onValueChange = { if (isSuspended) showSuspendedSnackbar() else viewModel.onAuthorsChange(it) },
                error = formState.authorsError,
                colors = suspendedFieldColors,
            )

            PriceWithCurrencyRow(
                price = formState.price,
                onPriceChange = { if (isSuspended) showSuspendedSnackbar() else viewModel.onPriceChange(it) },
                priceError = formState.priceError,
                selectedCurrency = formState.selectedCurrency,
                onCurrencyChange = { if (!isSuspended) viewModel.onCurrencyChange(it) },
                colors = suspendedFieldColors,
                isSuspended = isSuspended,
            )

            GenreField(
                selectedGenre = formState.selectedGenre,
                onGenreChange = { if (!isSuspended) viewModel.onGenreChange(it) },
                colors = suspendedFieldColors,
                isSuspended = isSuspended,
            )

            ConditionField(
                selectedCondition = formState.selectedCondition,
                onConditionChange = { if (!isSuspended) viewModel.onConditionChange(it) },
                colors = suspendedFieldColors,
                isSuspended = isSuspended,
            )

            LanguageField(
                selectedLanguage = formState.selectedLanguage,
                onLanguageChange = { if (!isSuspended) viewModel.onLanguageChange(it) },
                colors = suspendedFieldColors,
                isSuspended = isSuspended,
            )

            PublisherField(
                value = formState.publisher,
                onValueChange = { if (isSuspended) showSuspendedSnackbar() else viewModel.onPublisherChange(it) },
                colors = suspendedFieldColors,
            )

            PublicationYearField(
                value = formState.publicationYear,
                onValueChange = { if (isSuspended) showSuspendedSnackbar() else viewModel.onPublicationYearChange(it) },
                error = formState.yearError,
                colors = suspendedFieldColors,
            )

            IsbnField(
                value = formState.isbn,
                onValueChange = { if (isSuspended) showSuspendedSnackbar() else viewModel.onIsbnChange(it) },
                colors = suspendedFieldColors,
            )

            DescriptionField(
                value = formState.description,
                onValueChange = { if (isSuspended) showSuspendedSnackbar() else viewModel.onDescriptionChange(it) },
                colors = suspendedFieldColors,
            )

            Spacer(modifier = Modifier.height(AddPageDimensions.SmallSpacing))

            ImageUploaderView(
                onImagesSelected = if (isSuspended) { _ -> } else viewModel::onImageUrisChange,
                border = if (isSuspended) BorderStroke(1.dp, StatusSuspended) else null,
                isInteractionEnabled = !isSuspended,
            )

            Spacer(modifier = Modifier.height(AddPageDimensions.SmallSpacing))

            SubmitButton(
                isSubmitting = uiState == AddBookUiState.Submitting,
                onClick = {
                    if (!isSuspended) {
                        viewModel.submitForm()
                    }
                },
                border = if (isSuspended) BorderStroke(1.dp, StatusSuspended) else null,
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
    border: BorderStroke? = null,
) {
    Button(
        onClick = onClick,
        enabled = !isSubmitting,
        modifier = modifier.fillMaxWidth(),
        border = border,
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
