package hr.foi.air.honnomachi.pages

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
import hr.foi.air.honnomachi.model.BookModel
import hr.foi.air.honnomachi.ui.components.BookItemView
import hr.foi.air.honnomachi.viewmodel.HomeViewModel

sealed interface BookListState {
    object Loading : BookListState

    data class Success(
        val books: List<BookModel>,
    ) : BookListState

    object Empty : BookListState

    data class Error(
        val message: String,
    ) : BookListState
}

@Composable
fun HomePage(
    paddingValues: PaddingValues,
    navController: NavController,
    viewModel: HomeViewModel,
) {
    val bookListState by viewModel.bookListState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp)
                .padding(bottom = paddingValues.calculateBottomPadding()),
    ) {
        TextField(
            value = searchQuery,
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

        when (val currentState = bookListState) {
            BookListState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
            is BookListState.Success -> {
                val filteredBookList =
                    if (searchQuery.isEmpty()) {
                        currentState.books
                    } else {
                        currentState.books.filter { it.title.contains(searchQuery, ignoreCase = true) }
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
            BookListState.Empty -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(stringResource(R.string.no_books_available))
                }
            }
            is BookListState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(stringResource(R.string.error_occurred) + ": ${currentState.message}")
                }
            }
        }
    }
}
