package hr.foi.air.image_uploader.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.test.ext.junit.runners.AndroidJUnit4
import hr.foi.air.image_uploader.R
import hr.foi.air.image_uploader.sources.CameraImageSource
import hr.foi.air.image_uploader.sources.GalleryImageSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ImageSourceTest {

    @Test
    fun cameraImageSource_hasCorrectId() {
        val source = CameraImageSource()
        assertEquals("camera", source.id)
    }

    @Test
    fun cameraImageSource_hasCorrectNameResId() {
        val source = CameraImageSource()
        assertEquals(R.string.camera, source.nameResId)
    }

    @Test
    fun cameraImageSource_hasCorrectIcon() {
        val source = CameraImageSource()
        assertEquals(Icons.Default.CameraAlt, source.icon)
    }

    @Test
    fun galleryImageSource_hasCorrectId() {
        val source = GalleryImageSource()
        assertEquals("gallery", source.id)
    }

    @Test
    fun galleryImageSource_hasCorrectNameResId() {
        val source = GalleryImageSource()
        assertEquals(R.string.gallery, source.nameResId)
    }

    @Test
    fun galleryImageSource_hasCorrectIcon() {
        val source = GalleryImageSource()
        assertEquals(Icons.Default.PhotoLibrary, source.icon)
    }

    @Test
    fun cameraImageSource_implementsImageSource() {
        val source = CameraImageSource()
        assertTrue(source is ImageSource)
    }

    @Test
    fun galleryImageSource_implementsImageSource() {
        val source = GalleryImageSource()
        assertTrue(source is ImageSource)
    }

    @Test
    fun differentSources_haveDifferentIds() {
        val cameraSource = CameraImageSource()
        val gallerySource = GalleryImageSource()
        assertNotEquals(cameraSource.id, gallerySource.id)
    }

    @Test
    fun differentSources_haveDifferentIcons() {
        val cameraSource = CameraImageSource()
        val gallerySource = GalleryImageSource()
        assertNotEquals(cameraSource.icon, gallerySource.icon)
    }

    @Test
    fun differentSources_haveDifferentNameResIds() {
        val cameraSource = CameraImageSource()
        val gallerySource = GalleryImageSource()
        assertNotEquals(cameraSource.nameResId, gallerySource.nameResId)
    }

    @Test
    fun cameraImageSource_multipleInstances_haveSameProperties() {
        val source1 = CameraImageSource()
        val source2 = CameraImageSource()

        assertEquals(source1.id, source2.id)
        assertEquals(source1.nameResId, source2.nameResId)
        assertEquals(source1.icon, source2.icon)
    }

    @Test
    fun galleryImageSource_multipleInstances_haveSameProperties() {
        val source1 = GalleryImageSource()
        val source2 = GalleryImageSource()

        assertEquals(source1.id, source2.id)
        assertEquals(source1.nameResId, source2.nameResId)
        assertEquals(source1.icon, source2.icon)
    }
}
