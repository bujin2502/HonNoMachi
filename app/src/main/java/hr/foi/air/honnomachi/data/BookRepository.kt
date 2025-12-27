package hr.foi.air.honnomachi.data

import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import hr.foi.air.honnomachi.CrashlyticsManager
import hr.foi.air.honnomachi.model.BookModel
import hr.foi.air.honnomachi.ui.home.BookListState
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

interface BookRepository {
    fun getBooks(): Flow<BookListState>

    suspend fun getBookDetails(bookId: String): BookModel?
}

class BookRepositoryImpl : BookRepository {
    override fun getBooks(): Flow<BookListState> =
        callbackFlow {
            val listener =
                Firebase.firestore
                    .collection("books")
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            CrashlyticsManager.logException(error)
                            trySend(BookListState.Error(error.message ?: "Unknown error occurred."))
                            return@addSnapshotListener
                        }

                        if (snapshot != null) {
                            val resultList =
                                snapshot.documents.mapNotNull { doc ->
                                    doc.toObject(BookModel::class.java)
                                }
                            val state =
                                if (resultList.isEmpty()) {
                                    BookListState.Empty
                                } else {
                                    BookListState.Success(resultList)
                                }
                            trySend(state)
                        }
                    }
            awaitClose { listener.remove() }
        }

    override suspend fun getBookDetails(bookId: String): BookModel? =
        try {
            Firebase.firestore
                .collection("books")
                .document(bookId)
                .get()
                .await()
                .toObject(BookModel::class.java)
        } catch (e: Exception) {
            CrashlyticsManager.logException(e)
            null
        }
}
