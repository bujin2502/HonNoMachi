package hr.foi.air.honnomachi.data

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import hr.foi.air.honnomachi.CrashlyticsManager
import hr.foi.air.honnomachi.model.BookModel
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
                                CrashlyticsManager.instance.logException(error) // Keep Crashlytics logging
                                trySend(Result.Error(error))
                                return@addSnapshotListener
                            }

                            if (snapshot != null) {
                                val resultList =
                                    snapshot.documents.mapNotNull { doc ->
                                        doc.toObject(BookModel::class.java)
                                    }
                                trySend(Result.Success(resultList))
                            }
                        }
                awaitClose { listener.remove() }
            }

        override suspend fun getBookDetails(bookId: String): Result<BookModel?> =
            try {
                val book =
                    firestore
                        .collection("books")
                        .document(bookId)
                        .get()
                        .await()
                        .toObject(BookModel::class.java)
                Result.Success(book)
            } catch (e: Exception) {
                CrashlyticsManager.instance.logException(e) // Keep Crashlytics logging
                Result.Error(e)
            }

        override suspend fun addBook(book: BookModel): Result<String> {
            return try {
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
                CrashlyticsManager.instance.logException(e) // Keep Crashlytics logging
                Result.Error(e)
            }
        }
    }
