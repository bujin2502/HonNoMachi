package hr.foi.air.honnomachi.data

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import hr.foi.air.honnomachi.CrashlyticsManager
import hr.foi.air.honnomachi.model.BookModel
import hr.foi.air.honnomachi.model.ItemStatus
import hr.foi.air.honnomachi.util.Result
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

interface BookRepository {
    fun getBooks(): Flow<Result<List<BookModel>>>

    suspend fun getBookDetails(bookId: String): Result<BookModel?>

    suspend fun addBook(book: BookModel): Result<String>

    fun getSoldBooks(userId: String): Flow<Result<List<BookModel>>>

    fun getPurchasedBooks(userId: String): Flow<Result<List<BookModel>>>

    fun getMyListings(userId: String): Flow<Result<List<BookModel>>>

    suspend fun updateListingStatus(
        bookId: String,
        newStatus: ItemStatus,
    ): Result<Unit>

    suspend fun deleteListing(bookId: String): Result<Unit>
}

class BookRepositoryImpl
    @Inject
    constructor(
        private val auth: FirebaseAuth,
        private val firestore: FirebaseFirestore,
    ) : BookRepository {
        override fun getBooks(): Flow<Result<List<BookModel>>> =
            callbackFlow {
                val listener =
                    firestore
                        .collection("books")
                        .addSnapshotListener { snapshot, error ->
                            if (error != null) {
                                CrashlyticsManager.instance.logException(error)
                                trySend(Result.Error(error))
                                return@addSnapshotListener
                            }

                            if (snapshot != null) {
                                val resultList =
                                    snapshot.documents
                                        .mapNotNull { doc ->
                                            doc
                                                .toObject(BookModel::class.java)
                                                ?.copy(bookId = doc.id)
                                        }.filter { it.sellerSuspended != true }
                                trySend(Result.Success(resultList))
                            }
                        }
                awaitClose { listener.remove() }
            }

        override suspend fun getBookDetails(bookId: String): Result<BookModel?> =
            try {
                val snapshot =
                    firestore
                        .collection("books")
                        .document(bookId)
                        .get()
                        .await()
                val book =
                    snapshot
                        .toObject(BookModel::class.java)
                        ?.copy(bookId = snapshot.id)
                Result.Success(book)
            } catch (e: Exception) {
                CrashlyticsManager.instance.logException(e)
                Result.Error(e)
            }

        override suspend fun addBook(book: BookModel): Result<String> =
            try {
                val currentUser = auth.currentUser
                if (currentUser == null) {
                    Result.Error(Exception("Korisnik nije prijavljen."))
                } else {
                    val listing =
                        book.copy(
                            userID = currentUser.uid,
                            listingDate = Timestamp.now(),
                        )

                    val document = firestore.collection("books").document()
                    document.set(listing).await()
                    Result.Success(document.id)
                }
            } catch (e: Exception) {
                CrashlyticsManager.instance.logException(e)
                Result.Error(e)
            }

        override fun getSoldBooks(userId: String): Flow<Result<List<BookModel>>> =
            callbackFlow {
                val listener =
                    firestore
                        .collection("books")
                        .whereEqualTo("userID", userId)
                        .whereEqualTo("status", ItemStatus.SOLD.name)
                        .addSnapshotListener { snapshot, error ->
                            if (error != null) {
                                CrashlyticsManager.instance.logException(error)
                                trySend(Result.Error(error))
                                return@addSnapshotListener
                            }

                            if (snapshot != null) {
                                val resultList =
                                    snapshot.documents
                                        .mapNotNull { doc ->
                                            doc.toObject(BookModel::class.java)
                                        }
                                trySend(Result.Success(resultList))
                            }
                        }
                awaitClose { listener.remove() }
            }

        override fun getPurchasedBooks(userId: String): Flow<Result<List<BookModel>>> =
            callbackFlow {
                val query =
                    firestore
                        .collection("books")
                        .whereEqualTo("soldToUid", userId)

                val listener =
                    query.addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            trySend(Result.Error(error))
                            return@addSnapshotListener
                        }
                        val books = snapshot?.toObjects(BookModel::class.java) ?: emptyList()
                        trySend(Result.Success(books))
                    }
                awaitClose { listener.remove() }
            }

        override fun getMyListings(userId: String): Flow<Result<List<BookModel>>> =
            callbackFlow {
                val listener =
                    firestore
                        .collection("books")
                        .whereEqualTo("userID", userId)
                        .addSnapshotListener { snapshot, error ->
                            if (error != null) {
                                CrashlyticsManager.instance.logException(error)
                                trySend(Result.Error(error))
                                return@addSnapshotListener
                            }

                            if (snapshot != null) {
                                val resultList =
                                    snapshot.documents
                                        .mapNotNull { doc ->
                                            doc
                                                .toObject(BookModel::class.java)
                                                ?.copy(bookId = doc.id)
                                        }
                                trySend(Result.Success(resultList))
                            }
                        }
                awaitClose { listener.remove() }
            }

        override suspend fun updateListingStatus(
            bookId: String,
            newStatus: ItemStatus,
        ): Result<Unit> =
            try {
                firestore
                    .collection("books")
                    .document(bookId)
                    .update("status", newStatus.name)
                    .await()
                Result.Success(Unit)
            } catch (e: Exception) {
                CrashlyticsManager.instance.logException(e)
                Result.Error(e)
            }

        override suspend fun deleteListing(bookId: String): Result<Unit> =
            try {
                firestore
                    .collection("books")
                    .document(bookId)
                    .delete()
                    .await()
                Result.Success(Unit)
            } catch (e: Exception) {
                CrashlyticsManager.instance.logException(e)
                Result.Error(e)
            }
    }
