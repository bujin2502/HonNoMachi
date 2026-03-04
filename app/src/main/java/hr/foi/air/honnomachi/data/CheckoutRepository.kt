package hr.foi.air.honnomachi.data

import com.google.firebase.functions.FirebaseFunctions
import hr.foi.air.honnomachi.CrashlyticsManager
import hr.foi.air.honnomachi.util.Result
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class CheckoutPaymentIntentModel(
    val checkoutId: String,
    val paymentIntentId: String?,
    val clientSecret: String?,
    val amountMinor: Int,
    val totalAmountMinor: Int,
    val walletContributionMinor: Int,
    val currency: String,
    val expiresAt: String?,
    val reservationIds: List<String>,
    val requiresPaymentSheet: Boolean,
    val checkoutCompleted: Boolean,
)

interface CheckoutRepository {
    suspend fun createCheckoutPaymentIntent(
        reservationTtlMinutes: Int? = null,
    ): Result<CheckoutPaymentIntentModel>
}

class CheckoutRepositoryImpl
    @Inject
    constructor(
        private val functions: FirebaseFunctions,
    ) : CheckoutRepository {
        override suspend fun createCheckoutPaymentIntent(
            reservationTtlMinutes: Int?,
        ): Result<CheckoutPaymentIntentModel> {
            return try {
                val payload =
                    buildMap<String, Any> {
                        if (reservationTtlMinutes != null) {
                            put("reservationTtlMinutes", reservationTtlMinutes)
                        }
                    }

                val callableResult =
                    functions
                        .getHttpsCallable(CREATE_CHECKOUT_PAYMENT_INTENT_FUNCTION)
                        .call(payload)
                        .await()

                val responseData = callableResult.data as? Map<*, *>
                val checkoutId = responseData?.get("checkoutId") as? String
                val paymentIntentId = responseData?.get("paymentIntentId") as? String
                val clientSecret = responseData?.get("clientSecret") as? String
                val amountMinor = (responseData?.get("amountMinor") as? Number)?.toInt()
                val totalAmountMinor =
                    ((responseData?.get("totalAmountMinor") as? Number)?.toInt())
                        ?: amountMinor
                val walletContributionMinor =
                    ((responseData?.get("walletContributionMinor") as? Number)?.toInt())
                        ?: 0
                val requiresPaymentSheet =
                    (responseData?.get("requiresPaymentSheet") as? Boolean)
                        ?: !clientSecret.isNullOrBlank()
                val checkoutCompleted =
                    (responseData?.get("checkoutCompleted") as? Boolean)
                        ?: !requiresPaymentSheet
                val currency = responseData?.get("currency") as? String
                val expiresAt = responseData?.get("expiresAt") as? String
                val reservationIds =
                    (responseData?.get("reservationIds") as? List<*>)
                        ?.mapNotNull { it as? String }
                        .orEmpty()

                if (checkoutId.isNullOrBlank() || currency.isNullOrBlank()) {
                    Result.Error(Exception("Neispravan odgovor backend servisa za Stripe PaymentSheet."))
                } else if (amountMinor == null || amountMinor < 0) {
                    Result.Error(Exception("Neispravan odgovor backend servisa za Stripe PaymentSheet."))
                } else if (totalAmountMinor == null || totalAmountMinor <= 0) {
                    Result.Error(Exception("Neispravan odgovor backend servisa za Stripe PaymentSheet."))
                } else if (walletContributionMinor < 0) {
                    Result.Error(Exception("Neispravan odgovor backend servisa za Stripe PaymentSheet."))
                } else if (
                    requiresPaymentSheet &&
                    (
                        paymentIntentId.isNullOrBlank() ||
                            clientSecret.isNullOrBlank() ||
                            amountMinor <= 0
                    )
                ) {
                    Result.Error(Exception("Neispravan odgovor backend servisa za Stripe PaymentSheet."))
                } else {
                    Result.Success(
                        CheckoutPaymentIntentModel(
                            checkoutId = checkoutId,
                            paymentIntentId = paymentIntentId,
                            clientSecret = clientSecret,
                            amountMinor = amountMinor,
                            totalAmountMinor = totalAmountMinor,
                            walletContributionMinor = walletContributionMinor,
                            currency = currency,
                            expiresAt = expiresAt,
                            reservationIds = reservationIds,
                            requiresPaymentSheet = requiresPaymentSheet,
                            checkoutCompleted = checkoutCompleted,
                        ),
                    )
                }
            } catch (e: Exception) {
                CrashlyticsManager.instance.logException(e)
                Result.Error(e)
            }
        }

        companion object {
            private const val CREATE_CHECKOUT_PAYMENT_INTENT_FUNCTION = "createCheckoutPaymentIntent"
        }
    }
