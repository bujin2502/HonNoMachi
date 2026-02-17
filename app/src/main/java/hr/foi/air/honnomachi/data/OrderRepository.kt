package hr.foi.air.honnomachi.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import hr.foi.air.honnomachi.model.CartItemModel
import hr.foi.air.honnomachi.model.ItemStatus
import hr.foi.air.honnomachi.util.Result
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

interface OrderRepository {
    suspend fun placeOrder(): Result<Unit>
}

class OrderRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
) : OrderRepository {
    override suspend fun placeOrder(): Result<Unit> {
        val currentUser = auth.currentUser ?: return Result.Error(Exception("Korisnik nije prijavljen."))

        return try {
            val cartItemsSnapshot = firestore
                .collection("users")
                .document(currentUser.uid)
                .collection("cart")
                .get()
                .await()

            if (cartItemsSnapshot.isEmpty) {
                return Result.Error(Exception("Košarica je prazna."))
            }

            val cartItems = cartItemsSnapshot.toObjects(CartItemModel::class.java)

            firestore.runBatch { batch ->
                for (item in cartItems) {
                    val bookRef = firestore.collection("books").document(item.bookId)
                    batch.update(bookRef, "status", ItemStatus.SOLD.name)

                    val cartItemRef = firestore
                        .collection("users")
                        .document(currentUser.uid)
                        .collection("cart")
                        .document(item.bookId)
                    batch.delete(cartItemRef)
                }
            }.await()

            Result.Success(Unit)
        } catch (e: Exception) {
            // Log exception if needed
            Result.Error(e)
        }
    }
}
