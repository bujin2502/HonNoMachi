package hr.foi.air.honnomachi.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class CartItemModel(
    @DocumentId
    val id: String = "", // Firestore document ID, typically matches bookId
    val bookId: String = "",
    val title: String = "",
    val author: String = "",
    val price: Double = 0.0,
    val currency: String = "EUR",
    val imageUrl: String? = null,
    val addedAt: Timestamp = Timestamp.now(),
)
