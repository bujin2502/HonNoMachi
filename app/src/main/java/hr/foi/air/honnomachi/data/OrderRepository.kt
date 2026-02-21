package hr.foi.air.honnomachi.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import hr.foi.air.honnomachi.CrashlyticsManager
import hr.foi.air.honnomachi.model.CartItemModel
import hr.foi.air.honnomachi.model.ItemStatus
import hr.foi.air.honnomachi.util.Result
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

interface OrderRepository {
    suspend fun placeOrder(): Result<Unit>
}

class OrderRepositoryImpl
    @Inject
    constructor(
        private val auth: FirebaseAuth,
        private val firestore: FirebaseFirestore,
    ) : OrderRepository {
        override suspend fun placeOrder(): Result<Unit> {
            val currentUser = auth.currentUser ?: return Result.Error(Exception("Korisnik nije prijavljen."))

            return try {
                val soldItems = processCurrentUserOrder(currentUser.uid)
                cleanupOtherUsersCarts(soldItems, currentUser.uid)
                Result.Success(Unit)
            } catch (e: Exception) {
                CrashlyticsManager.instance.logException(e)
                Result.Error(e)
            }
        }

        private suspend fun processCurrentUserOrder(userId: String): List<CartItemModel> {
            val cartItemsSnapshot =
                firestore
                    .collection("users")
                    .document(userId)
                    .collection("cart")
                    .get()
                    .await()

            if (cartItemsSnapshot.isEmpty) {
                throw Exception("Košarica je prazna.")
            }
            val cartItems = cartItemsSnapshot.toObjects(CartItemModel::class.java)

            firestore
                .runBatch { batch ->
                    for (item in cartItems) {
                        val bookRef = firestore.collection("books").document(item.bookId)
                        batch.update(bookRef, "status", ItemStatus.SOLD.name)

                        val cartItemRef =
                            firestore
                                .collection("users")
                                .document(userId)
                                .collection("cart")
                                .document(item.bookId)
                        batch.delete(cartItemRef)
                    }
                }.await()
            return cartItems
        }

        private suspend fun cleanupOtherUsersCarts(
            soldItems: List<CartItemModel>,
            currentUserId: String,
        ) {
            try {
                val allUsersSnapshot = firestore.collection("users").get().await()
                val allUserIds = allUsersSnapshot.documents.map { it.id }

                firestore
                    .runBatch { batch ->
                        for (soldItem in soldItems) {
                            for (userId in allUserIds) {
                                if (userId == currentUserId) continue

                                val otherUserCartItemRef =
                                    firestore
                                        .collection("users")
                                        .document(userId)
                                        .collection("cart")
                                        .document(soldItem.bookId)
                                batch.delete(otherUserCartItemRef)
                            }
                        }
                    }.await()
            } catch (e: Exception) {
                CrashlyticsManager.instance.logException(e)
            }
        }
    }
