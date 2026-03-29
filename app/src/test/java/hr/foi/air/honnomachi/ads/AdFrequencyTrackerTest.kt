package hr.foi.air.honnomachi.ads

import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AdFrequencyTrackerTest {
    private val storedValues = mutableMapOf<String, String?>()
    private lateinit var prefs: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var tracker: AdFrequencyTrackerImpl

    @Before
    fun setup() {
        storedValues.clear()

        editor =
            mockk<SharedPreferences.Editor>(relaxed = true).apply {
                val keySlot = slot<String>()
                val valueSlot = slot<String>()
                every { putString(capture(keySlot), capture(valueSlot)) } answers {
                    storedValues[keySlot.captured] = valueSlot.captured
                    this@apply
                }
                every { apply() } returns Unit
            }

        prefs =
            mockk<SharedPreferences>().apply {
                every { getString(any(), any()) } answers {
                    storedValues[firstArg()] ?: secondArg()
                }
                every { edit() } returns editor
            }

        tracker = AdFrequencyTrackerImpl(prefs)
    }

    @Test
    fun `canShowAd returns true when no impressions recorded`() {
        assertTrue(tracker.canShowAd())
    }

    @Test
    fun `canShowAd returns true when under limit`() {
        repeat(3) { tracker.recordImpression() }
        assertTrue(tracker.canShowAd())
    }

    @Test
    fun `canShowAd returns false when limit reached`() {
        repeat(4) { tracker.recordImpression() }
        assertFalse(tracker.canShowAd())
    }

    @Test
    fun `getImpressionCount returns zero initially`() {
        assertEquals(0, tracker.getImpressionCount())
    }

    @Test
    fun `recordImpression increments count`() {
        tracker.recordImpression()
        assertEquals(1, tracker.getImpressionCount())
        tracker.recordImpression()
        assertEquals(2, tracker.getImpressionCount())
    }

    @Test
    fun `old impressions beyond 24h are ignored`() {
        val oldTimestamp = System.currentTimeMillis() - (25 * 60 * 60 * 1000L)
        storedValues["ad_impression_timestamps"] = oldTimestamp.toString()

        assertTrue(tracker.canShowAd())
        assertEquals(0, tracker.getImpressionCount())
    }

    @Test
    fun `recordImpression persists to SharedPreferences`() {
        tracker.recordImpression()

        verify { editor.putString(eq("ad_impression_timestamps"), any()) }
        verify { editor.apply() }
    }

    @Test
    fun `mixed old and recent impressions only count recent`() {
        val oldTimestamp = System.currentTimeMillis() - (25 * 60 * 60 * 1000L)
        val recentTimestamp = System.currentTimeMillis() - (1 * 60 * 60 * 1000L)
        storedValues["ad_impression_timestamps"] = "$oldTimestamp,$recentTimestamp"

        assertTrue(tracker.canShowAd())
        assertEquals(1, tracker.getImpressionCount())
    }
}
