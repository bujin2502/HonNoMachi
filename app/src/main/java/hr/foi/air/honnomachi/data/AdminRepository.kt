package hr.foi.air.honnomachi.data

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import hr.foi.air.honnomachi.CrashlyticsManager
import hr.foi.air.honnomachi.model.AuditLog
import hr.foi.air.honnomachi.model.UserModel
import hr.foi.air.honnomachi.util.Result
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class UserPage(
    val users: List<UserModel>,
    val lastDocSnapshot: DocumentSnapshot?,
)

interface AdminRepository {
    suspend fun isCurrentUserAdmin(): Result<Boolean>

    suspend fun getAllUsers(
        pageSize: Int = 20,
        lastDocSnapshot: DocumentSnapshot? = null,
    ): Result<UserPage>

    suspend fun getUserById(userId: String): Result<UserModel>

    suspend fun searchUsers(query: String): Result<List<UserModel>>

    suspend fun getUsersByStatus(isSuspended: Boolean): Result<List<UserModel>>

    suspend fun suspendUser(
        userId: String,
        reason: String,
    ): Result<Unit>

    suspend fun reactivateUser(userId: String): Result<Unit>
}

class AdminRepositoryImpl
    @Inject
    constructor(
        private val auth: FirebaseAuth,
        private val firestore: FirebaseFirestore,
    ) : AdminRepository {
        override suspend fun isCurrentUserAdmin(): Result<Boolean> =
            try {
                val currentUser = auth.currentUser
                if (currentUser == null) {
                    Result.Error(Exception("No user logged in."))
                } else {
                    val document =
                        firestore
                            .collection("users")
                            .document(currentUser.uid)
                            .get()
                            .await()

                    val user = document.toObject(UserModel::class.java)
                    Result.Success(user?.admin == true)
                }
            } catch (e: Exception) {
                CrashlyticsManager.instance.logException(e)
                Result.Error(e)
            }

        override suspend fun getAllUsers(
            pageSize: Int,
            lastDocSnapshot: DocumentSnapshot?,
        ): Result<UserPage> =
            try {
                var query: Query =
                    firestore
                        .collection("users")
                        .orderBy("name")
                        .limit(pageSize.toLong())

                if (lastDocSnapshot != null) {
                    query = query.startAfter(lastDocSnapshot)
                }

                val snapshot = query.get().await()

                val users =
                    snapshot.documents.mapNotNull { doc ->
                        doc.toObject(UserModel::class.java)?.copy(uid = doc.id)
                    }
                Result.Success(UserPage(users, snapshot.documents.lastOrNull()))
            } catch (e: Exception) {
                CrashlyticsManager.instance.logException(e)
                Result.Error(e)
            }

        override suspend fun getUserById(userId: String): Result<UserModel> =
            try {
                val document =
                    firestore
                        .collection("users")
                        .document(userId)
                        .get()
                        .await()

                if (document.exists()) {
                    val user = document.toObject(UserModel::class.java)?.copy(uid = document.id)
                    if (user != null) {
                        Result.Success(user)
                    } else {
                        Result.Error(Exception("Failed to parse user data."))
                    }
                } else {
                    Result.Error(Exception("User not found."))
                }
            } catch (e: Exception) {
                CrashlyticsManager.instance.logException(e)
                Result.Error(e)
            }

        override suspend fun searchUsers(query: String): Result<List<UserModel>> =
            try {
                val snapshot =
                    firestore
                        .collection("users")
                        .get()
                        .await()

                val allUsers =
                    snapshot.documents.mapNotNull { doc ->
                        doc.toObject(UserModel::class.java)?.copy(uid = doc.id)
                    }

                val filtered =
                    allUsers.filter { user ->
                        user.name.contains(query, ignoreCase = true) ||
                            user.email.contains(query, ignoreCase = true)
                    }
                Result.Success(filtered)
            } catch (e: Exception) {
                CrashlyticsManager.instance.logException(e)
                Result.Error(e)
            }

        override suspend fun getUsersByStatus(isSuspended: Boolean): Result<List<UserModel>> =
            try {
                val snapshot =
                    firestore
                        .collection("users")
                        .get()
                        .await()

                val allUsers =
                    snapshot.documents.mapNotNull { doc ->
                        doc.toObject(UserModel::class.java)?.copy(uid = doc.id)
                    }

                val filtered =
                    if (isSuspended) {
                        allUsers.filter { it.suspended == true }
                    } else {
                        allUsers.filter { it.suspended != true }
                    }
                Result.Success(filtered)
            } catch (e: Exception) {
                CrashlyticsManager.instance.logException(e)
                Result.Error(e)
            }

        override suspend fun suspendUser(
            userId: String,
            reason: String,
        ): Result<Unit> {
            val adminUid =
                auth.currentUser?.uid
                    ?: return Result.Error(Exception("Administrator nije prijavljen."))

            val userResult = getUserById(userId)
            if (userResult is Result.Error) {
                return Result.Error(userResult.exception)
            }

            val user = (userResult as Result.Success).data
            if (user.suspended == true) {
                return Result.Error(Exception("Korisnik je već suspendiran."))
            }

            return try {
                val now = Timestamp.now()
                val batch = firestore.batch()

                val userRef = firestore.collection("users").document(userId)
                batch.update(
                    userRef,
                    mapOf(
                        "suspended" to true,
                        "suspendedAt" to now,
                        "suspendedReason" to reason,
                        "suspendedBy" to adminUid,
                    ),
                )

                val auditLog =
                    AuditLog(
                        action = "USER_SUSPENDED",
                        targetUserId = userId,
                        adminUserId = adminUid,
                        reason = reason,
                        timestamp = now,
                    )
                val auditRef = firestore.collection("audit_logs").document()
                batch.set(auditRef, auditLog)

                val booksSnapshot =
                    firestore
                        .collection("books")
                        .whereEqualTo("userID", userId)
                        .get()
                        .await()

                for (bookDoc in booksSnapshot.documents) {
                    batch.update(bookDoc.reference, "sellerSuspended", true)
                }

                batch.commit().await()

                Result.Success(Unit)
            } catch (e: Exception) {
                CrashlyticsManager.instance.logException(e)
                Result.Error(e)
            }
        }

        override suspend fun reactivateUser(userId: String): Result<Unit> {
            val adminUid =
                auth.currentUser?.uid
                    ?: return Result.Error(Exception("Administrator nije prijavljen."))

            val userResult = getUserById(userId)
            if (userResult is Result.Error) {
                return Result.Error(userResult.exception)
            }

            val user = (userResult as Result.Success).data
            if (user.suspended != true) {
                return Result.Error(Exception("Korisnik nije suspendiran."))
            }

            return try {
                val now = Timestamp.now()
                val batch = firestore.batch()

                val userRef = firestore.collection("users").document(userId)
                batch.update(
                    userRef,
                    mapOf(
                        "suspended" to false,
                        "reactivatedAt" to now,
                        "reactivatedBy" to adminUid,
                        "suspendedReason" to FieldValue.delete(),
                    ),
                )

                val auditLog =
                    AuditLog(
                        action = "USER_REACTIVATED",
                        targetUserId = userId,
                        adminUserId = adminUid,
                        timestamp = now,
                    )
                val auditRef = firestore.collection("audit_logs").document()
                batch.set(auditRef, auditLog)

                val booksSnapshot =
                    firestore
                        .collection("books")
                        .whereEqualTo("userID", userId)
                        .get()
                        .await()

                for (bookDoc in booksSnapshot.documents) {
                    batch.update(bookDoc.reference, "sellerSuspended", FieldValue.delete())
                }

                batch.commit().await()

                Result.Success(Unit)
            } catch (e: Exception) {
                CrashlyticsManager.instance.logException(e)
                Result.Error(e)
            }
        }
    }
