package hr.foi.air.honnomachi.data

import com.google.firebase.functions.FirebaseFunctions
import hr.foi.air.honnomachi.CrashlyticsManager
import hr.foi.air.honnomachi.util.Result
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class CheckoutSessionModel(
    val sessionId: String,
    val checkoutUrl: String,
    val expiresAt: String?,
    val reservationIds: List<String>,
)

interface CheckoutRepository {
    suspend fun createCheckoutSession(
        successUrl: String,
        cancelUrl: String,
    ): Result<CheckoutSessionModel>
}

class CheckoutRepositoryImpl
    @Inject
    constructor(
        private val functions: FirebaseFunctions,
    ) : CheckoutRepository {
        override suspend fun createCheckoutSession(
            successUrl: String,
            cancelUrl: String,
        ): Result<CheckoutSessionModel> {
            return try {
                val payload =
                    mapOf(
                        "successUrl" to successUrl,
                        "cancelUrl" to cancelUrl,
                    )

                val callableResult =
                    functions
                        .getHttpsCallable(CREATE_CHECKOUT_SESSION_FUNCTION)
                        .call(payload)
                        .await()

                val responseData = callableResult.data as? Map<*, *>
                val sessionId = responseData?.get("sessionId") as? String
                val checkoutUrl = responseData?.get("checkoutUrl") as? String

                if (sessionId.isNullOrBlank() || checkoutUrl.isNullOrBlank()) {
                    Result.Error(Exception("Neispravan odgovor backend servisa za Stripe naplatu."))
                } else {
                    val expiresAt = responseData["expiresAt"] as? String
                    val reservationIds =
                        (responseData["reservationIds"] as? List<*>)
                            ?.mapNotNull { it as? String }
                            .orEmpty()

                    Result.Success(
                        CheckoutSessionModel(
                            sessionId = sessionId,
                            checkoutUrl = checkoutUrl,
                            expiresAt = expiresAt,
                            reservationIds = reservationIds,
                        ),
                    )
                }
            } catch (e: Exception) {
                CrashlyticsManager.instance.logException(e)
                Result.Error(e)
            }
        }

        companion object {
            private const val CREATE_CHECKOUT_SESSION_FUNCTION = "createCheckoutSession"
        }
    }
