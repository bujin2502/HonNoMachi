package hr.foi.air.honnomachi.data

import com.google.firebase.Firebase
import com.google.firebase.analytics.analytics
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.crashlytics.crashlytics
import com.google.firebase.firestore.FirebaseFirestore
import hr.foi.air.honnomachi.CrashlyticsManager
import hr.foi.air.honnomachi.model.UserModel
import hr.foi.air.honnomachi.util.Result
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

interface ProfileRepository {
    suspend fun getUserProfile(): Result<UserModel>

    suspend fun updateUserProfile(
        name: String,
        phoneNumber: String,
        street: String,
        postNumber: String,
        city: String,
    ): Result<UserModel>

    suspend fun updateAnalyticsSetting(isEnabled: Boolean): Result<Unit>

    suspend fun changePassword(
        oldPassword: String,
        newPassword: String,
    ): Result<Unit>
}

class ProfileRepositoryImpl
    @Inject
    constructor(
        private val auth: FirebaseAuth,
        private val firestore: FirebaseFirestore,
    ) : ProfileRepository {
        override suspend fun getUserProfile(): Result<UserModel> =
            try {
                val currentUser = auth.currentUser
                if (currentUser != null) {
                    val document =
                        firestore
                            .collection("users")
                            .document(currentUser.uid)
                            .get()
                            .await()

                    if (document.exists()) {
                        val user = document.toObject(UserModel::class.java)
                        if (user != null) {
                            Result.Success(user)
                        } else {
                            Result.Error(Exception("Failed to parse user data."))
                        }
                    } else {
                        Result.Error(Exception("User document not found."))
                    }
                } else {
                    Result.Error(Exception("No user logged in."))
                }
            } catch (e: Exception) {
                CrashlyticsManager.instance.logException(e)
                Result.Error(e)
            }

        override suspend fun updateUserProfile(
            name: String,
            phoneNumber: String,
            street: String,
            postNumber: String,
            city: String,
        ): Result<UserModel> =
            try {
                val currentUser = auth.currentUser
                if (currentUser != null) {
                    val updates =
                        mapOf(
                            "name" to name,
                            "phoneNumber" to phoneNumber,
                            "street" to street,
                            "postNumber" to postNumber,
                            "city" to city,
                        )

                    firestore
                        .collection("users")
                        .document(currentUser.uid)
                        .update(updates)
                        .await()

                    // Fetch updated user
                    val document =
                        firestore
                            .collection("users")
                            .document(currentUser.uid)
                            .get()
                            .await()

                    val updatedUser = document.toObject(UserModel::class.java)
                    if (updatedUser != null) {
                        Result.Success(updatedUser)
                    } else {
                        Result.Error(Exception("Failed to fetch updated user."))
                    }
                } else {
                    Result.Error(Exception("No user logged in."))
                }
            } catch (e: Exception) {
                CrashlyticsManager.instance.logException(e)
                Result.Error(e)
            }

        override suspend fun updateAnalyticsSetting(isEnabled: Boolean): Result<Unit> =
            try {
                val currentUser = auth.currentUser
                if (currentUser != null) {
                    Firebase.analytics.setAnalyticsCollectionEnabled(isEnabled)
                    Firebase.crashlytics.isCrashlyticsCollectionEnabled = isEnabled

                    firestore
                        .collection("users")
                        .document(currentUser.uid)
                        .update("analyticsEnabled", isEnabled)
                        .await()

                    Result.Success(Unit)
                } else {
                    Result.Error(Exception("No user logged in."))
                }
            } catch (e: Exception) {
                CrashlyticsManager.instance.logException(e)
                Result.Error(e)
            }

        override suspend fun changePassword(
            oldPassword: String,
            newPassword: String,
        ): Result<Unit> {
            return try {
                val user = auth.currentUser
                if (user == null || user.email == null) {
                    return Result.Error(Exception("User not logged in."))
                }

                val credential = EmailAuthProvider.getCredential(user.email!!, oldPassword)

                // Re-authenticate first
                user.reauthenticate(credential).await()

                // Then update password
                user.updatePassword(newPassword).await()

                Result.Success(Unit)
            } catch (e: Exception) {
                CrashlyticsManager.instance.logException(e)
                Result.Error(e)
            }
        }
    }
