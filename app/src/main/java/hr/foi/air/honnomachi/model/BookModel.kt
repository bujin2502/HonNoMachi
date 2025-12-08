package hr.foi.air.honnomachi.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class BookModel(
    @DocumentId
    val bookId: String? = null,
    val authors: List<String> = emptyList(),
    val condition: BookCondition = BookCondition.USED,
    val imageUrls: List<String>? = null,
    val genre: BookGenre = BookGenre.OTHER,
    val isbn13: String = "",
    val language: Language = Language.HR,
    val listingDate: Timestamp = Timestamp.now(),
    val price: Double = 0.0,
    val priceCurrency: Currency = Currency.EUR,
    val publicationYear: Int = 0,
    val publisher: String = "",
    val status: ItemStatus = ItemStatus.AVAILABLE,
    val title: String = "",
    val userID: String = ""
)
