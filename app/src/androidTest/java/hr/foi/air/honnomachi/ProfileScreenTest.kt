package hr.foi.air.honnomachi

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import hr.foi.air.honnomachi.data.ProfileRepository
import hr.foi.air.honnomachi.model.UserModel
import hr.foi.air.honnomachi.ui.profile.ProfileScreen
import hr.foi.air.honnomachi.ui.profile.ProfileUiState
import hr.foi.air.honnomachi.ui.profile.ProfileViewModel
import hr.foi.air.honnomachi.util.Result
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

class FakeProfileViewModel(
    repository: ProfileRepository,
) : ProfileViewModel(repository) {
    fun setState(state: ProfileUiState) {
        val field = ProfileViewModel::class.java.getDeclaredField("_uiState")
        field.isAccessible = true
        val stateFlow = field.get(this) as MutableStateFlow<ProfileUiState>
        stateFlow.value = state

        if (state is ProfileUiState.Success) { // Popuni stanje forme s podacima korisnika...
            onNameChange(state.user.name)
            onPhoneChange(state.user.phoneNumber ?: "")
            onStreetChange(state.user.street ?: "")
            onCityChange(state.user.city ?: "")
            onZipChange(state.user.postNumber ?: "")
        }
    }

    /**
     * Prazna funkcija kojom spriječavaš stvarne Firebase pozive...
     *
     * Nadjačava stvarni poziv kako bi se spriječilo dohvaćanje podataka s interneta.
     */
    override fun loadUserProfile() {
        // No-op
    }

    override fun saveProfile(onResult: (Boolean, String?) -> Unit) {
        if (formState.value.name.isBlank()) {
            validateName() // ovo ažurira stanje greške (error state)...
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
    private lateinit var mockRepository: ProfileRepository

    private val defaultUser =
        UserModel(
            name = "Initial Name",
            email = "test@example.com",
            uid = "123",
            phoneNumber = "0912345678",
            street = "Street 1",
            city = "City",
            postNumber = "10000",
            suspended = false,
            admin = false,
            analyticsEnabled = true,
            notificationsEnabled = true,
        )

    private fun launchScreen(
        user: UserModel = defaultUser,
        uiState: ProfileUiState = ProfileUiState.Success(user),
        onLogout: () -> Unit = {},
        onNavigateToChangePassword: () -> Unit = {},
        onNavigateToPrivacyPolicy: () -> Unit = {},
        onNavigateToAdmin: () -> Unit = {},
    ) {
        mockRepository = mockk(relaxed = true)
        coEvery { mockRepository.getUserProfile() } returns Result.Success(user)

        fakeViewModel = FakeProfileViewModel(mockRepository)
        fakeViewModel.setState(uiState)

        composeTestRule.setContent {
            ProfileScreen(
                paddingValues = PaddingValues(0.dp),
                onLogout = onLogout,
                onNavigateToChangePassword = onNavigateToChangePassword,
                onNavigateToPrivacyPolicy = onNavigateToPrivacyPolicy,
                onNavigateToAdmin = onNavigateToAdmin,
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

        composeTestRule.onNodeWithText("Initial Name").performTextClearance()

        composeTestRule
            .onNodeWithText(
                labelName,
            ).performTextInput("Updated Name")

        // Klikni gumb "Spremi"
        composeTestRule
            .onNodeWithText(btnSave)
            .performScrollTo()
            .performClick()
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

        // Obriši Ime (simulacija neispravnog unosa)
        composeTestRule
            .onNodeWithText("Initial Name")
            .performScrollTo()
            .performTextClearance()

        // Klikni gumb "Spremi"
        composeTestRule
            .onNodeWithText(btnSave)
            .performScrollTo()
            .performClick()

        // Potvrdi da je prikazana poruka greške
        composeTestRule.onNodeWithText(errorMsg).assertIsDisplayed()
    }

    // region Prikaz podataka korisnika

    @Test
    fun displays_user_name() {
        launchScreen()

        composeTestRule.onNodeWithText("Initial Name").assertIsDisplayed()
    }

    @Test
    fun displays_user_email() {
        launchScreen()

        composeTestRule.onNodeWithText("test@example.com").assertIsDisplayed()
    }

    @Test
    fun displays_active_status_for_active_user() {
        val context =
            androidx.test.platform.app.InstrumentationRegistry
                .getInstrumentation()
                .targetContext
        launchScreen(user = defaultUser.copy(suspended = false))

        composeTestRule
            .onNodeWithText(context.getString(R.string.value_active))
            .assertIsDisplayed()
    }

    @Test
    fun displays_suspended_status_for_suspended_user() {
        val context =
            androidx.test.platform.app.InstrumentationRegistry
                .getInstrumentation()
                .targetContext
        launchScreen(user = defaultUser.copy(suspended = true))

        composeTestRule
            .onNodeWithText(context.getString(R.string.value_suspended))
            .assertIsDisplayed()
    }

    @Test
    fun error_state_shows_error_message() {
        launchScreen(uiState = ProfileUiState.Error("Firebase nedostupan"))

        composeTestRule.onNodeWithText("Firebase nedostupan").assertIsDisplayed()
    }

    // endregion

    // region Logout gumb

    @Test
    fun logout_button_is_displayed() {
        val context =
            androidx.test.platform.app.InstrumentationRegistry
                .getInstrumentation()
                .targetContext
        launchScreen()

        composeTestRule
            .onNodeWithText(context.getString(R.string.label_logout))
            .assertIsDisplayed()
    }

    @Test
    fun logout_button_calls_onLogout() {
        val context =
            androidx.test.platform.app.InstrumentationRegistry
                .getInstrumentation()
                .targetContext
        var loggedOut = false
        launchScreen(onLogout = { loggedOut = true })

        composeTestRule
            .onNodeWithText(context.getString(R.string.label_logout))
            .performClick()
        composeTestRule.waitForIdle()

        assert(loggedOut)
    }

    // endregion

    // region Navigacijski gumbi

    @Test
    fun change_password_button_calls_onNavigateToChangePassword() {
        val context =
            androidx.test.platform.app.InstrumentationRegistry
                .getInstrumentation()
                .targetContext
        var navigated = false
        launchScreen(onNavigateToChangePassword = { navigated = true })

        composeTestRule
            .onNodeWithText(context.getString(R.string.button_reset_password))
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()

        assert(navigated)
    }

    @Test
    fun privacy_policy_button_calls_onNavigateToPrivacyPolicy() {
        val context =
            androidx.test.platform.app.InstrumentationRegistry
                .getInstrumentation()
                .targetContext
        var navigated = false
        launchScreen(onNavigateToPrivacyPolicy = { navigated = true })

        composeTestRule
            .onNodeWithText(context.getString(R.string.title_privacy_policy))
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()

        assert(navigated)
    }

    // endregion

    // region Admin panel gumb

    @Test
    fun admin_panel_button_shown_for_admin_user() {
        val context =
            androidx.test.platform.app.InstrumentationRegistry
                .getInstrumentation()
                .targetContext
        launchScreen(user = defaultUser.copy(admin = true))

        composeTestRule
            .onNodeWithText(context.getString(R.string.button_admin_panel))
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun admin_panel_button_not_shown_for_regular_user() {
        val context =
            androidx.test.platform.app.InstrumentationRegistry
                .getInstrumentation()
                .targetContext
        launchScreen(user = defaultUser.copy(admin = false))

        composeTestRule
            .onNodeWithText(context.getString(R.string.button_admin_panel))
            .assertDoesNotExist()
    }

    @Test
    fun admin_panel_button_calls_onNavigateToAdmin() {
        val context =
            androidx.test.platform.app.InstrumentationRegistry
                .getInstrumentation()
                .targetContext
        var navigated = false
        launchScreen(user = defaultUser.copy(admin = true), onNavigateToAdmin = { navigated = true })

        composeTestRule
            .onNodeWithText(context.getString(R.string.button_admin_panel))
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()

        assert(navigated)
    }

    // endregion

    // region Privacy settings

    @Test
    fun privacy_settings_section_is_displayed() {
        val context =
            androidx.test.platform.app.InstrumentationRegistry
                .getInstrumentation()
                .targetContext
        launchScreen()

        composeTestRule
            .onNodeWithText(context.getString(R.string.label_privacy_settings))
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(context.getString(R.string.label_analytics_consent))
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(context.getString(R.string.label_notifications_consent))
            .performScrollTo()
            .assertIsDisplayed()
    }

    // endregion

    // region Save gumb

    @Test
    fun save_button_disabled_when_no_changes() {
        val context =
            androidx.test.platform.app.InstrumentationRegistry
                .getInstrumentation()
                .targetContext
        launchScreen()

        composeTestRule
            .onNodeWithText(context.getString(R.string.button_save))
            .performScrollTo()
            .assertIsNotEnabled()
    }

    @Test
    fun save_button_enabled_after_name_changed() {
        val context =
            androidx.test.platform.app.InstrumentationRegistry
                .getInstrumentation()
                .targetContext
        launchScreen()

        composeTestRule.onNodeWithText("Initial Name").performTextClearance()
        composeTestRule
            .onNodeWithText(context.getString(R.string.label_name))
            .performTextInput("Novo Ime")

        composeTestRule
            .onNodeWithText(context.getString(R.string.button_save))
            .performScrollTo()
            .assertIsEnabled()
    }

    // endregion
}
