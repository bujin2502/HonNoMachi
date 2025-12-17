package hr.foi.air.honnomachi

private val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")

enum class ValidationErrorType {
    EMPTY_EMAIL,
    INVALID_EMAIL,
    EMPTY_NAME,
    SHORT_NAME,
    EMPTY_PASSWORD,
    SHORT_PASSWORD,
    EMPTY_PHONE,
    EMPTY_STREET,
    EMPTY_CITY,
    EMPTY_ZIP,
    PASSWORDS_DO_NOT_MATCH
}

data class ValidationResult(
    val isValid: Boolean,
    val error: ValidationErrorType? = null
)

data class SignupValidationResult(
    val email: ValidationResult,
    val name: ValidationResult,
    val password: ValidationResult
) {
    val isValid: Boolean = email.isValid && name.isValid && password.isValid
}

data class LoginValidationResult(
    val email: ValidationResult,
    val password: ValidationResult
) {
    val isValid: Boolean = email.isValid && password.isValid
}

data class ProfileEditValidationResult(
    val name: ValidationResult,
    val phone: ValidationResult,
    val street: ValidationResult,
    val city: ValidationResult,
    val zip: ValidationResult
) {
    val isValid: Boolean = name.isValid && phone.isValid && street.isValid && city.isValid && zip.isValid
}

data class ChangePasswordValidationResult(
    val oldPassword: ValidationResult,
    val newPassword: ValidationResult,
    val confirmPassword: ValidationResult
) {
    val isValid: Boolean = oldPassword.isValid && newPassword.isValid && confirmPassword.isValid
}

object FormValidator {
    fun validateEmail(email: String): ValidationResult {
        if (email.isBlank()) {
            return ValidationResult(isValid = false, error = ValidationErrorType.EMPTY_EMAIL)
        }
        if (!emailRegex.matches(email)) {
            return ValidationResult(isValid = false, error = ValidationErrorType.INVALID_EMAIL)
        }
        return ValidationResult(isValid = true)
    }

    fun validateName(name: String): ValidationResult {
        if (name.isBlank()) {
            return ValidationResult(isValid = false, error = ValidationErrorType.EMPTY_NAME)
        }
        if (name.length < 2) {
            return ValidationResult(isValid = false, error = ValidationErrorType.SHORT_NAME)
        }
        return ValidationResult(isValid = true)
    }

    fun validatePassword(password: String): ValidationResult {
        if (password.isBlank()) {
            return ValidationResult(isValid = false, error = ValidationErrorType.EMPTY_PASSWORD)
        }
        if (password.length < 6) {
            return ValidationResult(isValid = false, error = ValidationErrorType.SHORT_PASSWORD)
        }
        return ValidationResult(isValid = true)
    }

    fun validatePhone(phone: String): ValidationResult {
        if (phone.isBlank()) {
            return ValidationResult(isValid = false, error = ValidationErrorType.EMPTY_PHONE)
        }
        return ValidationResult(isValid = true)
    }

    fun validateStreet(street: String): ValidationResult {
        if (street.isBlank()) {
            return ValidationResult(isValid = false, error = ValidationErrorType.EMPTY_STREET)
        }
        return ValidationResult(isValid = true)
    }

    fun validateCity(city: String): ValidationResult {
        if (city.isBlank()) {
            return ValidationResult(isValid = false, error = ValidationErrorType.EMPTY_CITY)
        }
        return ValidationResult(isValid = true)
    }

    fun validateZip(zip: String): ValidationResult {
        if (zip.isBlank()) {
            return ValidationResult(isValid = false, error = ValidationErrorType.EMPTY_ZIP)
        }
        return ValidationResult(isValid = true)
    }

    fun validatePasswordConfirmation(password: String, confirm: String): ValidationResult {
        if (confirm.isBlank()) {
            return ValidationResult(isValid = false, error = ValidationErrorType.EMPTY_PASSWORD) // Confirm field is empty
        }
        if (password != confirm) {
            return ValidationResult(isValid = false, error = ValidationErrorType.PASSWORDS_DO_NOT_MATCH)
        }
        return ValidationResult(isValid = true)
    }

    fun validateSignupForm(email: String, name: String, password: String): SignupValidationResult {
        val emailValidation = validateEmail(email)
        val nameValidation = validateName(name)
        val passwordValidation = validatePassword(password)
        return SignupValidationResult(
            email = emailValidation,
            name = nameValidation,
            password = passwordValidation
        )
    }

    fun validateLoginForm(email: String, password: String): LoginValidationResult {
        val emailValidation = validateEmail(email)
        val passwordValidation = validatePassword(password)
        return LoginValidationResult(
            email = emailValidation,
            password = passwordValidation
        )
    }

    fun validateProfileEditForm(name: String, phone: String, street: String, city: String, zip: String): ProfileEditValidationResult {
        return ProfileEditValidationResult(
            name = validateName(name),
            phone = validatePhone(phone),
            street = validateStreet(street),
            city = validateCity(city),
            zip = validateZip(zip)
        )
    }

    fun validateChangePasswordForm(oldPass: String, newPass: String, confirmPass: String): ChangePasswordValidationResult {
        return ChangePasswordValidationResult(
            oldPassword = validatePassword(oldPass), // Reuse basic validation for old pass
            newPassword = validatePassword(newPass),
            confirmPassword = validatePasswordConfirmation(newPass, confirmPass)
        )
    }
}
