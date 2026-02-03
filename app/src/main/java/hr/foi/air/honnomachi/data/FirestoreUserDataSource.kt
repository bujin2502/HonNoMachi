package hr.foi.air.honnomachi.data

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObject
import hr.foi.air.honnomachi.model.UserModel
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
    }
