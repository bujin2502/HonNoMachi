package hr.foi.air.image_uploader.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ImageUploadResultTest {

    @Test
    fun `Idle should be singleton`() {
        val idle1 = ImageUploadResult.Idle
        val idle2 = ImageUploadResult.Idle

        assertTrue(idle1 === idle2)
    }

    @Test
    fun `Loading should be singleton`() {
        val loading1 = ImageUploadResult.Loading
        val loading2 = ImageUploadResult.Loading

        assertTrue(loading1 === loading2)
    }

    @Test
    fun `Success should contain data`() {
        val urls = listOf("url1", "url2")
        val result = ImageUploadResult.Success(urls)

        assertEquals(urls, result.data)
    }

    @Test
    fun `Error should contain message`() {
        val errorMessage = "Upload failed"
        val result = ImageUploadResult.Error(errorMessage)

        assertEquals(errorMessage, result.message)
    }

    @Test
    fun `when expression should match all states`() {
        val states: List<ImageUploadResult<String>> = listOf(
            ImageUploadResult.Idle,
            ImageUploadResult.Loading,
            ImageUploadResult.Success("data"),
            ImageUploadResult.Error("error")
        )

        val results = states.map { state ->
            when (state) {
                is ImageUploadResult.Idle -> "idle"
                is ImageUploadResult.Loading -> "loading"
                is ImageUploadResult.Success -> "success: ${state.data}"
                is ImageUploadResult.Error -> "error: ${state.message}"
            }
        }

        assertEquals(listOf("idle", "loading", "success: data", "error: error"), results)
    }

    @Test
    fun `Success with empty list should work`() {
        val result = ImageUploadResult.Success(emptyList<String>())

        assertTrue(result.data.isEmpty())
    }

    @Test
    fun `state transitions should be valid`() {
        var state: ImageUploadResult<String> = ImageUploadResult.Idle
        assertEquals(ImageUploadResult.Idle, state)

        state = ImageUploadResult.Loading
        assertEquals(ImageUploadResult.Loading, state)

        state = ImageUploadResult.Success("uploaded")
        assertEquals("uploaded", (state as ImageUploadResult.Success).data)

        state = ImageUploadResult.Idle
        assertEquals(ImageUploadResult.Idle, state)

        state = ImageUploadResult.Loading
        assertEquals(ImageUploadResult.Loading, state)

        state = ImageUploadResult.Error("failed")
        assertEquals("failed", (state as ImageUploadResult.Error).message)
    }

    @Test
    fun `Error with empty message should work`() {
        val result = ImageUploadResult.Error("")

        assertEquals("", result.message)
    }
}
