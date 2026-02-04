package hr.foi.air.image_uploader.model

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class ImageSourceRegistryTest {

    @Before
    fun setup() {
        ImageSourceRegistry.clear()
    }

    private fun createMockImageSource(id: String): ImageSource {
        return object : ImageSource {
            override val id: String = id
            override val nameResId: Int = 0
            override val icon: ImageVector = mockk()

            @Composable
            override fun rememberLauncher(onImageSelected: (List<Uri>) -> Unit): () -> Unit {
                return {}
            }
        }
    }

    @Test
    fun `register adds source to registry`() {
        val source = createMockImageSource("test")
        ImageSourceRegistry.register(source)

        assertEquals(1, ImageSourceRegistry.getAllSources().size)
    }

    @Test
    fun `register ignores duplicate ids`() {
        val source1 = createMockImageSource("test")
        val source2 = createMockImageSource("test")

        ImageSourceRegistry.register(source1)
        ImageSourceRegistry.register(source2)

        assertEquals(1, ImageSourceRegistry.getAllSources().size)
    }

    @Test
    fun `register allows different ids`() {
        val source1 = createMockImageSource("camera")
        val source2 = createMockImageSource("gallery")

        ImageSourceRegistry.register(source1)
        ImageSourceRegistry.register(source2)

        assertEquals(2, ImageSourceRegistry.getAllSources().size)
    }

    @Test
    fun `getSource returns correct source`() {
        val source = createMockImageSource("camera")
        ImageSourceRegistry.register(source)

        assertEquals(source, ImageSourceRegistry.getSource("camera"))
    }

    @Test
    fun `getSource returns null for unknown id`() {
        val source = createMockImageSource("camera")
        ImageSourceRegistry.register(source)

        assertNull(ImageSourceRegistry.getSource("unknown"))
    }

    @Test
    fun `getAllSources returns empty list when no sources registered`() {
        assertEquals(0, ImageSourceRegistry.getAllSources().size)
    }

    @Test
    fun `getAllSources returns copy of list`() {
        val source = createMockImageSource("test")
        ImageSourceRegistry.register(source)

        val sources1 = ImageSourceRegistry.getAllSources()
        val sources2 = ImageSourceRegistry.getAllSources()

        assertEquals(sources1, sources2)
    }

    @Test
    fun `clear removes all sources`() {
        ImageSourceRegistry.register(createMockImageSource("camera"))
        ImageSourceRegistry.register(createMockImageSource("gallery"))

        assertEquals(2, ImageSourceRegistry.getAllSources().size)

        ImageSourceRegistry.clear()

        assertEquals(0, ImageSourceRegistry.getAllSources().size)
    }
}
