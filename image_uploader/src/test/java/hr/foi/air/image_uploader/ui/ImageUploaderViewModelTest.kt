package hr.foi.air.image_uploader.ui

import android.net.Uri
import hr.foi.air.image_uploader.ImageUploader
import hr.foi.air.image_uploader.model.ImageUploadResult
import hr.foi.air.image_uploader.model.Result
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.mockkClass
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ImageUploaderViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var imageUploader: ImageUploader
    private lateinit var viewModel: ImageUploaderViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        imageUploader = mockk()
        viewModel = ImageUploaderViewModel(imageUploader)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state should be Idle`() {
        assertEquals(ImageUploadResult.Idle, viewModel.uploadState.value)
    }

    @Test
    fun `uploadImages with empty list should return Success with empty list`() = runTest {
        viewModel.uploadImages(emptyList())

        advanceUntilIdle()

        val state = viewModel.uploadState.value
        assertTrue(state is ImageUploadResult.Success)
        assertTrue((state as ImageUploadResult.Success).data.isEmpty())
    }

    @Test
    fun `uploadImages should set Loading state before upload`() = runTest {
        val uri = mockk<Uri>()
        val uploadedUrls = listOf("https://firebase.com/image1.jpg")

        coEvery { imageUploader.uploadImages(any(), any()) } returns Result.Success(uploadedUrls)

        viewModel.uploadImages(listOf(uri))

        assertEquals(ImageUploadResult.Loading, viewModel.uploadState.value)

        advanceUntilIdle()
    }

    @Test
    fun `uploadImages should return Success on successful upload`() = runTest {
        val uri = mockk<Uri>()
        val uploadedUrls = listOf("https://firebase.com/image1.jpg", "https://firebase.com/image2.jpg")

        coEvery { imageUploader.uploadImages(any(), any()) } returns Result.Success(uploadedUrls)

        viewModel.uploadImages(listOf(uri))
        advanceUntilIdle()

        val state = viewModel.uploadState.value
        assertTrue(state is ImageUploadResult.Success)
        assertEquals(uploadedUrls, (state as ImageUploadResult.Success).data)
    }

    @Test
    fun `uploadImages should return Error on failed upload`() = runTest {
        val uri = mockk<Uri>()
        val exception = RuntimeException("Network error")

        coEvery { imageUploader.uploadImages(any(), any()) } returns Result.Error(exception)

        viewModel.uploadImages(listOf(uri))
        advanceUntilIdle()

        val state = viewModel.uploadState.value
        assertTrue(state is ImageUploadResult.Error)
        assertEquals("Network error", (state as ImageUploadResult.Error).message)
    }

    @Test
    fun `uploadImages should handle exception with null message`() = runTest {
        val uri = mockk<Uri>()
        val exception = RuntimeException()

        coEvery { imageUploader.uploadImages(any(), any()) } returns Result.Error(exception)

        viewModel.uploadImages(listOf(uri))
        advanceUntilIdle()

        val state = viewModel.uploadState.value
        assertTrue(state is ImageUploadResult.Error)
        assertEquals("Unknown upload error", (state as ImageUploadResult.Error).message)
    }

    @Test
    fun `resetState should set state to Idle`() = runTest {
        val uri = mockk<Uri>()
        val uploadedUrls = listOf("https://firebase.com/image.jpg")

        coEvery { imageUploader.uploadImages(any(), any()) } returns Result.Success(uploadedUrls)

        viewModel.uploadImages(listOf(uri))
        advanceUntilIdle()

        assertTrue(viewModel.uploadState.value is ImageUploadResult.Success)

        viewModel.resetState()

        assertEquals(ImageUploadResult.Idle, viewModel.uploadState.value)
    }

    @Test
    fun `uploadImages should call imageUploader with correct parameters`() = runTest {
        val uri1 = mockk<Uri>()
        val uri2 = mockk<Uri>()
        val uris = listOf(uri1, uri2)

        coEvery { imageUploader.uploadImages(uris, "images") } returns Result.Success(emptyList())

        viewModel.uploadImages(uris)
        advanceUntilIdle()

        coVerify(exactly = 1) { imageUploader.uploadImages(uris, "images") }
    }

    @Test
    fun `multiple uploads should update state correctly`() = runTest {
        val uri = mockk<Uri>()

        coEvery { imageUploader.uploadImages(any(), any()) } returns Result.Success(listOf("url1"))

        viewModel.uploadImages(listOf(uri))
        advanceUntilIdle()
        assertTrue(viewModel.uploadState.value is ImageUploadResult.Success)

        viewModel.resetState()
        assertEquals(ImageUploadResult.Idle, viewModel.uploadState.value)

        coEvery { imageUploader.uploadImages(any(), any()) } returns Result.Error(RuntimeException("Error"))

        viewModel.uploadImages(listOf(uri))
        advanceUntilIdle()
        assertTrue(viewModel.uploadState.value is ImageUploadResult.Error)
    }
}
