package hr.foi.air.honnomachi.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class ReservationModel(
    @DocumentId
    val reservationId: String = "",
    val bookId: String = "",
    val buyerUid: String = "",
    val sellerUid: String = "",
    val status: ReservationStatus = ReservationStatus.PENDING,
    val createdAt: Timestamp = Timestamp.now(),
    val expiresAt: Timestamp = Timestamp.now(),
    val checkoutSessionId: String? = null,
)
