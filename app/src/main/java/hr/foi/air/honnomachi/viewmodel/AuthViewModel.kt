package hr.foi.air.honnomachi.viewmodel

import android.os.Bundle
import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import hr.foi.air.honnomachi.CrashlyticsManager
import hr.foi.air.honnomachi.model.UserModel

open class AuthViewModel(
    private val auth: FirebaseAuth? = Firebase.auth,
    private val firestore: FirebaseFirestore? = Firebase.firestore,
    private val analytics: FirebaseAnalytics? = Firebase.analytics,
) : ViewModel() {
    open fun signup(
        email: String,
        name: String,
        password: String,
        onResult: (Boolean, String?) -> Unit,
    ) {
        if (email.isBlank() || name.isBlank() || password.isBlank()) {
            onResult(false, "Email, name, and password cannot be empty.")
            return
        }
        auth
            ?.createUserWithEmailAndPassword(email, password)
            ?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = task.result?.user
                    user
                        ?.sendEmailVerification()
                        ?.addOnCompleteListener { verificationTask ->
                            if (verificationTask.isSuccessful) {
                                val userId = user.uid
                                val userModel =
                                    UserModel(
                                        name = name,
                                        email = email,
                                        uid = userId,
                                        isVerified = false,
                                    )
                                firestore
                                    ?.collection("users")
                                    ?.document(userId)
                                    ?.set(userModel)
                                    ?.addOnCompleteListener { dbTask ->
                                        if (dbTask.isSuccessful) {
                                            auth.signOut()
                                            onResult(true, null)
                                        } else {
                                            dbTask.exception?.let { CrashlyticsManager.logException(it) }
                                            onResult(false, "Database error.")
                                        }
                                    }
                            } else {
                                verificationTask.exception?.let { CrashlyticsManager.logException(it) }
                                onResult(false, "Failed to send verification email.")
                            }
                        }
                } else {
                    task.exception?.let { CrashlyticsManager.logException(it) }
                    onResult(false, task.exception?.localizedMessage)
                }
            } ?: run {
            onResult(false, "Authentication service is not available.")
        }
    }

    open fun login(
        email: String,
        password: String,
        onResult: (Boolean, String?) -> Unit,
    ) {
        if (email.isBlank() || password.isBlank()) {
            onResult(false, "Email and password cannot be empty.")
            return
        }
        auth
            ?.signInWithEmailAndPassword(email, password)
            ?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = task.result?.user
                    if (user?.isEmailVerified == true) {
                        val userId = user.uid
                        firestore
                            ?.collection("users")
                            ?.document(userId)
                            ?.update("isVerified", true)
                            ?.addOnCompleteListener { updateTask ->
                                if (updateTask.isSuccessful) {
                                    logLoginSuccess("email_password", user.uid)
                                    onResult(true, null)
                                } else {
                                    updateTask.exception?.let { CrashlyticsManager.logException(it) }
                                    onResult(false, "Failed to update verification status.")
                                }
                            }
                    } else {
                        auth.signOut()
                        onResult(false, "Please verify your email before logging in.")
                    }
                } else {
                    task.exception?.let { CrashlyticsManager.logException(it) }
                    logLoginFailure(task.exception, "email_password")
                    onResult(false, task.exception?.localizedMessage)
                }
            } ?: run {
            onResult(false, "Authentication service is not available.")
        }
    }

    open fun loginWithGoogle(
        idToken: String,
        onResult: (Boolean, String?) -> Unit,
    ) {
        val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
        auth
            ?.signInWithCredential(firebaseCredential)
            ?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = task.result?.user
                    val userId = user?.uid
                    if (user != null && userId != null) {
                        val userDocRef = firestore?.collection("users")?.document(userId)
                        if (userDocRef == null) {
                            logLoginSuccess("google", userId)
                            onResult(true, null)
                            return@addOnCompleteListener
                        }

                        userDocRef
                            .get()
                            .addOnSuccessListener { snapshot ->
                                val writeTask =
                                    if (snapshot.exists()) {
                                        userDocRef.update("isVerified", true)
                                    } else {
                                        val userModel =
                                            UserModel(
                                                name = user.displayName ?: "",
                                                email = user.email ?: "",
                                                uid = userId,
                                                isVerified = user.isEmailVerified,
                                            )
                                        userDocRef.set(userModel)
                                    }
                                writeTask.addOnCompleteListener {
                                    logLoginSuccess("google", userId)
                                    onResult(true, null)
                                }
                            }.addOnFailureListener {
                                CrashlyticsManager.logException(it)
                                logLoginSuccess("google", userId)
                                onResult(true, null)
                            }
                    } else {
                        logLoginSuccess("google", userId ?: "")
                        onResult(true, null)
                    }
                } else {
                    task.exception?.let { CrashlyticsManager.logException(it) }
                    logLoginFailure(task.exception, "google")
                    onResult(false, task.exception?.localizedMessage ?: "Google sign-in failed.")
                }
            } ?: run {
            onResult(false, "Authentication service is not available.")
        }
    }

    open fun signOut() {
        logLogout("user_logout")
        CrashlyticsManager.setUserId(null)
        auth?.signOut()
    }

    open fun forgotPassword(
        email: String,
        onResult: (Boolean, String?) -> Unit,
    ) {
        if (email.isBlank()) {
            onResult(false, "Email cannot be empty.")
            return
        }
        auth
            ?.sendPasswordResetEmail(email)
            ?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onResult(true, null)
                } else {
                    task.exception?.let { CrashlyticsManager.logException(it) }
                    onResult(false, task.exception?.localizedMessage)
                }
            } ?: run {
            onResult(false, "Authentication service is not available.")
        }
    }

    open fun resendVerificationEmail(
        email: String,
        password: String,
        onResult: (Boolean, String?) -> Unit,
    ) {
        auth
            ?.signInWithEmailAndPassword(email, password)
            ?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = task.result?.user
                    if (user?.isEmailVerified == false) {
                        user
                            .sendEmailVerification()
                            .addOnCompleteListener { verificationTask ->
                                if (verificationTask.isSuccessful) {
                                    auth.signOut()
                                    onResult(true, "Verification email sent. Please check your inbox.")
                                } else {
                                    verificationTask.exception?.let { CrashlyticsManager.logException(it) }
                                    auth.signOut()
                                    onResult(false, "Failed to send verification email.")
                                }
                            }
                    } else {
                        auth.signOut()
                        onResult(false, "This email is already verified.")
                    }
                } else {
                    task.exception?.let { CrashlyticsManager.logException(it) }
                    onResult(false, task.exception?.localizedMessage)
                }
            } ?: run {
            onResult(false, "Authentication service is not available.")
        }
    }

    // Funkcija za log uspjesne prijavu
    private fun logLoginSuccess(
        method: String,
        userId: String,
    ) {
        CrashlyticsManager.setUserId(userId)
        analytics?.setUserId(userId)
        val bundle =
            Bundle().apply {
                putString(FirebaseAnalytics.Param.METHOD, method)
            }
        analytics?.logEvent(FirebaseAnalytics.Event.LOGIN, bundle)
    }

    // Funkcija za log  neuspjesne prijave
    private fun logLoginFailure(
        exception: Throwable?,
        method: String,
    ) {
        val bundle =
            Bundle().apply {
                putString("error_type", exception?.javaClass?.simpleName ?: "unknown")
                putString("method", method)
            }
        analytics?.logEvent("login_failed", bundle)
    }

    // Funkcija za log odjave
    private fun logLogout(method: String) {
        val logoutBundle =
            Bundle().apply {
                putString(FirebaseAnalytics.Param.METHOD, method)
            }
        analytics?.logEvent("logout", logoutBundle)
        analytics?.setUserId(null)
    }

    open fun checkSession(onSessionExpired: () -> Unit) {
        val user = auth?.currentUser
        user
            ?.getIdToken(true)
            ?.addOnFailureListener { exception ->
                CrashlyticsManager.logException(exception)
                if (exception is com.google.firebase.auth.FirebaseAuthInvalidUserException) {
                    auth.signOut()
                    onSessionExpired()
                }
            }
    }

    // QA helper-Funkcija za testiranje tokena
    fun testSecureRead(
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit,
    ) {
        val user =
            auth?.currentUser
                ?: return onError(IllegalStateException("No logged-in user"))
        val firestoreInstance =
            firestore
                ?: return onError(IllegalStateException("Firestore service is not available"))

        user
            .getIdToken(true) // 'true' prisiljava osvjezavanje i provjeru statusa
            .addOnSuccessListener {
                firestoreInstance
                    .collection("users")
                    .document(user.uid)
                    .get()
                    .addOnSuccessListener { onSuccess() }
                    .addOnFailureListener { e ->
                        CrashlyticsManager.logException(e)
                        onError(e)
                    }
            }.addOnFailureListener { e ->
                CrashlyticsManager.logException(e)
                onError(e)
            }
    }
}
