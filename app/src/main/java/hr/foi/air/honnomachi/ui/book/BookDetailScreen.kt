package hr.foi.air.honnomachi.ui.book

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import hr.foi.air.honnomachi.R
import hr.foi.air.honnomachi.model.BookModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookDetailScreen(
    bookId: String?,
    viewModel: BookDetailViewModel = hiltViewModel(),
) {
    LaunchedEffect(key1 = bookId) {
        viewModel.loadBookDetails(bookId)
    }

    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.screen_title_book_details),
                        fontWeight = FontWeight.Bold,
                    )
                },
            )
        },
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            when (uiState) {
                is BookUiState.Loading -> {
                    BookDetailLoading()
                }

                is BookUiState.Error -> {
                    val errorMessage = (uiState as BookUiState.Error).message
                    BookDetailError(errorMessage = errorMessage)
                }

                is BookUiState.Success -> {
                    val book = (uiState as BookUiState.Success).book
                    BookDetailContent(book = book)
                }

                BookUiState.BookNotFound -> {
                    BookDetailNotFound(bookId = bookId)
                }
            }
        }
    }
}

@Composable
fun BookDetailLoading() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
fun BookDetailError(errorMessage: String) {
    Text(
        text = stringResource(id = R.string.error) + " $errorMessage",
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.error,
    )
}

@Composable
fun BookDetailNotFound(bookId: String?) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(id = R.string.book_not_found),
            fontWeight = FontWeight.Bold,
        )
        if (bookId == null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(id = R.string.error_invalid_book_id),
            )
        }
    }
}

@Composable
fun BookDetailContent(book: BookModel) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.Start,
    ) {
        // odnosi se na slike
        val images = book.imageUrls.orEmpty()
        val imageRatio = 180f / 260f

        @OptIn(ExperimentalFoundationApi::class)
        if (images.isNotEmpty()) {
            // Stanje pagera prati koja je slika trenutno aktivna
            val pagerState = rememberPagerState(pageCount = { images.size })

            HorizontalPager(
                state = pagerState,
                modifier =
                    Modifier
                        .fillMaxWidth(0.6f)
                        .aspectRatio(imageRatio)
                        .align(Alignment.CenterHorizontally),
            ) { pageIndex ->
                AsyncImage(
                    model = images[pageIndex],
                    contentDescription = stringResource(id = R.string.content_description_book_image, pageIndex + 1),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Indikator trenutne slike
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                images.forEachIndexed { index, _ ->
                    Box(
                        modifier =
                            Modifier
                                .padding(horizontal = 4.dp)
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (index == pagerState.currentPage) MaterialTheme.colorScheme.primary else Color.Gray,
                                ),
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        } else {
            Text(
                text = stringResource(id = R.string.no_images_available),
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(24.dp),
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
        // kraj slike

        Spacer(modifier = Modifier.height(24.dp))

        // Redak koji sadrzi dva stupca
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            // lijevi stupac
            Column(
                modifier = Modifier.weight(0.5f),
            ) {
                Text(text = stringResource(id = R.string.label_title), fontWeight = FontWeight.Bold)
                Text(text = book.title, style = MaterialTheme.typography.bodyLarge)
                Text(text = stringResource(id = R.string.label_author), fontWeight = FontWeight.Bold)
                Text(text = book.authors.joinToString(", "), style = MaterialTheme.typography.bodyLarge)
                Text(text = stringResource(id = R.string.label_publisher), fontWeight = FontWeight.Bold)
                Text(text = book.publisher, style = MaterialTheme.typography.bodyLarge)
                Text(text = stringResource(id = R.string.label_year), fontWeight = FontWeight.Bold)
                Text(text = book.publicationYear.toString(), style = MaterialTheme.typography.bodyLarge)
            }

            // desni stupac
            Column(
                modifier = Modifier.weight(0.5f),
                horizontalAlignment = Alignment.End,
            ) {
                Text(text = stringResource(id = R.string.label_genre), fontWeight = FontWeight.Bold)
                Text(text = stringResource(id = book.genre.resourceId), style = MaterialTheme.typography.bodyLarge)
                Text(text = stringResource(id = R.string.label_condition), fontWeight = FontWeight.Bold)
                Text(text = stringResource(id = book.condition.resourceId), style = MaterialTheme.typography.bodyLarge)
                Text(text = stringResource(id = R.string.label_price), fontWeight = FontWeight.Bold)
                Text(text = "${book.price} ${book.priceCurrency}", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { /* TODO: Implementirat dodavanje u kosaricu */ },
            modifier =
                Modifier
                    .fillMaxWidth(0.6f)
                    .align(Alignment.CenterHorizontally),
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = colorResource(id = R.color.blue),
                ),
        ) {
            Text(stringResource(id = R.string.button_add_to_cart), color = colorResource(id = R.color.black))
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(text = stringResource(id = R.string.label_description), fontWeight = FontWeight.Bold)
        Text(text = book.description, style = MaterialTheme.typography.bodyLarge)
    }
}
