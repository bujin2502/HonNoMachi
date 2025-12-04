package hr.foi.air.honnomachi.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import hr.foi.air.honnomachi.model.BookModel

@Composable
fun BookItemView(
    modifier: Modifier = Modifier,
    book: BookModel,
    onBookClick: (String?) -> Unit
) {
    Card(modifier = modifier
            .fillMaxWidth()
            .height(160.dp)
        .padding(8.dp)
            .clickable { onBookClick(book.bookId) }, shape = RoundedCornerShape(12.dp),
        colors = CardDefaults . cardColors (containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            AsyncImage(
                model = book.coverImageUrl,
                contentDescription = book.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
            Column(
                modifier = Modifier
                    .weight(2f)
                    .padding(start = 8.dp)
            ) {
                Text(
                    text = book.title,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = book.authors.joinToString(", ")
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${book.price} ${book.priceCurrency}"
                )
            }
        }
    }
}