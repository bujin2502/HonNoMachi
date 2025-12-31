package hr.foi.air.honnomachi.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import hr.foi.air.honnomachi.R
import hr.foi.air.honnomachi.ui.components.BookItemView

private const val SHOW_DEBUG_BUTTON = false

@Composable
fun HomePage(
    paddingValues: PaddingValues,
    navController: NavController,
    viewModel: HomeViewModel,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp)
                .padding(bottom = paddingValues.calculateBottomPadding()),
    ) {
        TextField(
            value = uiState.searchQuery,
            onValueChange = { viewModel.onSearchQueryChange(it) },
            placeholder = { Text(stringResource(R.string.search_by_title)) },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp, bottom = 2.dp)
                    .testTag("search_field"),
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = "Search Icon")
            },
            shape = RoundedCornerShape(24.dp),
            colors =
                TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                ),
        )
        if (SHOW_DEBUG_BUTTON) {
            Button(onClick = { throw RuntimeException("Testni crash") }) {
                Text("CRASH TEST")
            }
        }

        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else if (uiState.errorMessage != null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(stringResource(R.string.error_occurred) + ": ${uiState.errorMessage}")
            }
        } else if (uiState.books.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(stringResource(R.string.no_books_available))
            }
        } else {
            val filteredBookList =
                if (uiState.searchQuery.isEmpty()) {
                    uiState.books
                } else {
                    uiState.books.filter { it.title.contains(uiState.searchQuery, ignoreCase = true) }
                }

            if (filteredBookList.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(stringResource(R.string.no_books_found))
                }
            } else {
                LazyColumn(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(top = 8.dp)
                            .testTag("book_list"),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(filteredBookList) { item ->
                        BookItemView(
                            book = item,
                            onBookClick = { bookId ->
                                bookId?.let {
                                    navController.navigate("bookDetail/$it")
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}
