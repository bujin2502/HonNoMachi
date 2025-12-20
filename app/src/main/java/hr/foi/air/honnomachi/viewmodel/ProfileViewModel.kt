package hr.foi.air.honnomachi.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import hr.foi.air.honnomachi.FormValidator
import hr.foi.air.honnomachi.model.UserModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ProfileViewModel(
    private val auth: FirebaseAuth = Firebase.auth,
    private val firestore: FirebaseFirestore = Firebase.firestore
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _formState = MutableStateFlow(ProfileFormState())
    val formState: StateFlow<ProfileFormState> = _formState.asStateFlow()

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
                            // Initialize form state
                            _formState.update {
                                it.copy(
                                    name = user.name,
                                    phone = user.phoneNumber ?: "",
                                    street = user.street ?: "",
                                    city = user.city ?: "",
                                    zip = user.postNumber ?: "",
                                    nameError = null,
                                    phoneError = null,
                                    streetError = null,
                                    cityError = null,
                                    zipError = null
                                )
                            }
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

    // --- Form Update Methods ---

    fun onNameChange(newValue: String) {
        _formState.update { it.copy(name = newValue, nameError = null) }
    }

    fun onPhoneChange(newValue: String) {
         if (newValue.length <= 16) {
             _formState.update { it.copy(phone = newValue, phoneError = null) }
         }
    }

    fun onStreetChange(newValue: String) {
        _formState.update { it.copy(street = newValue, streetError = null) }
    }

    fun onCityChange(newValue: String) {
        _formState.update { it.copy(city = newValue, cityError = null) }
    }

    fun onZipChange(newValue: String) {
         if (newValue.length <= 5) {
             _formState.update { it.copy(zip = newValue, zipError = null) }
         }
    }

    // --- Validation Methods (e.g. on focus lost) ---

    fun validateName() {
        val result = FormValidator.validateName(_formState.value.name)
        _formState.update { it.copy(nameError = result.error) }
    }

    fun validatePhone() {
        val result = FormValidator.validatePhone(_formState.value.phone)
        _formState.update { it.copy(phoneError = result.error) }
    }

    fun validateStreet() {
        val result = FormValidator.validateStreet(_formState.value.street)
        _formState.update { it.copy(streetError = result.error) }
    }

    fun validateCity() {
        val result = FormValidator.validateCity(_formState.value.city)
        _formState.update { it.copy(cityError = result.error) }
    }

    fun validateZip() {
        val result = FormValidator.validateZip(_formState.value.zip)
        _formState.update { it.copy(zipError = result.error) }
    }

    // --- Save Logic ---

    fun saveProfile(onResult: (Boolean, String?) -> Unit) {
        val currentState = _formState.value
        val validation = FormValidator.validateProfileEditForm(
            currentState.name,
            currentState.phone,
            currentState.street,
            currentState.city,
            currentState.zip
        )

        if (!validation.isValid) {
            _formState.update {
                it.copy(
                    nameError = validation.name.error,
                    phoneError = validation.phone.error,
                    streetError = validation.street.error,
                    cityError = validation.city.error,
                    zipError = validation.zip.error
                )
            }
            onResult(false, "Molimo ispravno popunite sva polja.") // Or return specific error
            return
        }

        val currentUserState = _uiState.value
        if (currentUserState !is ProfileUiState.Success) return

        viewModelScope.launch {
            _formState.update { it.copy(isSaving = true) }
            try {
                val updatedUser = currentUserState.user.copy(
                    name = currentState.name,
                    phoneNumber = currentState.phone,
                    street = currentState.street,
                    postNumber = currentState.zip,
                    city = currentState.city
                )

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
                _formState.update { it.copy(isSaving = false) }
                onResult(true, null)
            } catch (e: Exception) {
                _formState.update { it.copy(isSaving = false) }
                onResult(false, e.message)
            }
        }
    }

    fun changePassword(oldPass: String, newPass: String, onResult: (Boolean, String?) -> Unit) {
        val user = auth.currentUser
        if (user == null || user.email == null) {
            onResult(false, "User not logged in.")
            return
        }

        val credential = EmailAuthProvider.getCredential(user.email!!, oldPass)

        user.reauthenticate(credential)
            .addOnCompleteListener { authTask ->
                if (authTask.isSuccessful) {
                    user.updatePassword(newPass)
                        .addOnCompleteListener { updateTask ->
                            if (updateTask.isSuccessful) {
                                onResult(true, null)
                            } else {
                                onResult(false, updateTask.exception?.message)
                            }
                        }
                } else {
                    onResult(false, authTask.exception?.message ?: "Re-authentication failed.")
                }
            }
    }

    fun checkSession(onInvalid: () -> Unit) {
        auth.currentUser?.getIdToken(true)
            ?.addOnFailureListener {
                onInvalid()
            }
    }
}
