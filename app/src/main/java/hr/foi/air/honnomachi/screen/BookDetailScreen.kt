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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookDetailScreen(bookId: String?) {

    val bookState = remember { mutableStateOf<BookModel?>(null) }
    val isLoading = remember { mutableStateOf(true) }
    val errorMessage = remember { mutableStateOf<String?>(null) }

    LaunchedEffect(key1 = bookId) {
        if (bookId == null) {
            errorMessage.value = "Nevažeći ID knjige."
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
                errorMessage.value = "Greška pri dohvaćanju detalja: ${it.message}"
                isLoading.value = false
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(
                    text = "Detalji knjige",
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
                    Text(text = "Greška: ${errorMessage.value}", modifier = Modifier.align(Alignment.Center))
                }
                bookState.value != null -> {
                    BookDetailContent(book = bookState.value!!)
                }
                else -> {
                    Text(text = "Knjiga nije pronađena.", modifier = Modifier.align(Alignment.Center))
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
        AsyncImage(
            model = book.coverImageUrl,
            contentDescription = book.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(180.dp, 260.dp)
                .align(Alignment.CenterHorizontally)
        )

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
                Text(text = "Naslov:", fontWeight = FontWeight.Bold)
                Text(text = book.title ?: "N/A", style = MaterialTheme.typography.bodyLarge)
                Text(text = "Autor:", fontWeight = FontWeight.Bold)
                Text(text = book.authors?.joinToString(", ") ?: "N/A", style = MaterialTheme.typography.bodyLarge)
                Text(text = "Izdavač:", fontWeight = FontWeight.Bold)
                Text(text = book.publisher ?: "N/A", style = MaterialTheme.typography.bodyLarge)
                Text(text = "Godina:", fontWeight = FontWeight.Bold)
                Text(text = book.publicationYear.toString() ?: "N/A", style = MaterialTheme.typography.bodyLarge)
            }

            // desni stupac
            Column(
                modifier = Modifier.weight(0.5f),
                horizontalAlignment = Alignment.End,
            ) {
                Text(text = "Žanr:", fontWeight = FontWeight.Bold)
                Text(text = stringResource(id = book.genre.resourceId), style = MaterialTheme.typography.bodyLarge)
                Text(text = "Stanje:", fontWeight = FontWeight.Bold)
                Text(text = stringResource(id = book.condition.resourceId), style = MaterialTheme.typography.bodyLarge)
                Text(text = "Cijena:", fontWeight = FontWeight.Bold)
                Text(text = "${book.price} ${book.priceCurrency}", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { /* TODO: Implementirat dodavanje u kosaricu */ },
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .align(Alignment.CenterHorizontally)
        ) {
            Text("Dodaj u košaricu")
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(text = "Opis:", fontWeight = FontWeight.Bold)
        Text(text = book.description ?: "N/A", style = MaterialTheme.typography.bodyLarge)
    }
}