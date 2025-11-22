package hr.foi.air.honnomachi

private val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")

enum class ValidationErrorType {
    EMPTY_EMAIL,
    INVALID_EMAIL,
    EMPTY_NAME,
    SHORT_NAME,
    SHORT_PASSWORD
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
        if (password.length < 6) {
            return ValidationResult(isValid = false, error = ValidationErrorType.SHORT_PASSWORD)
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
}
