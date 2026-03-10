package hr.foi.air.honnomachi.data

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObject
import hr.foi.air.honnomachi.model.UserModel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

object FirestoreCollections {
    const val USERS = "users"
}

object FirestoreFields {
    const val IS_VERIFIED = "isVerified"
}

interface FirestoreUserDataSource {
    suspend fun getUserDocument(uid: String): DocumentSnapshot

    suspend fun getUser(uid: String): UserModel?

    suspend fun createUser(user: UserModel)

    suspend fun updateVerificationStatus(
        uid: String,
        isVerified: Boolean,
    )

    fun observeUser(uid: String): Flow<UserModel?>
}

class FirestoreUserDataSourceImpl
    @Inject
    constructor(
        private val firestore: FirebaseFirestore,
    ) : FirestoreUserDataSource {
        override suspend fun getUserDocument(uid: String): DocumentSnapshot =
            firestore
                .collection(FirestoreCollections.USERS)
                .document(uid)
                .get()
                .await()

        override suspend fun getUser(uid: String): UserModel? = getUserDocument(uid).toObject<UserModel>()

        override suspend fun createUser(user: UserModel) {
            firestore
                .collection(FirestoreCollections.USERS)
                .document(user.uid)
                .set(user)
                .await()
        }

        override suspend fun updateVerificationStatus(
            uid: String,
            isVerified: Boolean,
        ) {
            firestore
                .collection(FirestoreCollections.USERS)
                .document(uid)
                .update(FirestoreFields.IS_VERIFIED, isVerified)
                .await()
        }

        override fun observeUser(uid: String): Flow<UserModel?> =
            callbackFlow {
                val listener =
                    firestore
                        .collection(FirestoreCollections.USERS)
                        .document(uid)
                        .addSnapshotListener { snapshot, error ->
                            if (error != null) {
                                close(error)
                                return@addSnapshotListener
                            }
                            trySend(snapshot?.toObject<UserModel>())
                        }
                awaitClose { listener.remove() }
            }
    }
