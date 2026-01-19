package hr.foi.air.image_uploader.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.test.ext.junit.runners.AndroidJUnit4
import hr.foi.air.image_uploader.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ImageSourceTest {

    @Test
    fun imageSource_createsWithCorrectValues() {
        val source = ImageSource(
            id = "camera",
            nameResId = R.string.camera,
            icon = Icons.Default.CameraAlt
        )

        assertEquals("camera", source.id)
        assertEquals(R.string.camera, source.nameResId)
        assertEquals(Icons.Default.CameraAlt, source.icon)
    }

    @Test
    fun imageSource_equalityWorks() {
        val source1 = ImageSource(
            id = "gallery",
            nameResId = R.string.gallery,
            icon = Icons.Default.PhotoLibrary
        )
        val source2 = ImageSource(
            id = "gallery",
            nameResId = R.string.gallery,
            icon = Icons.Default.PhotoLibrary
        )

        assertEquals(source1, source2)
    }

    @Test
    fun imageSource_differentIds_notEqual() {
        val source1 = ImageSource(
            id = "camera",
            nameResId = R.string.camera,
            icon = Icons.Default.CameraAlt
        )
        val source2 = ImageSource(
            id = "gallery",
            nameResId = R.string.camera,
            icon = Icons.Default.CameraAlt
        )

        assertNotEquals(source1, source2)
    }

    @Test
    fun imageSource_copyWorks() {
        val original = ImageSource(
            id = "camera",
            nameResId = R.string.camera,
            icon = Icons.Default.CameraAlt
        )
        val copied = original.copy(id = "camera_new")

        assertEquals("camera_new", copied.id)
        assertEquals(original.nameResId, copied.nameResId)
        assertEquals(original.icon, copied.icon)
    }

    @Test
    fun imageSource_hashCodeConsistent() {
        val source1 = ImageSource(
            id = "camera",
            nameResId = R.string.camera,
            icon = Icons.Default.CameraAlt
        )
        val source2 = ImageSource(
            id = "camera",
            nameResId = R.string.camera,
            icon = Icons.Default.CameraAlt
        )

        assertEquals(source1.hashCode(), source2.hashCode())
    }

    @Test
    fun imageSource_destructuringWorks() {
        val source = ImageSource(
            id = "gallery",
            nameResId = R.string.gallery,
            icon = Icons.Default.PhotoLibrary
        )

        val (id, nameResId, icon) = source

        assertEquals("gallery", id)
        assertEquals(R.string.gallery, nameResId)
        assertEquals(Icons.Default.PhotoLibrary, icon)
    }

    @Test
    fun imageSource_toStringContainsValues() {
        val source = ImageSource(
            id = "camera",
            nameResId = R.string.camera,
            icon = Icons.Default.CameraAlt
        )

        val stringRepresentation = source.toString()

        assert(stringRepresentation.contains("camera"))
        assert(stringRepresentation.contains("ImageSource"))
    }

    @Test
    fun imageSource_withDifferentIcons() {
        val cameraSource = ImageSource(
            id = "camera",
            nameResId = R.string.camera,
            icon = Icons.Default.CameraAlt
        )
        val gallerySource = ImageSource(
            id = "gallery",
            nameResId = R.string.gallery,
            icon = Icons.Default.PhotoLibrary
        )

        assertNotEquals(cameraSource.icon, gallerySource.icon)
    }
}
