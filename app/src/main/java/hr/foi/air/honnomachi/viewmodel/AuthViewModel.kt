package hr.foi.air.honnomachi.viewmodel

import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import hr.foi.air.honnomachi.model.UserModel

class AuthViewModel : ViewModel() {

    private val auth = Firebase.auth

    private val firestore = Firebase.firestore


    fun signup(
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

    fun login(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
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

    fun signOut() {
        auth.signOut()
    }
}