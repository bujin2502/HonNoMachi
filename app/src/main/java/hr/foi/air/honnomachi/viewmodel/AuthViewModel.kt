package hr.foi.air.honnomachi.viewmodel

import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import hr.foi.air.honnomachi.model.UserModel

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

open class AuthViewModel (
    private val auth: FirebaseAuth? = Firebase.auth,
    private val firestore: FirebaseFirestore? = Firebase.firestore,
    private val analytics: FirebaseAnalytics? = Firebase.analytics
): ViewModel() {

    open fun signup(
        email: String,
        name: String,
        password: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        if (email.isBlank() || name.isBlank() || password.isBlank()) {
            onResult(false, "Email, name, and password cannot be empty.")
            return
        }
        auth?.createUserWithEmailAndPassword(email, password)
            ?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = task.result?.user
                    user?.sendEmailVerification()
                        ?.addOnCompleteListener { verificationTask ->
                            if (verificationTask.isSuccessful) {
                                val userId = user.uid
                                val userModel = UserModel(
                                    name = name,
                                    email = email,
                                    uid = userId,
                                    isVerified = false
                                )
                                firestore?.collection("users")
                                    ?.document(userId)
                                    ?.set(userModel)
                                    ?.addOnCompleteListener { dbTask ->
                                        if (dbTask.isSuccessful) {
                                            auth?.signOut()
                                            onResult(true, null)
                                        } else {
                                            onResult(false, "Database error.")
                                        }
                                    }
                            } else {
                                onResult(false, "Failed to send verification email.")
                            }
                        }
                } else {
                    onResult(false, task.exception?.localizedMessage)
                }
                } ?: run {
                onResult(false, "Authentication service is not available.")
            }
    }

    open fun login(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        if (email.isBlank() || password.isBlank()) {
            onResult(false, "Email and password cannot be empty.")
            return
        }
        auth?.signInWithEmailAndPassword(email, password)
            ?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = task.result?.user
                    if (user?.isEmailVerified == true) {
                        val userId = user.uid
                        firestore?.collection("users")?.document(userId)
                            ?.update("isVerified", true)
                            ?.addOnCompleteListener { updateTask ->
                                if (updateTask.isSuccessful) {
                                    logLoginSuccess("email_password", user.uid)
                                    onResult(true, null)
                                } else {
                                    onResult(false, "Failed to update verification status.")
                                }
                            }
                    } else {
                        auth?.signOut()
                        onResult(false, "Please verify your email before logging in.")
                    }
                } else {
                    logLoginFailure(task.exception, "email_password")
                    onResult(false, task.exception?.localizedMessage)
                }
                } ?: run {
                onResult(false, "Authentication service is not available.")
            }
    }

    open fun signOut() {
        logLogout("user_logout")
        auth?.signOut()
    }

    open fun forgotPassword(email: String, onResult: (Boolean, String?) -> Unit) {
        if (email.isBlank()) {
            onResult(false, "Email cannot be empty.")
            return
        }
        auth?.sendPasswordResetEmail(email)
            ?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onResult(true, null)
                } else {
                    onResult(false, task.exception?.localizedMessage)
                }
                } ?: run {
                onResult(false, "Authentication service is not available.")
            }
    }

    open fun resendVerificationEmail(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        auth?.signInWithEmailAndPassword(email, password)
            ?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = task.result?.user
                    if (user?.isEmailVerified == false) {
                        user.sendEmailVerification()
                            .addOnCompleteListener { verificationTask ->
                                if (verificationTask.isSuccessful) {
                                    auth?.signOut()
                                    onResult(true, "Verification email sent. Please check your inbox.")
                                } else {
                                    auth?.signOut()
                                    onResult(false, "Failed to send verification email.")
                                }
                            }
                    } else {
                        auth?.signOut()
                        onResult(false, "This email is already verified.")
                    }
                } else {
                    onResult(false, task.exception?.localizedMessage)
                }
                } ?: run {
                onResult(false, "Authentication service is not available.")
            }
    }

  // Funkcija za log uspjesne prijavu
    private fun logLoginSuccess(method: String, userId: String) {
        analytics?.setUserId(userId)
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.METHOD, method)
        }
        analytics?.logEvent(FirebaseAnalytics.Event.LOGIN, bundle)
    }

    // Funkcija za log  neuspjesne prijave
    private fun logLoginFailure(exception: Throwable?, method: String) {
        val bundle = Bundle().apply {
            putString("error_type", exception?.javaClass?.simpleName ?: "unknown")
            putString("method", method)
        }
        analytics?.logEvent("login_failed", bundle)
    }

    // Funkcija za log odjave
    private fun logLogout(method: String) {
        val logoutBundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.METHOD, method)
        }
        analytics?.logEvent("logout", logoutBundle)
        analytics?.setUserId(null)
    }
}
