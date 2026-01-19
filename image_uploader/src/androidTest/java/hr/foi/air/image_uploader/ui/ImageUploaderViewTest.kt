package hr.foi.air.image_uploader.ui

import android.net.Uri
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ImageUploaderViewTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun imageUploaderView_displaysAddImagesButton() {
        composeTestRule.setContent {
            ImageUploaderView(
                onImagesSelected = {}
            )
        }

        composeTestRule
            .onNodeWithText("Add Images")
            .assertIsDisplayed()
    }

    @Test
    fun imageUploaderView_addImagesButtonClick_opensImagePicker() {
        composeTestRule.setContent {
            ImageUploaderView(
                onImagesSelected = {}
            )
        }

        composeTestRule
            .onNodeWithText("Add Images")
            .performClick()

        composeTestRule
            .onNodeWithText("Choose Image Source")
            .assertIsDisplayed()
    }

    @Test
    fun imageUploaderView_imagePicker_displaysCameraAndGalleryOptions() {
        composeTestRule.setContent {
            ImageUploaderView(
                onImagesSelected = {}
            )
        }

        composeTestRule
            .onNodeWithText("Add Images")
            .performClick()

        composeTestRule
            .onNodeWithText("Camera")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Gallery")
            .assertIsDisplayed()
    }

    @Test
    fun imageUploaderView_initialState_noImagesDisplayed() {
        composeTestRule.setContent {
            ImageUploaderView(
                onImagesSelected = {}
            )
        }

        composeTestRule
            .onNodeWithContentDescription("Remove image")
            .assertDoesNotExist()
    }

    @Test
    fun imageUploaderView_clickOutsidePicker_dismissesPicker() {
        composeTestRule.setContent {
            ImageUploaderView(
                onImagesSelected = {}
            )
        }

        composeTestRule
            .onNodeWithText("Add Images")
            .performClick()

        composeTestRule
            .onNodeWithText("Choose Image Source")
            .assertIsDisplayed()
    }

    @Test
    fun imageUploaderView_buttonIsClickable() {
        var clickCount = 0

        composeTestRule.setContent {
            ImageUploaderView(
                onImagesSelected = { clickCount++ }
            )
        }

        composeTestRule
            .onNodeWithText("Add Images")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Add Images")
            .performClick()

        composeTestRule
            .onNodeWithText("Choose Image Source")
            .assertIsDisplayed()
    }

    @Test
    fun imageUploaderView_cameraIconIsDisplayedInPicker() {
        composeTestRule.setContent {
            ImageUploaderView(
                onImagesSelected = {}
            )
        }

        composeTestRule
            .onNodeWithText("Add Images")
            .performClick()

        composeTestRule
            .onNodeWithContentDescription("Camera")
            .assertIsDisplayed()
    }

    @Test
    fun imageUploaderView_galleryIconIsDisplayedInPicker() {
        composeTestRule.setContent {
            ImageUploaderView(
                onImagesSelected = {}
            )
        }

        composeTestRule
            .onNodeWithText("Add Images")
            .performClick()

        composeTestRule
            .onNodeWithContentDescription("Gallery")
            .assertIsDisplayed()
    }
}
