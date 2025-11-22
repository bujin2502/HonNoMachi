package hr.foi.air.honnomachi.viewmodel

import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import hr.foi.air.honnomachi.model.UserModel

open class AuthViewModel : ViewModel() {

    private val auth = Firebase.auth

    private val firestore = Firebase.firestore


    open fun signup(
        email: String,
        name: String,
        password: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
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
                                firestore.collection("users")
                                    .document(userId)
                                    .set(userModel)
                                    .addOnCompleteListener { dbTask ->
                                        if (dbTask.isSuccessful) {
                                            auth.signOut()
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
            }
    }

    open fun login(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = task.result?.user
                    if (user?.isEmailVerified == true) {
                        val userId = user.uid
                        firestore.collection("users").document(userId)
                            .update("isVerified", true)
                            .addOnCompleteListener { updateTask ->
                                if (updateTask.isSuccessful) {
                                    onResult(true, null)
                                } else {
                                    onResult(false, "Failed to update verification status.")
                                }
                            }
                    } else {
                        auth.signOut()
                        onResult(false, "Please verify your email before logging in.")
                    }
                } else {
                    onResult(false, task.exception?.localizedMessage)
                }
            }
    }

    open fun signOut() {
        auth.signOut()
    }

    open fun forgotPassword(email: String, onResult: (Boolean, String?) -> Unit) {
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onResult(true, null)
                } else {
                    onResult(false, task.exception?.localizedMessage)
                }
            }
    }

    open fun resendVerificationEmail(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = task.result?.user
                    if (user?.isEmailVerified == false) {
                        user.sendEmailVerification()
                            .addOnCompleteListener { verificationTask ->
                                if (verificationTask.isSuccessful) {
                                    auth.signOut()
                                    onResult(true, "Verification email sent. Please check your inbox.")
                                } else {
                                    auth.signOut()
                                    onResult(false, "Failed to send verification email.")
                                }
                            }
                    } else {
                        auth.signOut()
                        onResult(false, "This email is already verified.")
                    }
                } else {
                    onResult(false, task.exception?.localizedMessage)
                }
            }
    }
}
