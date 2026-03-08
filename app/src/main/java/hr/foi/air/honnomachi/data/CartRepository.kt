package hr.foi.air.honnomachi.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import hr.foi.air.honnomachi.CrashlyticsManager
import hr.foi.air.honnomachi.model.BookModel
import hr.foi.air.honnomachi.model.CartItemModel
import hr.foi.air.honnomachi.util.Result
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

interface CartRepository {
    suspend fun addToCart(book: BookModel): Result<Unit>

    fun getCartItems(): Flow<Result<List<CartItemModel>>>

    suspend fun removeFromCart(cartItemId: String): Result<Unit>
}

class CartRepositoryImpl
    @Inject
    constructor(
        private val auth: FirebaseAuth,
        private val firestore: FirebaseFirestore,
        private val functions: FirebaseFunctions,
    ) : CartRepository {
        override suspend fun addToCart(book: BookModel): Result<Unit> =
            try {
                val currentUser = auth.currentUser
                if (currentUser != null && book.bookId != null) {
                    currentUser.getIdToken(true).await()
                    functions
                        .getHttpsCallable("addToCartAndReserve")
                        .call(mapOf("bookId" to book.bookId))
                        .await()
                    Result.Success(Unit)
                } else {
                    Result.Error(Exception("Korisnik nije prijavljen ili knjiga nema ID."))
                }
            } catch (e: Exception) {
                CrashlyticsManager.instance.logException(e)
                Result.Error(mapFunctionsAuthError(e))
            }

        override fun getCartItems(): Flow<Result<List<CartItemModel>>> =
            callbackFlow {
                val currentUser = auth.currentUser
                if (currentUser == null) {
                    trySend(Result.Error(Exception("Korisnik nije prijavljen.")))
                    close()
                    return@callbackFlow
                }

                var latestCartItems: List<CartItemModel> = emptyList()
                val bookExpiryCache = mutableMapOf<String, Pair<com.google.firebase.Timestamp?, String?>>()
                val bookListeners = mutableMapOf<String, ListenerRegistration>()
                var previousBookIds = emptySet<String>()

                fun emitMerged() {
                    val merged =
                        latestCartItems.map { item ->
                            val cached = bookExpiryCache[item.bookId]
                            if (cached != null && cached.second == currentUser.uid) {
                                item.copy(
                                    reservationExpiresAt = cached.first,
                                    reservedByUid = cached.second,
                                )
                            } else {
                                item
                            }
                        }
                    trySend(Result.Success(merged))
                }

                val cartListener =
                    firestore
                        .collection("users")
                        .document(currentUser.uid)
                        .collection("cart")
                        .orderBy("addedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                        .addSnapshotListener { snapshot, error ->
                            if (error != null) {
                                CrashlyticsManager.instance.logException(error)
                                trySend(Result.Error(error))
                                return@addSnapshotListener
                            }

                            if (snapshot != null) {
                                latestCartItems = snapshot.toObjects(CartItemModel::class.java)
                                val newBookIds = latestCartItems.map { it.bookId }.toSet()

                                (newBookIds - previousBookIds).forEach { bookId ->
                                    val bookListener =
                                        firestore
                                            .collection("books")
                                            .document(bookId)
                                            .addSnapshotListener { bookSnap, bookErr ->
                                                if (bookErr != null) return@addSnapshotListener
                                                if (bookSnap != null) {
                                                    bookExpiryCache[bookId] =
                                                        Pair(
                                                            bookSnap.getTimestamp("reservationExpiresAt"),
                                                            bookSnap.getString("reservedByUid"),
                                                        )
                                                    emitMerged()
                                                }
                                            }
                                    bookListeners[bookId] = bookListener
                                }

                                (previousBookIds - newBookIds).forEach { bookId ->
                                    bookListeners.remove(bookId)?.remove()
                                    bookExpiryCache.remove(bookId)
                                }

                                previousBookIds = newBookIds
                                emitMerged()
                            }
                        }

                awaitClose {
                    cartListener.remove()
                    bookListeners.values.forEach { it.remove() }
                }
            }

        override suspend fun removeFromCart(cartItemId: String): Result<Unit> =
            try {
                val currentUser = auth.currentUser
                if (currentUser != null) {
                    currentUser.getIdToken(true).await()
                    functions
                        .getHttpsCallable("removeFromCartAndRelease")
                        .call(mapOf("bookId" to cartItemId))
                        .await()
                    Result.Success(Unit)
                } else {
                    Result.Error(Exception("Korisnik nije prijavljen."))
                }
            } catch (e: Exception) {
                CrashlyticsManager.instance.logException(e)
                Result.Error(mapFunctionsAuthError(e))
            }

        private fun mapFunctionsAuthError(exception: Exception): Exception {
            val functionsException = exception as? FirebaseFunctionsException ?: return exception
            return when (functionsException.code) {
                FirebaseFunctionsException.Code.UNAUTHENTICATED ->
                    Exception("Sesija je istekla. Prijavite se ponovno.")
                else -> exception
            }
        }
    }
