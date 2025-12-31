package hr.foi.air.honnomachi.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import hr.foi.air.honnomachi.data.AuthRepository
import hr.foi.air.honnomachi.util.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel
    @Inject
    constructor(
        private val authRepository: AuthRepository,
        private val firebaseAuth: FirebaseAuth,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(AuthUiState())
        val uiState = _uiState.asStateFlow()

        init {
            // Sluša promjene stanja prijave (login, logout, istek tokena)
            firebaseAuth.addAuthStateListener { auth ->
                viewModelScope.launch {
                    val user = auth.currentUser
                    if (user == null) {
                        _uiState.update { it.copy(isUserLoggedIn = false, needsVerification = false) }
                    } else {
                        // Firebase može imati zastarjele podatke, pozovi reload() za svježe stanje
                        user.reload().addOnCompleteListener { task ->
                            val freshUser = firebaseAuth.currentUser
                            if (task.isSuccessful && freshUser != null) {
                                val isVerified = freshUser.isEmailVerified
                                _uiState.update {
                                    it.copy(
                                        isUserLoggedIn = isVerified,
                                        needsVerification = !isVerified,
                                    )
                                }
                            } else {
                                // Ako reload ne uspije (npr. nema interneta, token istekao),
                                // smatraj korisnika odjavljenim za svaki slučaj.
                                _uiState.update { it.copy(isUserLoggedIn = false, needsVerification = false) }
                            }
                        }
                    }
                }
            }
        }

        fun signup(
            name: String,
            email: String,
            password: String,
        ) {
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
                val result = authRepository.register(name, email, password)
                when (result) {
                    is Result.Success -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                user = result.data,
                                needsVerification = true,
                                errorMessage = null,
                            )
                        }
                    }

                    is Result.Error -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = result.exception.message,
                            )
                        }
                    }
                }
            }
        }

        fun login(
            email: String,
            password: String,
        ) {
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
                val result = authRepository.login(email, password)
                when (result) {
                    is Result.Success -> {
                        val isVerified = result.data.isVerified
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                user = result.data,
                                isUserLoggedIn = isVerified,
                                needsVerification = !isVerified,
                                errorMessage = if (isVerified) null else "Please verify your email.",
                            )
                        }
                    }

                    is Result.Error -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = result.exception.message,
                            )
                        }
                    }
                }
            }
        }

        fun signOut() {
            viewModelScope.launch {
                authRepository.signOut()
                _uiState.update { AuthUiState() } // Reset state
            }
        }

        fun forgotPassword(email: String) {
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
                val result = authRepository.sendPasswordResetEmail(email)
                _uiState.update {
                    when (result) {
                        is Result.Success -> {
                            it.copy(isLoading = false, errorMessage = null)
                        }

                        is Result.Error -> {
                            it.copy(
                                isLoading = false,
                                errorMessage = result.exception.message,
                            )
                        }
                    }
                }
            }
        }

        fun consumeErrorMessage() {
            _uiState.update { it.copy(errorMessage = null) }
        }

        fun checkSession() {
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
                val result = authRepository.checkSession()
                when (result) {
                    is Result.Success -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                user = result.data,
                                isUserLoggedIn = true,
                                needsVerification = false,
                                errorMessage = null,
                            )
                        }
                    }

                    is Result.Error -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isUserLoggedIn = false,
                                needsVerification = false,
                                errorMessage = result.exception.message,
                            )
                        }
                    }
                }
            }
        }

        fun testSecureRead(
            onSuccess: (String) -> Unit,
            onError: (String) -> Unit,
        ) {
            viewModelScope.launch {
                val result = authRepository.testSecureRead()
                when (result) {
                    is Result.Success -> onSuccess(result.data)
                    is Result.Error -> onError(result.exception.message ?: "Unknown error")
                }
            }
        }

        fun loginWithGoogle(
            idToken: String,
            onComplete: (success: Boolean, errorMessage: String?) -> Unit,
        ) {
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
                val result = authRepository.loginWithGoogle(idToken)
                when (result) {
                    is Result.Success -> {
                        val isVerified = result.data.isVerified
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                user = result.data,
                                isUserLoggedIn = true,
                                needsVerification = !isVerified,
                                errorMessage = null,
                            )
                        }
                        onComplete(true, null)
                    }

                    is Result.Error -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = result.exception.message,
                            )
                        }
                        onComplete(false, result.exception.message)
                    }
                }
            }
        }

        fun resendVerificationEmail(
            email: String,
            password: String,
            onComplete: (success: Boolean, message: String) -> Unit,
        ) {
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
                val result = authRepository.resendVerificationEmail(email, password)
                when (result) {
                    is Result.Success -> {
                        _uiState.update { it.copy(isLoading = false, errorMessage = null) }
                        onComplete(true, "Verification email sent successfully")
                    }

                    is Result.Error -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = result.exception.message,
                            )
                        }
                        onComplete(false, result.exception.message ?: "Failed to send verification email")
                    }
                }
            }
        }
    }
