package hr.foi.air.honnomachi

import android.app.Application
import com.stripe.android.PaymentConfiguration
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class HonNoMachiApp : Application() {
    override fun onCreate() {
        super.onCreate()
        PaymentConfiguration.init(this, BuildConfig.STRIPE_PUBLISHABLE_KEY)
    }
}
