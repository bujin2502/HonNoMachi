
package hr.foi.air.honnomachi.ads.di

import android.content.Context
import android.content.SharedPreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import hr.foi.air.honnomachi.BuildConfig
import hr.foi.air.honnomachi.ads.AdFrequencyTracker
import hr.foi.air.honnomachi.ads.AdFrequencyTrackerImpl
import hr.foi.air.honnomachi.ads.AdManager
import hr.foi.air.honnomachi.ads.AdManagerImpl
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AdModule {
    @Provides
    @Named("adPrefs")
    fun provideAdPreferences(
        @ApplicationContext context: Context,
    ): SharedPreferences = context.getSharedPreferences("ad_frequency", Context.MODE_PRIVATE)

    @Provides
    @Named("bannerAdUnitId")
    fun provideBannerAdUnitId(): String = BuildConfig.ADMOB_BANNER_AD_UNIT_ID

    @Provides
    @Singleton
    fun provideAdFrequencyTracker(
        @Named("adPrefs") prefs: SharedPreferences,
    ): AdFrequencyTracker = AdFrequencyTrackerImpl(prefs)

    @Provides
    @Singleton
    fun provideAdManager(
        @Named("bannerAdUnitId") adUnitId: String,
    ): AdManager = AdManagerImpl(adUnitId)
}
