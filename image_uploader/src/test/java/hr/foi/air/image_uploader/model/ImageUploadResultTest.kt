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
        val result: ImageUploadResult<List<String>> = ImageUploadResult.Success(urls)

        assertTrue(result is ImageUploadResult.Success)
        assertEquals(urls, (result as ImageUploadResult.Success).data)
    }

    @Test
    fun `Error should contain message`() {
        val errorMessage = "Upload failed"
        val result: ImageUploadResult<String> = ImageUploadResult.Error(errorMessage)

        assertTrue(result is ImageUploadResult.Error)
        assertEquals(errorMessage, (result as ImageUploadResult.Error).message)
    }

    @Test
    fun `when expression should match all states`() {
        val states = listOf(
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
        val result: ImageUploadResult<List<String>> = ImageUploadResult.Success(emptyList())

        assertTrue(result is ImageUploadResult.Success)
        assertTrue((result as ImageUploadResult.Success).data.isEmpty())
    }

    @Test
    fun `state transitions should be valid`() {
        var state: ImageUploadResult<String> = ImageUploadResult.Idle
        assertTrue(state is ImageUploadResult.Idle)

        state = ImageUploadResult.Loading
        assertTrue(state is ImageUploadResult.Loading)

        state = ImageUploadResult.Success("uploaded")
        assertTrue(state is ImageUploadResult.Success)

        state = ImageUploadResult.Idle
        assertTrue(state is ImageUploadResult.Idle)

        state = ImageUploadResult.Loading
        assertTrue(state is ImageUploadResult.Loading)

        state = ImageUploadResult.Error("failed")
        assertTrue(state is ImageUploadResult.Error)
    }

    @Test
    fun `Error with empty message should work`() {
        val result: ImageUploadResult<String> = ImageUploadResult.Error("")

        assertTrue(result is ImageUploadResult.Error)
        assertEquals("", (result as ImageUploadResult.Error).message)
    }
}
