package hr.foi.air.honnomachi.ads

import android.content.SharedPreferences
import javax.inject.Inject
import javax.inject.Named

interface AdFrequencyTracker {
    fun canShowAd(): Boolean

    fun recordImpression()

    fun getImpressionCount(): Int
}

class AdFrequencyTrackerImpl
    @Inject
    constructor(
        @Named("adPrefs") private val prefs: SharedPreferences,
    ) : AdFrequencyTracker {
        companion object {
            private const val KEY_IMPRESSIONS = "ad_impression_timestamps"
            private const val MAX_IMPRESSIONS = 4
            private const val WINDOW_MILLIS = 24 * 60 * 60 * 1000L
        }

        override fun canShowAd(): Boolean = getRecentTimestamps().size < MAX_IMPRESSIONS

        override fun recordImpression() {
            val timestamps = getRecentTimestamps().toMutableList()
            timestamps.add(System.currentTimeMillis())
            saveTimestamps(timestamps)
        }

        override fun getImpressionCount(): Int = getRecentTimestamps().size

        private fun getRecentTimestamps(): List<Long> {
            val raw = prefs.getString(KEY_IMPRESSIONS, null) ?: return emptyList()
            val cutoff = System.currentTimeMillis() - WINDOW_MILLIS
            return raw
                .split(",")
                .mapNotNull { it.toLongOrNull() }
                .filter { it > cutoff }
        }

        private fun saveTimestamps(timestamps: List<Long>) {
            prefs
                .edit()
                .putString(KEY_IMPRESSIONS, timestamps.joinToString(","))
                .apply()
        }
    }
