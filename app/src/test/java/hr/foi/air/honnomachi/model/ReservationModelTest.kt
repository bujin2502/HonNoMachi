package hr.foi.air.honnomachi.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReservationModelTest {
    @Test
    fun `default reservation is pending`() {
        val reservation =
            ReservationModel(
                bookId = "book-1",
                buyerUid = "buyer-1",
                sellerUid = "seller-1",
            )

        assertEquals(ReservationStatus.PENDING, reservation.status)
        assertNotNull(reservation.createdAt)
        assertNotNull(reservation.expiresAt)
    }

    @Test
    fun `reservation status enum contains terminal states`() {
        val statuses = ReservationStatus.values().toSet()

        assertTrue(ReservationStatus.CONFIRMED in statuses)
        assertTrue(ReservationStatus.EXPIRED in statuses)
        assertTrue(ReservationStatus.CANCELED in statuses)
    }
}
