package hr.foi.air.honnomachi.pages

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import hr.foi.air.honnomachi.model.BookModel

@Composable
fun HomePage(modifier: Modifier = Modifier) {
    val bookList = remember {
        mutableStateOf<List<BookModel>>(emptyList())
    }

    LaunchedEffect(key1 = Unit) {
        Firebase.firestore.collection("books")
            .get().addOnSuccessListener {
                val resultList = it.documents.mapNotNull { doc ->
                    doc.toObject(BookModel::class.java)
                }
                bookList.value = resultList
            }
    }

    LazyColumn (
        modifier = modifier.fillMaxSize()
            .padding(25.dp)
    ) {
        items(bookList.value) { item ->
            Text(text = item.title + " --- " + item.author + "\n" + item.price + " " + item.priceCurrency)
            Spacer(modifier = Modifier.height(50.dp))
        }
    }
}