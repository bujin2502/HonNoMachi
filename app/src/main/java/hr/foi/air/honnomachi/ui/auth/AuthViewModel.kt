package hr.foi.air.honnomachi.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.lifecycle.HiltViewModel
import hr.foi.air.honnomachi.data.AuthRepository
import hr.foi.air.honnomachi.data.FirestoreUserDataSource
import hr.foi.air.honnomachi.util.Result
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
open class AuthViewModel
    @Inject
    constructor(
        private val authRepository: AuthRepository,
        private val firebaseAuth: FirebaseAuth,
        private val userDataSource: FirestoreUserDataSource,
    ) : ViewModel() {
        private var suspensionMonitorJob: Job? = null
        private val _uiState = MutableStateFlow(AuthUiState())
        open val uiState = _uiState.asStateFlow()

        init {
            firebaseAuth.addAuthStateListener { auth ->
                viewModelScope.launch {
                    handleAuthStateChange(auth.currentUser)
                }
            }
        }

        private suspend fun handleAuthStateChange(user: FirebaseUser?) {
            if (user == null) {
                suspensionMonitorJob?.cancel()
                _uiState.update {
                    it.copy(isUserLoggedIn = false, needsVerification = false)
                }
                return
            }

            try {
                user.reload().await()
                val freshUser = firebaseAuth.currentUser
                if (freshUser != null) {
                    val isVerified = freshUser.isEmailVerified
                    if (isVerified) {
                        val userData = userDataSource.getUser(freshUser.uid)
                        if (userData?.suspended == true) {
                            authRepository.signOut()
                            _uiState.update {
                                it.copy(
                                    isUserLoggedIn = false,
                                    needsVerification = false,
                                    isSuspended = true,
                                    suspendedReason = userData.suspendedReason,
                                )
                            }
                            return
                        }
                        startSuspensionMonitor(freshUser.uid)
                    }
                    _uiState.update {
                        it.copy(
                            isUserLoggedIn = isVerified,
                            needsVerification = !isVerified,
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(isUserLoggedIn = false, needsVerification = false)
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isUserLoggedIn = false, needsVerification = false)
                }
            }
        }

        open fun signup(
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

        open fun login(
            email: String,
            password: String,
        ) {
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
                val result = authRepository.login(email, password)
                when (result) {
                    is Result.Success -> {
                        val isVerified = result.data.isVerified
                        if (isVerified && result.data.suspended == true) {
                            authRepository.signOut()
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    isSuspended = true,
                                    suspendedReason = result.data.suspendedReason,
                                    isUserLoggedIn = false,
                                )
                            }
                            return@launch
                        }
                        if (isVerified) {
                            startSuspensionMonitor(result.data.uid)
                        }
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                user = result.data,
                                isUserLoggedIn = isVerified,
                                needsVerification = !isVerified,
                                errorMessage = if (isVerified) null else ErrorMessages.VERIFY_EMAIL,
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

        open fun signOut() {
            suspensionMonitorJob?.cancel()
            viewModelScope.launch {
                authRepository.signOut()
                _uiState.update { AuthUiState() }
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
                        if (result.data.suspended == true) {
                            authRepository.signOut()
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    isSuspended = true,
                                    suspendedReason = result.data.suspendedReason,
                                    isUserLoggedIn = false,
                                )
                            }
                            return@launch
                        }
                        startSuspensionMonitor(result.data.uid)
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

        fun testSecureRead() {
            viewModelScope.launch {
                val result = authRepository.testSecureRead()
                when (result) {
                    is Result.Success -> {
                        _uiState.update { it.copy(secureReadResult = result.data) }
                    }
                    is Result.Error -> {
                        _uiState.update {
                            it.copy(
                                secureReadResult = null,
                                errorMessage = result.exception.message ?: ErrorMessages.UNKNOWN_ERROR,
                            )
                        }
                    }
                }
            }
        }

        fun consumeSecureReadResult() {
            _uiState.update { it.copy(secureReadResult = null) }
        }

        fun loginWithGoogle(idToken: String) {
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
                val result = authRepository.loginWithGoogle(idToken)
                when (result) {
                    is Result.Success -> {
                        val isVerified = result.data.isVerified
                        if (result.data.suspended == true) {
                            authRepository.signOut()
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    isSuspended = true,
                                    suspendedReason = result.data.suspendedReason,
                                    isUserLoggedIn = false,
                                )
                            }
                            return@launch
                        }
                        startSuspensionMonitor(result.data.uid)
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                user = result.data,
                                isUserLoggedIn = true,
                                needsVerification = !isVerified,
                                errorMessage = null,
                                googleLoginResult = OperationResult(true, SuccessMessages.GOOGLE_LOGIN_SUCCESS),
                            )
                        }
                    }

                    is Result.Error -> {
                        val errorMsg = result.exception.message ?: ErrorMessages.UNKNOWN_ERROR
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = result.exception.message,
                                googleLoginResult = OperationResult(false, errorMsg),
                            )
                        }
                    }
                }
            }
        }

        fun consumeGoogleLoginResult() {
            _uiState.update { it.copy(googleLoginResult = null) }
        }

        open fun resendVerificationEmail(
            email: String,
            password: String,
        ) {
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
                val result = authRepository.resendVerificationEmail(email, password)
                when (result) {
                    is Result.Success -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = null,
                                verificationEmailResult =
                                    OperationResult(true, SuccessMessages.VERIFICATION_EMAIL_SENT),
                            )
                        }
                    }

                    is Result.Error -> {
                        val errorMsg = result.exception.message ?: ErrorMessages.VERIFICATION_EMAIL_FAILED
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = result.exception.message,
                                verificationEmailResult = OperationResult(false, errorMsg),
                            )
                        }
                    }
                }
            }
        }

        fun consumeVerificationEmailResult() {
            _uiState.update { it.copy(verificationEmailResult = null) }
        }

        fun checkVerificationStatus() {
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true, errorMessage = null) }
                val result = authRepository.syncVerificationStatus()
                when (result) {
                    is Result.Success -> {
                        authRepository.signOut()
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                user = null,
                                isUserLoggedIn = false,
                                needsVerification = false,
                                errorMessage = null,
                                verificationStatusResult =
                                    OperationResult(true, SuccessMessages.EMAIL_VERIFIED),
                            )
                        }
                    }

                    is Result.Error -> {
                        val errorMsg = result.exception.message ?: ErrorMessages.EMAIL_NOT_VERIFIED
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = result.exception.message,
                                verificationStatusResult = OperationResult(false, errorMsg),
                            )
                        }
                    }
                }
            }
        }

        fun consumeVerificationStatusResult() {
            _uiState.update { it.copy(verificationStatusResult = null) }
        }

        /** Briše stanje suspenzije nakon što korisnik vidi ekran suspenzije. */
        fun consumeSuspendedState() {
            _uiState.update { it.copy(isSuspended = false, suspendedReason = null) }
        }

        /**
         * Pokreće real-time praćenje suspenzije putem Firestore snapshot listenera.
         *
         * Ako se korisnik suspendira dok koristi aplikaciju,
         * automatski ga odjavljuje i prikazuje ekran suspenzije.
         */
        private fun startSuspensionMonitor(userId: String) {
            suspensionMonitorJob?.cancel()
            suspensionMonitorJob =
                viewModelScope.launch {
                    userDataSource.observeUser(userId).collect { userData ->
                        if (userData?.suspended == true) {
                            authRepository.signOut()
                            _uiState.update {
                                it.copy(
                                    isSuspended = true,
                                    suspendedReason = userData.suspendedReason,
                                    isUserLoggedIn = false,
                                )
                            }
                            suspensionMonitorJob?.cancel()
                        }
                    }
                }
        }

        object ErrorMessages {
            const val VERIFY_EMAIL = "Please verify your email."
            const val UNKNOWN_ERROR = "Unknown error"
            const val VERIFICATION_EMAIL_FAILED = "Failed to send verification email"
            const val EMAIL_NOT_VERIFIED = "Email not yet verified"
        }

        object SuccessMessages {
            const val VERIFICATION_EMAIL_SENT = "Verification email sent successfully"
            const val EMAIL_VERIFIED = "Email verified! Please log in again to continue."
            const val GOOGLE_LOGIN_SUCCESS = "Google login successful"
        }
    }
