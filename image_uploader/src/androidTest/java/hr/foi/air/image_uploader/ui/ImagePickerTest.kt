package hr.foi.air.image_uploader.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import hr.foi.air.image_uploader.R
import hr.foi.air.image_uploader.model.ImageSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ImagePickerTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val testImageSources = listOf(
        ImageSource("camera", R.string.camera, Icons.Default.CameraAlt),
        ImageSource("gallery", R.string.gallery, Icons.Default.PhotoLibrary)
    )

    @Test
    fun imagePicker_displaysTitle() {
        composeTestRule.setContent {
            ImagePicker(
                onDismiss = {},
                onSourceSelected = {},
                imageSources = testImageSources
            )
        }

        composeTestRule
            .onNodeWithText("Choose Image Source")
            .assertIsDisplayed()
    }

    @Test
    fun imagePicker_displaysCameraOption() {
        composeTestRule.setContent {
            ImagePicker(
                onDismiss = {},
                onSourceSelected = {},
                imageSources = testImageSources
            )
        }

        composeTestRule
            .onNodeWithText("Camera")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithContentDescription("Camera")
            .assertIsDisplayed()
    }

    @Test
    fun imagePicker_displaysGalleryOption() {
        composeTestRule.setContent {
            ImagePicker(
                onDismiss = {},
                onSourceSelected = {},
                imageSources = testImageSources
            )
        }

        composeTestRule
            .onNodeWithText("Gallery")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithContentDescription("Gallery")
            .assertIsDisplayed()
    }

    @Test
    fun imagePicker_cameraOptionClick_callsOnSourceSelected() {
        var selectedSource: ImageSource? = null

        composeTestRule.setContent {
            ImagePicker(
                onDismiss = {},
                onSourceSelected = { selectedSource = it },
                imageSources = testImageSources
            )
        }

        composeTestRule
            .onNodeWithText("Camera")
            .performClick()

        assertEquals("camera", selectedSource?.id)
    }

    @Test
    fun imagePicker_galleryOptionClick_callsOnSourceSelected() {
        var selectedSource: ImageSource? = null

        composeTestRule.setContent {
            ImagePicker(
                onDismiss = {},
                onSourceSelected = { selectedSource = it },
                imageSources = testImageSources
            )
        }

        composeTestRule
            .onNodeWithText("Gallery")
            .performClick()

        assertEquals("gallery", selectedSource?.id)
    }

    @Test
    fun imagePicker_displaysAllProvidedSources() {
        val customSources = listOf(
            ImageSource("source1", R.string.camera, Icons.Default.CameraAlt),
            ImageSource("source2", R.string.gallery, Icons.Default.PhotoLibrary)
        )

        composeTestRule.setContent {
            ImagePicker(
                onDismiss = {},
                onSourceSelected = {},
                imageSources = customSources
            )
        }

        composeTestRule
            .onNodeWithText("Camera")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Gallery")
            .assertIsDisplayed()
    }

    @Test
    fun imagePicker_withSingleSource_displaysCorrectly() {
        val singleSource = listOf(
            ImageSource("camera", R.string.camera, Icons.Default.CameraAlt)
        )

        composeTestRule.setContent {
            ImagePicker(
                onDismiss = {},
                onSourceSelected = {},
                imageSources = singleSource
            )
        }

        composeTestRule
            .onNodeWithText("Camera")
            .assertIsDisplayed()
    }

    @Test
    fun imagePicker_iconClick_callsOnSourceSelected() {
        var selectedSource: ImageSource? = null

        composeTestRule.setContent {
            ImagePicker(
                onDismiss = {},
                onSourceSelected = { selectedSource = it },
                imageSources = testImageSources
            )
        }

        composeTestRule
            .onNodeWithContentDescription("Camera")
            .performClick()

        assertEquals("camera", selectedSource?.id)
    }
}
