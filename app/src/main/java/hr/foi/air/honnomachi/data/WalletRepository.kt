package hr.foi.air.honnomachi.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import hr.foi.air.honnomachi.CrashlyticsManager
import hr.foi.air.honnomachi.util.Result
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Locale
import javax.inject.Inject

data class WalletTopupIntentModel(
    val topupId: String,
    val paymentIntentId: String,
    val clientSecret: String,
    val amountMinor: Int,
    val currency: String,
)

data class WalletBalanceModel(
    val balanceMinor: Int,
    val currency: String,
)

enum class WalletTopupStatus {
    PENDING,
    SUCCEEDED,
    FAILED,
    CANCELED,
    REFUNDED,
    UNKNOWN,
}

data class WalletTopupModel(
    val topupId: String,
    val status: WalletTopupStatus,
    val failureMessage: String?,
)

interface WalletRepository {
    suspend fun createWalletTopupIntent(
        amountMinor: Int,
        currency: String,
        idempotencyKey: String,
    ): Result<WalletTopupIntentModel>

    fun observeWalletBalance(): Flow<Result<WalletBalanceModel>>

    fun observeTopupStatus(topupId: String): Flow<Result<WalletTopupModel>>
}

class WalletRepositoryImpl
    @Inject
    constructor(
        private val auth: FirebaseAuth,
        private val firestore: FirebaseFirestore,
        private val functions: FirebaseFunctions,
    ) : WalletRepository {
        override suspend fun createWalletTopupIntent(
            amountMinor: Int,
            currency: String,
            idempotencyKey: String,
        ): Result<WalletTopupIntentModel> {
            return try {
                if (auth.currentUser == null) {
                    return Result.Error(Exception("Korisnik nije prijavljen."))
                }

                val payload =
                    mapOf(
                        "amountMinor" to amountMinor,
                        "currency" to currency.lowercase(Locale.ROOT),
                        "idempotencyKey" to idempotencyKey,
                    )

                val callableResult =
                    functions
                        .getHttpsCallable(CREATE_WALLET_TOPUP_FUNCTION)
                        .call(payload)
                        .await()

                val responseData = callableResult.data as? Map<*, *>
                val topupId = responseData?.get("topupId") as? String
                val paymentIntentId = responseData?.get("paymentIntentId") as? String
                val clientSecret = responseData?.get("clientSecret") as? String
                val resolvedAmountMinor = (responseData?.get("amountMinor") as? Number)?.toInt()
                val resolvedCurrency = (responseData?.get("currency") as? String)?.lowercase(Locale.ROOT)

                if (
                    topupId.isNullOrBlank() ||
                    paymentIntentId.isNullOrBlank() ||
                    clientSecret.isNullOrBlank() ||
                    resolvedAmountMinor == null ||
                    resolvedAmountMinor <= 0 ||
                    resolvedCurrency.isNullOrBlank()
                ) {
                    Result.Error(Exception("Neispravan odgovor backend servisa za wallet topup."))
                } else {
                    Result.Success(
                        WalletTopupIntentModel(
                            topupId = topupId,
                            paymentIntentId = paymentIntentId,
                            clientSecret = clientSecret,
                            amountMinor = resolvedAmountMinor,
                            currency = resolvedCurrency,
                        ),
                    )
                }
            } catch (e: Exception) {
                CrashlyticsManager.instance.logException(e)
                Result.Error(e)
            }
        }

        override fun observeWalletBalance(): Flow<Result<WalletBalanceModel>> =
            callbackFlow {
                val currentUser = auth.currentUser
                if (currentUser == null) {
                    trySend(Result.Error(Exception("Korisnik nije prijavljen.")))
                    close()
                    return@callbackFlow
                }

                val listener =
                    firestore
                        .collection("wallets")
                        .document(currentUser.uid)
                        .addSnapshotListener { snapshot, error ->
                            if (error != null) {
                                CrashlyticsManager.instance.logException(error)
                                trySend(Result.Error(error))
                                return@addSnapshotListener
                            }

                            val data = snapshot?.data.orEmpty()
                            val balanceMinor = (data["balanceMinor"] as? Number)?.toInt() ?: 0
                            val currency =
                                (data["currency"] as? String)
                                    ?.lowercase(Locale.ROOT)
                                    ?: DEFAULT_CURRENCY

                            trySend(
                                Result.Success(
                                    WalletBalanceModel(
                                        balanceMinor = balanceMinor,
                                        currency = currency,
                                    ),
                                ),
                            )
                        }

                awaitClose { listener.remove() }
            }

        override fun observeTopupStatus(topupId: String): Flow<Result<WalletTopupModel>> =
            callbackFlow {
                if (topupId.isBlank()) {
                    trySend(Result.Error(Exception("Topup ID je prazan.")))
                    close()
                    return@callbackFlow
                }

                val currentUser = auth.currentUser
                if (currentUser == null) {
                    trySend(Result.Error(Exception("Korisnik nije prijavljen.")))
                    close()
                    return@callbackFlow
                }

                val listener =
                    firestore
                        .collection("walletTopups")
                        .document(topupId)
                        .addSnapshotListener { snapshot, error ->
                            if (error != null) {
                                CrashlyticsManager.instance.logException(error)
                                trySend(Result.Error(error))
                                return@addSnapshotListener
                            }

                            if (snapshot == null || !snapshot.exists()) {
                                trySend(Result.Error(Exception("Wallet topup nije pronađen.")))
                                return@addSnapshotListener
                            }

                            val data = snapshot.data.orEmpty()
                            val userId = data["userId"] as? String
                            if (!userId.isNullOrBlank() && userId != currentUser.uid) {
                                trySend(Result.Error(Exception("Topup ne pripada prijavljenom korisniku.")))
                                return@addSnapshotListener
                            }

                            val statusRaw = (data["status"] as? String).orEmpty()
                            val failureMessage = data["failureMessage"] as? String
                            trySend(
                                Result.Success(
                                    WalletTopupModel(
                                        topupId = snapshot.id,
                                        status = statusRaw.toWalletTopupStatus(),
                                        failureMessage = failureMessage,
                                    ),
                                ),
                            )
                        }

                awaitClose { listener.remove() }
            }

        companion object {
            private const val CREATE_WALLET_TOPUP_FUNCTION = "createWalletTopupIntent"
            private const val DEFAULT_CURRENCY = "eur"
        }
    }

private fun String.toWalletTopupStatus(): WalletTopupStatus =
    when (uppercase(Locale.ROOT)) {
        "PENDING" -> WalletTopupStatus.PENDING
        "SUCCEEDED" -> WalletTopupStatus.SUCCEEDED
        "FAILED" -> WalletTopupStatus.FAILED
        "CANCELED" -> WalletTopupStatus.CANCELED
        "REFUNDED" -> WalletTopupStatus.REFUNDED
        else -> WalletTopupStatus.UNKNOWN
    }
