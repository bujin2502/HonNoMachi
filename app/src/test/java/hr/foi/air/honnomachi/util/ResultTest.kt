package hr.foi.air.honnomachi.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ResultTest {
    @Test
    fun `Succeeded returns true for Success and false for Error`() {
        val success: Result<String> = Result.Success("data")
        val error: Result<String> = Result.Error(RuntimeException("error"))

        assertTrue(success.Succeeded)
        assertFalse(error.Succeeded)
    }

    @Test
    fun `Failed returns true for Error and false for Success`() {
        val success: Result<String> = Result.Success("data")
        val error: Result<String> = Result.Error(RuntimeException("error"))

        assertTrue(error.Failed)
        assertFalse(success.Failed)
    }

    @Test
    fun `getOrNull returns data for Success and null for Error`() {
        val success: Result<String> = Result.Success("data")
        val error: Result<String> = Result.Error(RuntimeException("error"))

        assertEquals("data", success.getOrNull())
        assertNull(error.getOrNull())
    }
}
