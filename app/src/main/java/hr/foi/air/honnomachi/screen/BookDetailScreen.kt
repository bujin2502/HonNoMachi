package hr.foi.air.honnomachi.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import hr.foi.air.honnomachi.model.BookModel
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import hr.foi.air.honnomachi.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookDetailScreen(bookId: String?) {

    val bookState = remember { mutableStateOf<BookModel?>(null) }
    val isLoading = remember { mutableStateOf(true) }
    val errorMessage = remember { mutableStateOf<String?>(null) }
    val invalidBookIdError = stringResource(id = R.string.error_invalid_book_id)
    val fetchingDetailsErrorPrefix = stringResource(id = R.string.error_fetching_details)

    LaunchedEffect(key1 = bookId) {
        if (bookId == null) {
            errorMessage.value = invalidBookIdError
            isLoading.value = false
            return@LaunchedEffect
        }

        Firebase.firestore.collection("books").document(bookId)
            .get()
            .addOnSuccessListener { document ->
                bookState.value = document.toObject(BookModel::class.java)
                isLoading.value = false
            }
            .addOnFailureListener {
                errorMessage.value = "$fetchingDetailsErrorPrefix ${it.message}"
                isLoading.value = false
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(
                    text = stringResource(id = R.string.screen_title_book_details),
                    fontWeight= FontWeight.Bold)}
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            when {
                isLoading.value -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                errorMessage.value != null -> {
                    Text(text = stringResource(id = R.string.error) + {errorMessage.value}, modifier = Modifier.align(Alignment.Center))
                }
                bookState.value != null -> {
                    BookDetailContent(book = bookState.value!!)
                }
                else -> {
                    Text(text = stringResource(id = R.string.book_not_found), modifier = Modifier.align(Alignment.Center))
                }
            }
        }
    }
}

@Composable
fun BookDetailContent(book: BookModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.Start
    ) {
        //odnosi se na slike
        val images = book.imageUrls.orEmpty()
        val imageRatio = 180f / 260f

        @OptIn(ExperimentalFoundationApi::class)
        if (images.isNotEmpty()) {
            // Stanje pagera prati koja je stranica trenutno aktivna
            val pagerState = rememberPagerState(pageCount = { images.size })

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .aspectRatio(imageRatio)
                    .align(Alignment.CenterHorizontally)
            ) { pageIndex ->
                AsyncImage(
                    model = images[pageIndex],
                    contentDescription = stringResource(id = R.string.content_description_book_image, pageIndex + 1),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            //Indikator trenutne slike
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                images.forEachIndexed { index, _ ->
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                if (index == pagerState.currentPage) MaterialTheme.colorScheme.primary else Color.Gray
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

        } else {
            Text(
                text = stringResource(id = R.string.no_images_available),
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(24.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
        //kraj slike

        Spacer(modifier = Modifier.height(24.dp))

        // Redak koji sadrzi dva stupca
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // lijevi stupac
            Column(
                modifier = Modifier.weight(0.5f)
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
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .align(Alignment.CenterHorizontally),
            colors = ButtonDefaults.buttonColors(
                containerColor = colorResource(id = R.color.blue)
            )
        ) {
            Text(stringResource(id = R.string.button_add_to_cart), color = colorResource(id = R.color.black))
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(text = stringResource(id = R.string.label_description), fontWeight = FontWeight.Bold)
        Text(text = book.description, style = MaterialTheme.typography.bodyLarge)
    }
}