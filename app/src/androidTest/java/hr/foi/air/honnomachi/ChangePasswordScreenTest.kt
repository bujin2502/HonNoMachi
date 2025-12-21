package hr.foi.air.honnomachi

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import hr.foi.air.honnomachi.screen.ChangePasswordScreen
import hr.foi.air.honnomachi.viewmodel.ProfileViewModel
import io.mockk.mockk
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// Fake ViewModel
class FakeChangePasswordViewModel : ProfileViewModel(mockk(relaxed = true), mockk(relaxed = true)) {
    
    var changePasswordCalled = false
    var shouldSucceed = true

    override fun loadUserProfile() {}

    override fun changePassword(
        oldPass: String,
        newPass: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        changePasswordCalled = true
        if (shouldSucceed) {
            onResult(true, null)
        } else {
            onResult(false, "Error")
        }
    }
}

@RunWith(AndroidJUnit4::class)
class ChangePasswordScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var fakeViewModel: FakeChangePasswordViewModel

    private fun launchScreen() {
        fakeViewModel = FakeChangePasswordViewModel()
        composeTestRule.setContent {
            ChangePasswordScreen(
                navController = rememberNavController(),
                profileViewModel = fakeViewModel
            )
        }
    }

    @Test
    fun successful_password_change() {
        launchScreen()
        composeTestRule.waitForIdle()

        val context = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        val labelOldPass = context.getString(hr.foi.air.honnomachi.R.string.label_old_password)
        val labelNewPass = context.getString(hr.foi.air.honnomachi.R.string.label_new_password)
        val labelConfirmPass = context.getString(hr.foi.air.honnomachi.R.string.label_confirm_password)
        val btnSave = context.getString(hr.foi.air.honnomachi.R.string.button_save)

        composeTestRule.onNodeWithText(labelOldPass).performTextInput("OldPass1!")
        composeTestRule.onNodeWithText(labelNewPass).performTextInput("NewPass1!")
        composeTestRule.onNodeWithText(labelConfirmPass).performTextInput("NewPass1!")

        composeTestRule.onNodeWithText(btnSave).performClick()

        assert(fakeViewModel.changePasswordCalled)
    }

    @Test
    fun password_mismatch_shows_error() {
        launchScreen()
        composeTestRule.waitForIdle()

        val context = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        val labelOldPass = context.getString(hr.foi.air.honnomachi.R.string.label_old_password)
        val labelNewPass = context.getString(hr.foi.air.honnomachi.R.string.label_new_password)
        val labelConfirmPass = context.getString(hr.foi.air.honnomachi.R.string.label_confirm_password)
        val btnSave = context.getString(hr.foi.air.honnomachi.R.string.button_save)
        val errorMsg = context.getString(hr.foi.air.honnomachi.R.string.error_passwords_do_not_match)

        composeTestRule.onNodeWithText(labelOldPass).performTextInput("OldPass1!")
        composeTestRule.onNodeWithText(labelNewPass).performTextInput("NewPass1!")
        composeTestRule.onNodeWithText(labelConfirmPass).performTextInput("Different1!")

        composeTestRule.onNodeWithText(btnSave).performClick()

        composeTestRule.onNodeWithText(errorMsg).assertIsDisplayed()
        
        assert(!fakeViewModel.changePasswordCalled)
    }
}
