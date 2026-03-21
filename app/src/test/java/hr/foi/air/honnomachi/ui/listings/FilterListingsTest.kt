package hr.foi.air.honnomachi.ui.listings

import hr.foi.air.honnomachi.model.BookModel
import hr.foi.air.honnomachi.model.ItemStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class FilterListingsTest {
    private val listings =
        listOf(
            BookModel(bookId = "1", title = "Available Book", status = ItemStatus.AVAILABLE),
            BookModel(bookId = "2", title = "Inactive Book", status = ItemStatus.INACTIVE),
            BookModel(bookId = "3", title = "Sold Book", status = ItemStatus.SOLD),
            BookModel(bookId = "4", title = "Another Available", status = ItemStatus.AVAILABLE),
        )

    @Test
    fun `filterListings ALL returns all listings`() {
        val result = filterListings(listings, ListingsFilter.ALL)

        assertEquals(4, result.size)
    }

    @Test
    fun `filterListings ACTIVE returns only available listings`() {
        val result = filterListings(listings, ListingsFilter.ACTIVE)

        assertEquals(2, result.size)
        result.forEach { assertEquals(ItemStatus.AVAILABLE, it.status) }
    }

    @Test
    fun `filterListings INACTIVE returns only inactive listings`() {
        val result = filterListings(listings, ListingsFilter.INACTIVE)

        assertEquals(1, result.size)
        assertEquals(ItemStatus.INACTIVE, result[0].status)
    }

    @Test
    fun `filterListings ALL with empty list returns empty`() {
        val result = filterListings(emptyList(), ListingsFilter.ALL)

        assertEquals(0, result.size)
    }

    @Test
    fun `filterListings ACTIVE with no available books returns empty`() {
        val inactiveOnly =
            listOf(
                BookModel(bookId = "1", title = "Inactive", status = ItemStatus.INACTIVE),
                BookModel(bookId = "2", title = "Sold", status = ItemStatus.SOLD),
            )

        val result = filterListings(inactiveOnly, ListingsFilter.ACTIVE)

        assertEquals(0, result.size)
    }
}
