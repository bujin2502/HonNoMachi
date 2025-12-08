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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import hr.foi.air.honnomachi.R
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Box

sealed interface BookListState {
    object Loading : BookListState
    data class Success(val books: List<BookModel>) : BookListState
    object Empty : BookListState
    data class Error(val message: String) : BookListState
}

// ...

@Composable
fun HomePage(paddingValues: PaddingValues, navController: NavController) {
    var bookListState by remember {
        mutableStateOf<BookListState>(BookListState.Loading)
    }
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(key1 = Unit) {
        Firebase.firestore.collection("books")
            .get()
            .addOnSuccessListener {
                val resultList = it.documents.mapNotNull { doc ->
                    doc.toObject(BookModel::class.java)
                }
                bookListState = if (resultList.isEmpty()) {
                    BookListState.Empty
                } else {
                    BookListState.Success(resultList)
                }
            }
            .addOnFailureListener { exception ->
                bookListState = BookListState.Error(exception.message ?: "Unknown error occurred.")
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp)
            .padding(bottom = paddingValues.calculateBottomPadding()) // Apply bottom padding to the whole column
    ) {
        TextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text(stringResource(R.string.search_by_title)) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp, bottom = 2.dp),
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = "Search Icon")
            },
            shape = RoundedCornerShape(24.dp),
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent
            )
        )

        val currentState = bookListState // Introduce a local immutable variable

        when (currentState) {
            BookListState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is BookListState.Success -> {
                val filteredBookList = if (searchQuery.isEmpty()) {
                    currentState.books
                } else {
                    currentState.books.filter { it.title.contains(searchQuery, ignoreCase = true) }
                }

                if (filteredBookList.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(stringResource(R.string.no_books_found))
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredBookList) { item ->
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
            }
            BookListState.Empty -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(stringResource(R.string.no_books_available))
                }
            }
            is BookListState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(stringResource(R.string.error_occurred) + ": ${currentState.message}")
                }
            }
        }
    }
}