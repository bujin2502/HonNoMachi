package hr.foi.air.image_uploader

import android.net.Uri
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import hr.foi.air.image_uploader.model.Result
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FirebaseImageUploaderTest {

    private lateinit var storage: FirebaseStorage
    private lateinit var storageRef: StorageReference
    private lateinit var childRef: StorageReference
    private lateinit var uploader: FirebaseImageUploader

    @Before
    fun setup() {
        storage = mockk(relaxed = true)
        storageRef = mockk(relaxed = true)
        childRef = mockk(relaxed = true)

        every { storage.reference } returns storageRef
        every { storageRef.child(any()) } returns childRef

        uploader = FirebaseImageUploader(storage)

        mockkStatic("kotlinx.coroutines.tasks.TasksKt")
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `uploadImage should return Success with download URL on successful upload`() = runTest {
        val uri = mockk<Uri>()
        val downloadUri = mockk<Uri>()
        val uploadTask = mockk<UploadTask>()
        val downloadUrlTask = mockk<Task<Uri>>()

        every { childRef.putFile(uri) } returns uploadTask
        every { childRef.downloadUrl } returns downloadUrlTask
        every { downloadUri.toString() } returns "https://firebase.com/test-image.jpg"

        every { uploadTask.isComplete } returns true
        every { uploadTask.isCanceled } returns false
        every { uploadTask.exception } returns null
        every { uploadTask.result } returns mockk()

        every { downloadUrlTask.isComplete } returns true
        every { downloadUrlTask.isCanceled } returns false
        every { downloadUrlTask.exception } returns null
        every { downloadUrlTask.result } returns downloadUri

        val result = uploader.uploadImage(uri, "test-path")

        assertTrue(result is Result.Success)
        assertEquals("https://firebase.com/test-image.jpg", (result as Result.Success).data)
    }

    @Test
    fun `uploadImage should return Error when upload fails`() = runTest {
        val uri = mockk<Uri>()
        val uploadTask = mockk<UploadTask>()
        val exception = RuntimeException("Upload failed")

        every { childRef.putFile(uri) } returns uploadTask
        every { uploadTask.isComplete } returns true
        every { uploadTask.isCanceled } returns false
        every { uploadTask.exception } returns exception
        every { uploadTask.result } throws exception

        val result = uploader.uploadImage(uri, "test-path")

        assertTrue(result is Result.Error)
        assertEquals("Upload failed", (result as Result.Error).exception.message)
    }

    @Test
    fun `deleteImage should return Success on successful delete`() = runTest {
        val imageUrl = "https://firebase.com/test-image.jpg"
        val imageRef = mockk<StorageReference>()
        val deleteTask = mockk<Task<Void>>()

        every { storage.getReferenceFromUrl(imageUrl) } returns imageRef
        every { imageRef.delete() } returns deleteTask
        every { deleteTask.isComplete } returns true
        every { deleteTask.isCanceled } returns false
        every { deleteTask.exception } returns null
        every { deleteTask.result } returns null

        val result = uploader.deleteImage(imageUrl)

        assertTrue(result is Result.Success)
    }

    @Test
    fun `deleteImage should return Error when delete fails`() = runTest {
        val imageUrl = "https://firebase.com/test-image.jpg"
        val imageRef = mockk<StorageReference>()
        val deleteTask = mockk<Task<Void>>()
        val exception = RuntimeException("Delete failed")

        every { storage.getReferenceFromUrl(imageUrl) } returns imageRef
        every { imageRef.delete() } returns deleteTask
        every { deleteTask.isComplete } returns true
        every { deleteTask.isCanceled } returns false
        every { deleteTask.exception } returns exception
        every { deleteTask.result } throws exception

        val result = uploader.deleteImage(imageUrl)

        assertTrue(result is Result.Error)
        assertEquals("Delete failed", (result as Result.Error).exception.message)
    }

    @Test
    fun `uploadImages with empty list should return Success with empty list`() = runTest {
        val result = uploader.uploadImages(emptyList(), "test-path")

        assertTrue(result is Result.Success)
        assertTrue((result as Result.Success).data.isEmpty())
    }

    @Test
    fun `uploadImage creates correct storage path`() = runTest {
        val uri = mockk<Uri>()
        val uploadTask = mockk<UploadTask>()
        val downloadUrlTask = mockk<Task<Uri>>()
        val downloadUri = mockk<Uri>()

        every { childRef.putFile(uri) } returns uploadTask
        every { childRef.downloadUrl } returns downloadUrlTask
        every { downloadUri.toString() } returns "https://firebase.com/image.jpg"

        every { uploadTask.isComplete } returns true
        every { uploadTask.isCanceled } returns false
        every { uploadTask.exception } returns null
        every { uploadTask.result } returns mockk()

        every { downloadUrlTask.isComplete } returns true
        every { downloadUrlTask.isCanceled } returns false
        every { downloadUrlTask.exception } returns null
        every { downloadUrlTask.result } returns downloadUri

        uploader.uploadImage(uri, "users/profile")

        verify { storageRef.child(match { it.startsWith("users/profile/") }) }
    }
}
