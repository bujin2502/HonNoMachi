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

// ...

@Composable
fun HomePage(paddingValues: PaddingValues, navController: NavController) {
    val bookList = remember {
        mutableStateOf<List<BookModel>>(emptyList())
    }
    var searchQuery by remember { mutableStateOf("") }


    LaunchedEffect(key1 = Unit) {
        Firebase.firestore.collection("books")
            .get().addOnSuccessListener {
                val resultList = it.documents.mapNotNull { doc ->
                    doc.toObject(BookModel::class.java)
                }
                bookList.value = resultList
            }
    }

    val filteredBookList = if (searchQuery.isEmpty()) {
        bookList.value
    } else {
        bookList.value.filter { it.title.contains(searchQuery, ignoreCase = true) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp)
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

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 8.dp),
            contentPadding = PaddingValues(bottom = paddingValues.calculateBottomPadding()),
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