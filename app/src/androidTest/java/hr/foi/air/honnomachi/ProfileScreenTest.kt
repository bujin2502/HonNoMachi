package hr.foi.air.honnomachi

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import hr.foi.air.honnomachi.model.UserModel
import hr.foi.air.honnomachi.screen.ProfileScreen
import hr.foi.air.honnomachi.viewmodel.ProfileUiState
import hr.foi.air.honnomachi.viewmodel.ProfileViewModel
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// Create a Fake ViewModel to bypass Firebase
class FakeProfileViewModel : ProfileViewModel(mockk(relaxed = true), mockk(relaxed = true)) {
    // Helper to set private state via reflection
    fun setState(state: ProfileUiState) {
        val field = ProfileViewModel::class.java.getDeclaredField("_uiState")
        field.isAccessible = true
        val stateFlow = field.get(this) as MutableStateFlow<ProfileUiState>
        stateFlow.value = state

        if (state is ProfileUiState.Success) {
            // Also populate form state
            onNameChange(state.user.name)
            onPhoneChange(state.user.phoneNumber ?: "")
            onStreetChange(state.user.street ?: "")
            onCityChange(state.user.city ?: "")
            onZipChange(state.user.postNumber ?: "")
        }
    }

    override fun loadUserProfile() {
        // No-op to prevent Firebase calls
    }

    override fun saveProfile(onResult: (Boolean, String?) -> Unit) {
        // Simulate save logic using the open validation methods
        if (formState.value.name.isBlank()) {
            validateName() // this updates error state
            onResult(false, "Error")
            return
        }
        onResult(true, null)
    }
}

@RunWith(AndroidJUnit4::class)
class ProfileScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var fakeViewModel: FakeProfileViewModel

    private fun launchScreen() {
        fakeViewModel = FakeProfileViewModel()

        // Set initial state
        val user =
            UserModel(
                name = "Initial Name",
                email = "test@example.com",
                uid = "123",
                phoneNumber = "0912345678",
                street = "Street 1",
                city = "City",
                postNumber = "10000",
            )
        fakeViewModel.setState(ProfileUiState.Success(user))

        composeTestRule.setContent {
            ProfileScreen(
                paddingValues = PaddingValues(0.dp),
                onLogout = {},
                onNavigateToChangePassword = {},
                profileViewModel = fakeViewModel,
            )
        }
    }

    @Test
    fun successful_profile_edit() {
        launchScreen()

        composeTestRule.onNodeWithText("Initial Name").assertIsDisplayed()

        val context =
            androidx.test.platform.app.InstrumentationRegistry
                .getInstrumentation()
                .targetContext
        val labelName = context.getString(hr.foi.air.honnomachi.R.string.label_name)
        val btnSave = context.getString(hr.foi.air.honnomachi.R.string.button_save)

        // Find text field via initial value, then update it
        composeTestRule.onNodeWithText("Initial Name").performTextClearance()
        // Find by label now that value is gone, or use label from start
        composeTestRule.onNodeWithText(labelName).performTextInput("Updated Name")

        // Click Save
        composeTestRule.onNodeWithText(btnSave).performScrollTo().performClick()
    }

    @Test
    fun invalid_input_shows_error() {
        launchScreen()

        val context =
            androidx.test.platform.app.InstrumentationRegistry
                .getInstrumentation()
                .targetContext
        val btnSave = context.getString(hr.foi.air.honnomachi.R.string.button_save)
        val errorMsg = context.getString(hr.foi.air.honnomachi.R.string.error_name_required)

        // Clear Name
        composeTestRule.onNodeWithText("Initial Name").performScrollTo().performTextClearance()

        // Click Save
        composeTestRule.onNodeWithText(btnSave).performScrollTo().performClick()

        // Verify Error Message
        composeTestRule.onNodeWithText(errorMsg).assertIsDisplayed()
    }
}
