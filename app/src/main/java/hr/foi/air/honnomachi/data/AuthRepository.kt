package hr.foi.air.honnomachi.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObject
import hr.foi.air.honnomachi.model.UserModel
import hr.foi.air.honnomachi.util.Result
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

interface AuthRepository {
    val currentUser: UserModel?

    suspend fun login(
        email: String,
        password: String,
    ): Result<UserModel>

    suspend fun register(
        name: String,
        email: String,
        password: String,
    ): Result<UserModel>

    suspend fun sendPasswordResetEmail(email: String): Result<Unit>

    suspend fun reloadUser(): Result<Unit>

    suspend fun signOut(): Result<Unit>

    suspend fun checkSession(): Result<UserModel>

    suspend fun testSecureRead(): Result<String>

    suspend fun loginWithGoogle(idToken: String): Result<UserModel>

    suspend fun resendVerificationEmail(
        email: String,
        password: String,
    ): Result<Unit>

    suspend fun syncVerificationStatus(): Result<UserModel>
}

class AuthRepositoryImpl
    @Inject
    constructor(
        private val auth: FirebaseAuth,
        private val firestore: FirebaseFirestore,
        private val crashlytics: FirebaseCrashlytics,
    ) : AuthRepository {
        override val currentUser: UserModel?
            get() =
                auth.currentUser?.let { firebaseUser ->
                    // This is a simplified version. For a complete user profile,
                    // you would fetch the user document from Firestore here as well.
                    UserModel(
                        uid = firebaseUser.uid,
                        email = firebaseUser.email ?: "",
                        isVerified = firebaseUser.isEmailVerified,
                    )
                }

        override suspend fun login(
            email: String,
            password: String,
        ): Result<UserModel> =
            try {
                val authResult = auth.signInWithEmailAndPassword(email, password).await()
                val firebaseUser = authResult.user

                if (firebaseUser != null) {
                    // Reload to get fresh verification status
                    firebaseUser.reload().await()
                    val freshUser = auth.currentUser

                    val user =
                        firestore
                            .collection("users")
                            .document(firebaseUser.uid)
                            .get()
                            .await()
                            .toObject<UserModel>()

                    if (user != null) {
                        // Use Firebase Auth isEmailVerified as source of truth
                        val isVerified = freshUser?.isEmailVerified ?: false

                        Log.d("AuthRepository", "Login - Firebase Auth isEmailVerified: $isVerified")
                        Log.d("AuthRepository", "Login - Firestore isVerified: ${user.isVerified}")

                        // Sync Firestore if needed
                        if (isVerified && !user.isVerified) {
                            Log.d("AuthRepository", "Syncing Firestore isVerified to true")
                            firestore
                                .collection("users")
                                .document(firebaseUser.uid)
                                .update("isVerified", true)
                                .await()
                        }

                        Result.Success(user.copy(isVerified = isVerified))
                    } else {
                        Result.Error(Exception("User data not found in Firestore."))
                    }
                } else {
                    Result.Error(Exception("Authentication failed"))
                }
            } catch (e: Exception) {
                crashlytics.recordException(e)
                crashlytics.log("Login failed for email: $email")
                Result.Error(e)
            }

        override suspend fun register(
            name: String,
            email: String,
            password: String,
        ): Result<UserModel> =
            try {
                val authResult = auth.createUserWithEmailAndPassword(email, password).await()
                auth.currentUser?.sendEmailVerification()?.await()
                val uid = authResult.user?.uid
                if (uid != null) {
                    val newUser =
                        UserModel(
                            name = name,
                            email = email,
                            uid = uid,
                        )
                    firestore
                        .collection("users")
                        .document(uid)
                        .set(newUser)
                        .await()
                    Result.Success(newUser)
                } else {
                    Result.Error(Exception("User is null after registration"))
                }
            } catch (e: Exception) {
                crashlytics.recordException(e)
                crashlytics.log("Registration failed for email: $email")
                Result.Error(e)
            }

        override suspend fun sendPasswordResetEmail(email: String): Result<Unit> =
            try {
                auth.sendPasswordResetEmail(email).await()
                Result.Success(Unit)
            } catch (e: Exception) {
                crashlytics.recordException(e)
                crashlytics.log("Password reset failed for email: $email")
                Result.Error(e)
            }

        override suspend fun reloadUser(): Result<Unit> =
            try {
                auth.currentUser?.reload()?.await()
                Result.Success(Unit)
            } catch (e: Exception) {
                crashlytics.recordException(e)
                crashlytics.log("Reload user failed for uid: ${auth.currentUser?.uid}")
                Result.Error(e)
            }

        override suspend fun signOut(): Result<Unit> =
            try {
                auth.signOut()
                Result.Success(Unit)
            } catch (e: Exception) {
                crashlytics.recordException(e)
                crashlytics.log("Sign out failed")
                Result.Error(e)
            }

        override suspend fun checkSession(): Result<UserModel> =
            try {
                val firebaseUser = auth.currentUser
                if (firebaseUser == null) {
                    Result.Error(Exception("No user is currently logged in"))
                } else {
                    // Reload user to get fresh data from server
                    firebaseUser.reload().await()
                    val freshUser = auth.currentUser
                    if (freshUser != null && freshUser.isEmailVerified) {
                        // Fetch full user data from Firestore
                        val userDoc =
                            firestore
                                .collection("users")
                                .document(freshUser.uid)
                                .get()
                                .await()
                        val user = userDoc.toObject<UserModel>()
                        if (user != null) {
                            Result.Success(user.copy(isVerified = true))
                        } else {
                            Result.Error(Exception("User data not found in Firestore"))
                        }
                    } else {
                        Result.Error(Exception("User is not verified or session expired"))
                    }
                }
            } catch (e: Exception) {
                crashlytics.recordException(e)
                crashlytics.log("Check session failed for uid: ${auth.currentUser?.uid}")
                Result.Error(e)
            }

        override suspend fun testSecureRead(): Result<String> =
            try {
                val user = auth.currentUser
                if (user == null) {
                    Result.Error(Exception("No authenticated user"))
                } else {
                    // Test reading from a protected Firestore collection
                    val testDoc =
                        firestore
                            .collection("users")
                            .document(user.uid)
                            .get()
                            .await()
                    if (testDoc.exists()) {
                        Result.Success("Secure read successful - token is valid")
                    } else {
                        Result.Error(Exception("Document not found"))
                    }
                }
            } catch (e: Exception) {
                crashlytics.recordException(e)
                crashlytics.log("Test secure read failed for uid: ${auth.currentUser?.uid}")
                Result.Error(Exception("Token validation failed: ${e.message}"))
            }

        override suspend fun loginWithGoogle(idToken: String): Result<UserModel> =
            try {
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                val authResult = auth.signInWithCredential(credential).await()
                val firebaseUser = authResult.user

                if (firebaseUser != null) {
                    val uid = firebaseUser.uid
                    // Check if user document exists in Firestore
                    val userDoc =
                        firestore
                            .collection("users")
                            .document(uid)
                            .get()
                            .await()

                    val user =
                        if (userDoc.exists()) {
                            // User exists, fetch their data
                            userDoc.toObject<UserModel>()
                        } else {
                            // New user, create a document in Firestore
                            val newUser =
                                UserModel(
                                    uid = uid,
                                    email = firebaseUser.email ?: "",
                                    name = firebaseUser.displayName ?: "",
                                    isVerified = firebaseUser.isEmailVerified,
                                )
                            firestore
                                .collection("users")
                                .document(uid)
                                .set(newUser)
                                .await()
                            newUser
                        }

                    if (user != null) {
                        Result.Success(user.copy(isVerified = firebaseUser.isEmailVerified))
                    } else {
                        Result.Error(Exception("Failed to retrieve user data"))
                    }
                } else {
                    Result.Error(Exception("Firebase user is null"))
                }
            } catch (e: Exception) {
                crashlytics.recordException(e)
                crashlytics.log("Google login failed")
                Result.Error(e)
            }

        override suspend fun resendVerificationEmail(
            email: String,
            password: String,
        ): Result<Unit> =
            try {
                // Re-authenticate user to get fresh credentials
                val authResult = auth.signInWithEmailAndPassword(email, password).await()
                val user = authResult.user

                if (user != null) {
                    if (user.isEmailVerified) {
                        // Sync Firestore with Authentication status
                        syncVerificationStatus()
                        Result.Error(Exception("Email is already verified"))
                    } else {
                        user.sendEmailVerification().await()
                        Result.Success(Unit)
                    }
                } else {
                    Result.Error(Exception("User not found"))
                }
            } catch (e: Exception) {
                crashlytics.recordException(e)
                crashlytics.log("Resend verification email failed for: $email")
                Result.Error(e)
            }

        override suspend fun syncVerificationStatus(): Result<UserModel> =
            try {
                val user = auth.currentUser
                if (user == null) {
                    Result.Error(Exception("No user is currently logged in"))
                } else {
                    // Reload to get fresh verification status from server
                    user.reload().await()
                    val freshUser = auth.currentUser

                    if (freshUser != null && freshUser.isEmailVerified) {
                        // Update Firestore document with verified status
                        firestore
                            .collection("users")
                            .document(freshUser.uid)
                            .update("isVerified", true)
                            .await()

                        // Fetch and return updated user document
                        val userDoc =
                            firestore
                                .collection("users")
                                .document(freshUser.uid)
                                .get()
                                .await()
                        val userModel = userDoc.toObject<UserModel>()
                        if (userModel != null) {
                            Result.Success(userModel.copy(isVerified = true))
                        } else {
                            Result.Error(Exception("User data not found in Firestore"))
                        }
                    } else if (freshUser != null) {
                        Result.Error(Exception("Email is not yet verified"))
                    } else {
                        Result.Error(Exception("User session expired"))
                    }
                }
            } catch (e: Exception) {
                crashlytics.recordException(e)
                crashlytics.log("Sync verification status failed for uid: ${auth.currentUser?.uid}")
                Result.Error(e)
            }
    }
