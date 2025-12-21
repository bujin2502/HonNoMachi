package hr.foi.air.honnomachi

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FormValidatorTest {
    // --- Email Tests ---
    @Test
    fun `empty email fails with required error`() {
        val result = FormValidator.validateEmail("")
        assertFalse(result.isValid)
        assertEquals(ValidationErrorType.EMPTY_EMAIL, result.error)
    }

    @Test
    fun `invalid email format is flagged`() {
        val result = FormValidator.validateEmail("invalid-email")
        assertFalse(result.isValid)
        assertEquals(ValidationErrorType.INVALID_EMAIL, result.error)
    }

    @Test
    fun `valid email passes`() {
        val result = FormValidator.validateEmail("user@example.com")
        assertTrue(result.isValid)
        assertEquals(null, result.error)
    }

    // --- Name Tests ---
    @Test
    fun `short name fails validation`() {
        val result = FormValidator.validateName("A")
        assertFalse(result.isValid)
        assertEquals(ValidationErrorType.SHORT_NAME, result.error)
    }

    @Test
    fun `blank name fails validation`() {
        val result = FormValidator.validateName("")
        assertFalse(result.isValid)
        assertEquals(ValidationErrorType.EMPTY_NAME, result.error)
    }

    @Test
    fun `invalid name format fails`() {
        val result = FormValidator.validateName("User123") // Numbers not allowed
        assertFalse(result.isValid)
        assertEquals(ValidationErrorType.INVALID_NAME_FORMAT, result.error)
    }

    @Test
    fun `valid name passes`() {
        val result = FormValidator.validateName("John Doe")
        assertTrue(result.isValid)
    }

    // --- Basic Password Tests ---
    @Test
    fun `blank password fails validation`() {
        val result = FormValidator.validatePassword("")
        assertFalse(result.isValid)
        assertEquals(ValidationErrorType.EMPTY_PASSWORD, result.error)
    }

    @Test
    fun `short password fails validation`() {
        val result = FormValidator.validatePassword("123")
        assertFalse(result.isValid)
        assertEquals(ValidationErrorType.SHORT_PASSWORD, result.error)
    }

    // --- Strict Password Tests ---
    @Test
    fun `weak password fails strict validation`() {
        // Needs Uppercase, Number, Special char
        val result = FormValidator.validateStrictPassword("password123") 
        assertFalse(result.isValid)
        assertEquals(ValidationErrorType.WEAK_PASSWORD, result.error)
    }

    @Test
    fun `strong password passes strict validation`() {
        val result = FormValidator.validateStrictPassword("Password123!")
        assertTrue(result.isValid)
    }

    // --- Password Confirmation Tests ---
    @Test
    fun `password confirmation mismatch fails`() {
        val result = FormValidator.validatePasswordConfirmation("Password123!", "Password123")
        assertFalse(result.isValid)
        assertEquals(ValidationErrorType.PASSWORDS_DO_NOT_MATCH, result.error)
    }

    @Test
    fun `password confirmation match passes`() {
        val result = FormValidator.validatePasswordConfirmation("Password123!", "Password123!")
        assertTrue(result.isValid)
    }
    
    @Test
    fun `empty confirm password fails`() {
         val result = FormValidator.validatePasswordConfirmation("Password123!", "")
         assertFalse(result.isValid)
         assertEquals(ValidationErrorType.EMPTY_PASSWORD, result.error)
    }

    // --- Phone Tests ---
    @Test
    fun `empty phone fails`() {
        val result = FormValidator.validatePhone("")
        assertFalse(result.isValid)
        assertEquals(ValidationErrorType.EMPTY_PHONE, result.error)
    }

    @Test
    fun `invalid phone format fails`() {
        val result = FormValidator.validatePhone("abc")
        assertFalse(result.isValid)
        assertEquals(ValidationErrorType.INVALID_PHONE_FORMAT, result.error)
    }

    @Test
    fun `valid phone passes`() {
        val result = FormValidator.validatePhone("+385912345678")
        assertTrue(result.isValid)
    }

    // --- Address Tests ---
    @Test
    fun `empty street fails`() {
        val result = FormValidator.validateStreet("")
        assertFalse(result.isValid)
        assertEquals(ValidationErrorType.EMPTY_STREET, result.error)
    }

    @Test
    fun `invalid street format fails`() {
        val result = FormValidator.validateStreet("Main Street") // Missing number
        assertFalse(result.isValid)
        assertEquals(ValidationErrorType.INVALID_STREET_FORMAT, result.error)
    }
    
    @Test
    fun `valid street passes`() {
        val result = FormValidator.validateStreet("Main Street 12")
        assertTrue(result.isValid)
    }

    @Test
    fun `empty city fails`() {
        val result = FormValidator.validateCity("")
        assertFalse(result.isValid)
        assertEquals(ValidationErrorType.EMPTY_CITY, result.error)
    }
    
    @Test
    fun `valid city passes`() {
        val result = FormValidator.validateCity("New York")
        assertTrue(result.isValid)
    }

    @Test
    fun `empty zip fails`() {
        val result = FormValidator.validateZip("")
        assertFalse(result.isValid)
        assertEquals(ValidationErrorType.EMPTY_ZIP, result.error)
    }

    @Test
    fun `invalid zip format fails`() {
        val result = FormValidator.validateZip("123")
        assertFalse(result.isValid)
        assertEquals(ValidationErrorType.INVALID_ZIP_FORMAT, result.error)
    }
    
    @Test
    fun `valid zip passes`() {
        val result = FormValidator.validateZip("10000")
        assertTrue(result.isValid)
    }

    // --- Aggregate Forms Tests ---

    @Test
    fun `valid signup form passes aggregate validation`() {
        val result =
            FormValidator.validateSignupForm(
                email = "user@example.com",
                name = "Test User",
                password = "secret123",
            )

        assertTrue(result.isValid)
        assertTrue(result.email.isValid)
        assertTrue(result.name.isValid)
        assertTrue(result.password.isValid)
    }

    @Test
    fun `valid profile edit form passes`() {
        val result = FormValidator.validateProfileEditForm(
            name = "John Doe",
            phone = "+1234567890",
            street = "Main St 1",
            city = "City",
            zip = "12345"
        )
        assertTrue(result.isValid)
    }

    @Test
    fun `invalid profile edit form fails`() {
        val result = FormValidator.validateProfileEditForm(
            name = "",
            phone = "invalid",
            street = "",
            city = "",
            zip = "1"
        )
        assertFalse(result.isValid)
        assertEquals(ValidationErrorType.EMPTY_NAME, result.name.error)
        assertEquals(ValidationErrorType.INVALID_PHONE_FORMAT, result.phone.error)
    }

    @Test
    fun `valid change password form passes`() {
        val result = FormValidator.validateChangePasswordForm(
            oldPass = "OldPass1!",
            newPass = "NewPass1!",
            confirmPass = "NewPass1!"
        )
        assertTrue(result.isValid)
    }
    
    @Test
    fun `change password form fails on mismatch`() {
         val result = FormValidator.validateChangePasswordForm(
            oldPass = "OldPass1!",
            newPass = "NewPass1!",
            confirmPass = "Different1!"
        )
        assertFalse(result.isValid)
        assertEquals(ValidationErrorType.PASSWORDS_DO_NOT_MATCH, result.confirmPassword.error)
    }
}