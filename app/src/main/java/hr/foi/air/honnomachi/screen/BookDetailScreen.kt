package hr.foi.air.honnomachi.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import hr.foi.air.honnomachi.model.BookModel

@Composable
fun BookDetailScreen(bookId: String?) {
    val bookState = remember { mutableStateOf<BookModel?>(null) }

    LaunchedEffect(bookId) {
        bookId?.let {
            Firebase.firestore.collection("books").document(it)
                .get()
                .addOnSuccessListener { document ->
                    bookState.value = document.toObject(BookModel::class.java)
                }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        bookState.value?.let { book ->
            Text(text = "Detalji knjige:\n" + book.title)
        } ?: Text(text = "Loading book details...")
    }
}
