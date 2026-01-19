package hr.foi.air.image_uploader.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ResultTest {

    @Test
    fun `Success should contain data`() {
        val data = "test data"
        val result: Result<String> = Result.Success(data)

        assertTrue(result is Result.Success)
        assertEquals(data, (result as Result.Success).data)
    }

    @Test
    fun `Error should contain exception`() {
        val exception = RuntimeException("Test error")
        val result: Result<String> = Result.Error(exception)

        assertTrue(result is Result.Error)
        assertEquals(exception, (result as Result.Error).exception)
        assertEquals("Test error", result.exception.message)
    }

    @Test
    fun `Success with different types should work correctly`() {
        val intResult: Result<Int> = Result.Success(42)
        val listResult: Result<List<String>> = Result.Success(listOf("a", "b", "c"))

        assertEquals(42, (intResult as Result.Success).data)
        assertEquals(3, (listResult as Result.Success).data.size)
    }

    @Test
    fun `when expression should match Success`() {
        val result: Result<String> = Result.Success("data")

        val output = when (result) {
            is Result.Success -> "success: ${result.data}"
            is Result.Error -> "error: ${result.exception.message}"
        }

        assertEquals("success: data", output)
    }

    @Test
    fun `when expression should match Error`() {
        val result: Result<String> = Result.Error(IllegalArgumentException("invalid"))

        val output = when (result) {
            is Result.Success -> "success: ${result.data}"
            is Result.Error -> "error: ${result.exception.message}"
        }

        assertEquals("error: invalid", output)
    }

    @Test
    fun `Success with Unit should work for delete operations`() {
        val result: Result<Unit> = Result.Success(Unit)

        assertTrue(result is Result.Success)
        assertEquals(Unit, (result as Result.Success).data)
    }

    @Test
    fun `Error preserves exception type`() {
        val ioException = java.io.IOException("Network error")
        val result: Result<String> = Result.Error(ioException)

        assertTrue((result as Result.Error).exception is java.io.IOException)
    }
}
