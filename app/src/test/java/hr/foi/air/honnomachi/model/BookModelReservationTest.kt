package hr.foi.air.honnomachi.model

import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BookModelReservationTest {
    @Test
    fun `default reservation fields are empty`() {
        val book = BookModel()

        assertNull(book.reservedByUid)
        assertNull(book.reservationId)
        assertNull(book.reservationExpiresAt)
        assertNull(book.soldAt)
        assertNull(book.soldToUid)
    }

    @Test
    fun `reserved status is available for books`() {
        assertTrue(ItemStatus.entries.contains(ItemStatus.RESERVED))
    }
}
