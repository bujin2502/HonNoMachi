package hr.foi.air.honnomachi.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import hr.foi.air.honnomachi.model.UserModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ProfileViewModel(
    private val auth: FirebaseAuth = Firebase.auth,
    private val firestore: FirebaseFirestore = Firebase.firestore
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadUserProfile()
    }

    fun loadUserProfile() {
        viewModelScope.launch {
            _uiState.value = ProfileUiState.Loading
            val currentUser = auth.currentUser
            if (currentUser != null) {
                try {
                    val document = firestore.collection("users").document(currentUser.uid).get().await()
                    if (document.exists()) {
                        val user = document.toObject(UserModel::class.java)
                        if (user != null) {
                            _uiState.value = ProfileUiState.Success(user)
                        } else {
                            _uiState.value = ProfileUiState.Error("Failed to parse user data.")
                        }
                    } else {
                        _uiState.value = ProfileUiState.Error("User document not found.")
                    }
                } catch (e: Exception) {
                    _uiState.value = ProfileUiState.Error(e.message ?: "An error occurred.")
                }
            } else {
                _uiState.value = ProfileUiState.Error("No user logged in.")
            }
        }
    }

    fun updateUserProfile(updatedUser: UserModel, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                val updates = mapOf(
                    "name" to updatedUser.name,
                    "phoneNumber" to updatedUser.phoneNumber,
                    "street" to updatedUser.street,
                    "postNumber" to updatedUser.postNumber,
                    "city" to updatedUser.city
                )

                firestore.collection("users").document(updatedUser.uid)
                    .update(updates)
                    .await()

                _uiState.value = ProfileUiState.Success(updatedUser)
                onResult(true, null)
            } catch (e: Exception) {
                onResult(false, e.message)
            }
        }
    }
}