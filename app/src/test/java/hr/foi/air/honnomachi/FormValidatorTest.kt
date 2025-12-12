package hr.foi.air.honnomachi

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FormValidatorTest {
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
}
