package hr.foi.air.honnomachi

private val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")
// Regex patterns
private val nameRegex = Regex("""^[\p{L} .'-]+$""")
private val phoneRegex = Regex("""^\+?[0-9]{9,15}$""")
private val streetRegex = Regex("""^[\p{L}\s\.-]+ \d+[A-Za-z]?$""")
private val cityRegex = Regex("""^[\p{L}\s-]+$""")
private val zipRegex = Regex("""^\d{5}$""")
private val passwordRegex = Regex("""^(?=.*[A-Z])(?=.*\d)(?=.*[!@#\$%^&*()_+\-=\[\]{};':"\\|,.<>/?]).{6,}$""")


enum class ValidationErrorType {
    EMPTY_EMAIL,
    INVALID_EMAIL,
    EMPTY_NAME,
    SHORT_NAME,
    INVALID_NAME_FORMAT,
    EMPTY_PASSWORD,
    SHORT_PASSWORD,
    WEAK_PASSWORD,
    EMPTY_PHONE,
    INVALID_PHONE_FORMAT,
    EMPTY_STREET,
    INVALID_STREET_FORMAT,
    EMPTY_CITY,
    INVALID_CITY_FORMAT,
    EMPTY_ZIP,
    INVALID_ZIP_FORMAT,
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
        if (!nameRegex.matches(name)) {
             return ValidationResult(isValid = false, error = ValidationErrorType.INVALID_NAME_FORMAT)
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

    fun validateStrictPassword(password: String): ValidationResult {
        if (password.isBlank()) {
            return ValidationResult(isValid = false, error = ValidationErrorType.EMPTY_PASSWORD)
        }
        if (!passwordRegex.matches(password)) {
            return ValidationResult(isValid = false, error = ValidationErrorType.WEAK_PASSWORD)
        }
        return ValidationResult(isValid = true)
    }

    fun validatePhone(phone: String): ValidationResult {
        if (phone.isBlank()) {
            return ValidationResult(isValid = false, error = ValidationErrorType.EMPTY_PHONE)
        }
        if (!phoneRegex.matches(phone)) {
            return ValidationResult(isValid = false, error = ValidationErrorType.INVALID_PHONE_FORMAT)
        }
        return ValidationResult(isValid = true)
    }

    fun validateStreet(street: String): ValidationResult {
        if (street.isBlank()) {
            return ValidationResult(isValid = false, error = ValidationErrorType.EMPTY_STREET)
        }
        if (!streetRegex.matches(street)) {
            return ValidationResult(isValid = false, error = ValidationErrorType.INVALID_STREET_FORMAT)
        }
        return ValidationResult(isValid = true)
    }

    fun validateCity(city: String): ValidationResult {
        if (city.isBlank()) {
            return ValidationResult(isValid = false, error = ValidationErrorType.EMPTY_CITY)
        }
        if (!cityRegex.matches(city)) {
            return ValidationResult(isValid = false, error = ValidationErrorType.INVALID_CITY_FORMAT)
        }
        return ValidationResult(isValid = true)
    }

    fun validateZip(zip: String): ValidationResult {
        if (zip.isBlank()) {
            return ValidationResult(isValid = false, error = ValidationErrorType.EMPTY_ZIP)
        }
        if (!zipRegex.matches(zip)) {
            return ValidationResult(isValid = false, error = ValidationErrorType.INVALID_ZIP_FORMAT)
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
        val passwordValidation = validatePassword(password) // Signup might use strict, but keeping consistent with existing logic for now unless requested. User only mentioned changePasswordScreen specifically for strict password.
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
            oldPassword = validatePassword(oldPass), // Just needs to be non-empty/valid length format, not strictly regex check as old password might predate rules
            newPassword = validateStrictPassword(newPass),
            confirmPassword = validatePasswordConfirmation(newPass, confirmPass)
        )
    }
}