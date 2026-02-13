package hr.foi.air.image_uploader

import hr.foi.air.image_uploader.model.ImageSourceRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

class ImageSourceInitializerTest {
    @Before
    fun setup() {
        ImageSourceInitializer.reset()
    }

    @Test
    fun `initialize registers camera and gallery sources`() {
        ImageSourceInitializer.initialize()

        val sources = ImageSourceRegistry.getAllSources()
        assertEquals(2, sources.size)
        assertNotNull(ImageSourceRegistry.getSource("camera"))
        assertNotNull(ImageSourceRegistry.getSource("gallery"))
    }

    @Test
    fun `camera source has correct id`() {
        ImageSourceInitializer.initialize()

        val cameraSource = ImageSourceRegistry.getSource("camera")
        assertNotNull(cameraSource)
        assertEquals("camera", cameraSource?.id)
    }

    @Test
    fun `gallery source has correct id`() {
        ImageSourceInitializer.initialize()

        val gallerySource = ImageSourceRegistry.getSource("gallery")
        assertNotNull(gallerySource)
        assertEquals("gallery", gallerySource?.id)
    }

    @Test
    fun `initialize is idempotent - calling twice does not duplicate sources`() {
        ImageSourceInitializer.initialize()
        ImageSourceInitializer.initialize()

        val sources = ImageSourceRegistry.getAllSources()
        assertEquals(2, sources.size)
    }

    @Test
    fun `reset clears sources and allows re-initialization`() {
        ImageSourceInitializer.initialize()
        assertEquals(2, ImageSourceRegistry.getAllSources().size)

        ImageSourceInitializer.reset()
        assertEquals(0, ImageSourceRegistry.getAllSources().size)

        ImageSourceInitializer.initialize()
        assertEquals(2, ImageSourceRegistry.getAllSources().size)
    }
}
