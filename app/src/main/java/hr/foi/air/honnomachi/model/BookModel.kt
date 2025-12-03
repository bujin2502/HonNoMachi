package hr.foi.air.honnomachi.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class BookModel(
    @DocumentId
    val bookId: String? = null,
    val author: String = "",
    val condition: String = "",
    val coverImageUrl: String = "",
    val genre: String = "",
    val isbn13: String = "",
    val language: String = "",
    val listingDate: Timestamp = Timestamp.now(),
    val price: Double = 0.0,
    val priceCurrency: String = "",
    val publicationYear: Int = 0,
    val publisher: String = "",
    val status: ItemStatus = ItemStatus.AVAILABLE,
    val title: String = "",
    val userID: String = ""
)
