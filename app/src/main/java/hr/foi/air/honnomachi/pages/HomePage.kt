package hr.foi.air.honnomachi.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import hr.foi.air.honnomachi.model.BookModel
import hr.foi.air.honnomachi.ui.components.BookItemView

import androidx.compose.foundation.layout.PaddingValues

// ...

@Composable
fun HomePage(paddingValues: PaddingValues, navController: NavController) {
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

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = paddingValues,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(bookList.value) { item ->
            BookItemView(
                book = item,
                onBookClick = { bookId ->
                    bookId?.let {
                        navController.navigate("bookDetail/$it")
                    }
                }
            )
        }
    }
}