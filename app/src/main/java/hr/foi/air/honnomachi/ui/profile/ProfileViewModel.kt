package hr.foi.air.honnomachi.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import hr.foi.air.honnomachi.FormValidator
import hr.foi.air.honnomachi.data.ProfileRepository
import hr.foi.air.honnomachi.util.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
open class ProfileViewModel
    @Inject
    constructor(
        private val profileRepository: ProfileRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
        val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

        private val _formState = MutableStateFlow(ProfileFormState())
        val formState: StateFlow<ProfileFormState> = _formState.asStateFlow()

        init {
            loadUserProfile()
        }

        open fun loadUserProfile() {
            viewModelScope.launch {
                _uiState.value = ProfileUiState.Loading
                when (val result = profileRepository.getUserProfile()) {
                    is Result.Success -> {
                        val user = result.data
                        _uiState.value = ProfileUiState.Success(user)
                        // Initialize form state
                        _formState.update {
                            it.copy(
                                name = user.name,
                                phone = user.phoneNumber ?: "",
                                street = user.street ?: "",
                                city = user.city ?: "",
                                zip = user.postNumber ?: "",
                                analyticsEnabled = user.analyticsEnabled,
                                nameError = null,
                                phoneError = null,
                                streetError = null,
                                cityError = null,
                                zipError = null,
                            )
                        }
                    }
                    is Result.Error -> {
                        _uiState.value = ProfileUiState.Error(result.exception.message ?: "An error occurred.")
                    }
                }
            }
        }

        // --- Form Update Methods ---

        open fun onNameChange(newValue: String) {
            _formState.update { it.copy(name = newValue, nameError = null) }
        }

        open fun onPhoneChange(newValue: String) {
            if (newValue.length <= 16) {
                _formState.update { it.copy(phone = newValue, phoneError = null) }
            }
        }

        open fun onStreetChange(newValue: String) {
            _formState.update { it.copy(street = newValue, streetError = null) }
        }

        open fun onCityChange(newValue: String) {
            _formState.update { it.copy(city = newValue, cityError = null) }
        }

        open fun onZipChange(newValue: String) {
            if (newValue.length <= 5) {
                _formState.update { it.copy(zip = newValue, zipError = null) }
            }
        }

        // Postavke privatnosti switch button
        open fun onAnalyticsToggled(isEnabled: Boolean) {
            _formState.update { it.copy(analyticsEnabled = isEnabled) }

            viewModelScope.launch {
                when (val result = profileRepository.updateAnalyticsSetting(isEnabled)) {
                    is Result.Success -> {
                        val uiStateValue = _uiState.value
                        if (uiStateValue is ProfileUiState.Success) {
                            val updatedUser = uiStateValue.user.copy(analyticsEnabled = isEnabled)
                            _uiState.value = ProfileUiState.Success(updatedUser)
                        }
                    }
                    is Result.Error -> {
                        _formState.update { it.copy(analyticsEnabled = !isEnabled) }
                    }
                }
            }
        }

        // --- Validation Methods (e.g. on focus lost) ---

        open fun validateName() {
            val result = FormValidator.validateName(_formState.value.name)
            _formState.update { it.copy(nameError = result.error) }
        }

        open fun validatePhone() {
            val result = FormValidator.validatePhone(_formState.value.phone)
            _formState.update { it.copy(phoneError = result.error) }
        }

        open fun validateStreet() {
            val result = FormValidator.validateStreet(_formState.value.street)
            _formState.update { it.copy(streetError = result.error) }
        }

        open fun validateCity() {
            val result = FormValidator.validateCity(_formState.value.city)
            _formState.update { it.copy(cityError = result.error) }
        }

        open fun validateZip() {
            val result = FormValidator.validateZip(_formState.value.zip)
            _formState.update { it.copy(zipError = result.error) }
        }

        // --- Save Logic ---

        open fun saveProfile(onResult: (Boolean, String?) -> Unit) {
            val currentState = _formState.value
            val validation =
                FormValidator.validateProfileEditForm(
                    currentState.name,
                    currentState.phone,
                    currentState.street,
                    currentState.city,
                    currentState.zip,
                )

            if (!validation.isValid) {
                _formState.update {
                    it.copy(
                        nameError = validation.name.error,
                        phoneError = validation.phone.error,
                        streetError = validation.street.error,
                        cityError = validation.city.error,
                        zipError = validation.zip.error,
                    )
                }
                onResult(false, "Molimo ispravno popunite sva polja.") // Or return specific error
                return
            }

            val currentUserState = _uiState.value
            if (currentUserState !is ProfileUiState.Success) return

            viewModelScope.launch {
                _formState.update { it.copy(isSaving = true) }
                when (
                    val result =
                        profileRepository.updateUserProfile(
                            name = currentState.name,
                            phoneNumber = currentState.phone,
                            street = currentState.street,
                            postNumber = currentState.zip,
                            city = currentState.city,
                        )
                ) {
                    is Result.Success -> {
                        _uiState.value = ProfileUiState.Success(result.data)
                        _formState.update { it.copy(isSaving = false) }
                        onResult(true, null)
                    }
                    is Result.Error -> {
                        _formState.update { it.copy(isSaving = false) }
                        onResult(false, result.exception.message)
                    }
                }
            }
        }

        open fun changePassword(
            oldPass: String,
            newPass: String,
            onResult: (Boolean, String?) -> Unit,
        ) {
            viewModelScope.launch {
                when (val result = profileRepository.changePassword(oldPass, newPass)) {
                    is Result.Success -> {
                        onResult(true, null)
                    }
                    is Result.Error -> {
                        onResult(false, result.exception.message)
                    }
                }
            }
        }
    }
