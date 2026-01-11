package hr.foi.air.honnomachi.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
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
    ) : CartRepository {
        override suspend fun addToCart(book: BookModel): Result<Unit> =
            try {
                val currentUser = auth.currentUser
                if (currentUser != null && book.bookId != null) {
                    val cartItem =
                        CartItemModel(
                            bookId = book.bookId,
                            title = book.title,
                            author = book.authors.firstOrNull() ?: "Unknown Author",
                            price = book.price,
                            currency = book.priceCurrency.name,
                            imageUrl = book.imageUrls?.firstOrNull(),
                        )

                    // Koristimo bookId kao document ID da spriječimo duplikate
                    firestore
                        .collection("users")
                        .document(currentUser.uid)
                        .collection("cart")
                        .document(book.bookId)
                        .set(cartItem)
                        .await()

                    Result.Success(Unit)
                } else {
                    Result.Error(Exception("Korisnik nije prijavljen ili knjiga nema ID."))
                }
            } catch (e: Exception) {
                CrashlyticsManager.instance.logException(e)
                Result.Error(e)
            }

        override fun getCartItems(): Flow<Result<List<CartItemModel>>> =
            callbackFlow {
                val currentUser = auth.currentUser
                if (currentUser == null) {
                    trySend(Result.Error(Exception("Korisnik nije prijavljen.")))
                    close()
                    return@callbackFlow
                }

                val listener =
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
                                val items = snapshot.toObjects(CartItemModel::class.java)
                                trySend(Result.Success(items))
                            }
                        }
                awaitClose { listener.remove() }
            }

        override suspend fun removeFromCart(cartItemId: String): Result<Unit> =
            try {
                val currentUser = auth.currentUser
                if (currentUser != null) {
                    firestore
                        .collection("users")
                        .document(currentUser.uid)
                        .collection("cart")
                        .document(cartItemId)
                        .delete()
                        .await()
                    Result.Success(Unit)
                } else {
                    Result.Error(Exception("Korisnik nije prijavljen."))
                }
            } catch (e: Exception) {
                CrashlyticsManager.instance.logException(e)
                Result.Error(e)
            }
    }
