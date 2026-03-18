package hr.foi.air.honnomachi.ui.listings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import hr.foi.air.honnomachi.R
import hr.foi.air.honnomachi.model.BookModel
import hr.foi.air.honnomachi.model.ItemStatus

@Composable
internal fun MyListingsContent(
    modifier: Modifier = Modifier,
    @Suppress("DEPRECATION")
    viewModel: MyListingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.onSnackbarDismissed()
        }
    }

    if (uiState.bookToDelete != null) {
        DeleteConfirmationDialog(
            bookTitle = uiState.bookToDelete?.title ?: "",
            onConfirm = viewModel::onConfirmDelete,
            onDismiss = viewModel::onDismissDelete,
        )
    }

    Box(modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize()) {
            FilterRow(
                selectedFilter = uiState.selectedFilter,
                onFilterChange = viewModel::onFilterChange,
            )

            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
                uiState.errorMessage != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = uiState.errorMessage ?: "",
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
                else -> {
                    val filteredListings = filterListings(uiState.listings, uiState.selectedFilter)

                    if (filteredListings.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = stringResource(R.string.my_listings_empty),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(
                                items = filteredListings,
                                key = { it.bookId ?: "" },
                            ) { book ->
                                ListingCard(
                                    book = book,
                                    onToggleStatus = {
                                        book.bookId?.let { id ->
                                            viewModel.onToggleStatus(id, book.status)
                                        }
                                    },
                                    onDelete = { viewModel.onRequestDelete(book) },
                                )
                            }
                        }
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
internal fun FilterRow(
    selectedFilter: ListingsFilter,
    onFilterChange: (ListingsFilter) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ListingsFilter.entries.forEach { filter ->
            FilterChip(
                selected = filter == selectedFilter,
                onClick = { onFilterChange(filter) },
                label = {
                    Text(
                        text =
                            when (filter) {
                                ListingsFilter.ALL -> stringResource(R.string.my_listings_filter_all)
                                ListingsFilter.ACTIVE -> stringResource(R.string.my_listings_filter_active)
                                ListingsFilter.INACTIVE -> stringResource(R.string.my_listings_filter_inactive)
                            },
                    )
                },
            )
        }
    }
}

@Composable
internal fun ListingCard(
    book: BookModel,
    onToggleStatus: () -> Unit,
    onDelete: () -> Unit,
) {
    val isActive = book.status == ItemStatus.AVAILABLE
    val canToggle = book.status == ItemStatus.AVAILABLE || book.status == ItemStatus.INACTIVE
    val canDelete = book.status == ItemStatus.AVAILABLE || book.status == ItemStatus.INACTIVE

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = book.authors.joinToString(", "),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(book.status.resourceId),
                        style = MaterialTheme.typography.labelMedium,
                        color =
                            if (isActive) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outline
                            },
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "%.2f %s".format(book.price, book.priceCurrency.name),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }

            if (canToggle) {
                IconButton(onClick = onToggleStatus) {
                    Icon(
                        imageVector =
                            if (isActive) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription =
                            if (isActive) {
                                stringResource(R.string.my_listings_deactivate)
                            } else {
                                stringResource(R.string.my_listings_activate)
                            },
                    )
                }
            }

            if (canDelete) {
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.my_listings_delete),
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

@Composable
internal fun DeleteConfirmationDialog(
    bookTitle: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.my_listings_delete_title)) },
        text = {
            Text(
                text = stringResource(R.string.my_listings_delete_message, bookTitle),
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = stringResource(R.string.my_listings_delete_confirm),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.my_listings_delete_cancel))
            }
        },
    )
}

internal fun filterListings(
    listings: List<BookModel>,
    filter: ListingsFilter,
): List<BookModel> =
    when (filter) {
        ListingsFilter.ALL -> listings
        ListingsFilter.ACTIVE -> listings.filter { it.status == ItemStatus.AVAILABLE }
        ListingsFilter.INACTIVE -> listings.filter { it.status == ItemStatus.INACTIVE }
    }
