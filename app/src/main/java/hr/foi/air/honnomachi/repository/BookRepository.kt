package hr.foi.air.honnomachi.repository

import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import hr.foi.air.honnomachi.model.BookModel
import kotlinx.coroutines.tasks.await

class BookRepository {
    private val db = Firebase.firestore

    suspend fun getBookDetails(bookId: String): Result<BookModel?> {
        return try {
            val document = db.collection("books").document(bookId).get().await()

            if (document.exists()) {
                val book = document.toObject(BookModel::class.java)
                Result.success(book)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}