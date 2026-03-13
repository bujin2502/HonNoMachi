package hr.foi.air.honnomachi

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import hr.foi.air.honnomachi.data.AdminRepository
import hr.foi.air.honnomachi.model.UserModel
import hr.foi.air.honnomachi.ui.admin.AdminUserDetailScreen
import hr.foi.air.honnomachi.ui.admin.AdminUserDetailUiState
import hr.foi.air.honnomachi.ui.admin.AdminUserDetailViewModel
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private val context
    get() = InstrumentationRegistry.getInstrumentation().targetContext

private fun str(id: Int) = context.getString(id)

private class FakeAdminUserDetailViewModel(
    repository: AdminRepository,
) : AdminUserDetailViewModel(
        SavedStateHandle(mapOf("userId" to "test-uid")),
        repository,
    ) {
    fun setState(state: AdminUserDetailUiState) {
        val field = AdminUserDetailViewModel::class.java.getDeclaredField("_uiState")
        field.isAccessible = true
        val stateFlow = field.get(this) as MutableStateFlow<AdminUserDetailUiState>
        stateFlow.value = state
    }

    override fun loadUser() {
        /** Prazna implementacija — sprječava Firebase pozive. */
    }
}

@RunWith(AndroidJUnit4::class)
class AdminUserDetailScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private fun launchScreen(
        state: AdminUserDetailUiState,
        onNavigateBack: () -> Unit = {},
    ): FakeAdminUserDetailViewModel {
        val fakeViewModel = FakeAdminUserDetailViewModel(mockk(relaxed = true))
        fakeViewModel.setState(state)

        composeTestRule.setContent {
            AdminUserDetailScreen(
                onNavigateBack = onNavigateBack,
                viewModel = fakeViewModel,
            )
        }
        composeTestRule.waitForIdle()
        return fakeViewModel
    }

    // region Loading state

    @Test
    fun loading_state_shows_progress_indicator() {
        launchScreen(AdminUserDetailUiState.Loading)

        composeTestRule
            .onNodeWithText(str(R.string.admin_error_loading_user))
            .assertDoesNotExist()
    }

    // endregion

    // region Error state

    @Test
    fun error_state_shows_error_message() {
        launchScreen(AdminUserDetailUiState.Error("Greška"))

        composeTestRule
            .onNodeWithText(str(R.string.admin_error_loading_user))
            .assertIsDisplayed()
    }

    @Test
    fun error_state_shows_retry_button() {
        launchScreen(AdminUserDetailUiState.Error("Greška"))

        composeTestRule
            .onNodeWithText(str(R.string.admin_retry))
            .assertIsDisplayed()
    }

    // endregion

    // region Success state — opći prikaz

    @Test
    fun success_state_shows_user_name() {
        launchScreen(
            AdminUserDetailUiState.Success(
                user = UserModel(name = "Marko Marković", email = "marko@test.com"),
            ),
        )

        composeTestRule.onNodeWithText("Marko Marković").assertIsDisplayed()
    }

    @Test
    fun success_state_shows_user_email() {
        launchScreen(
            AdminUserDetailUiState.Success(
                user = UserModel(name = "Ana Anić", email = "ana@test.com"),
            ),
        )

        composeTestRule.onAllNodesWithText("ana@test.com")[0].assertIsDisplayed()
    }

    @Test
    fun success_state_shows_avatar_initials() {
        launchScreen(
            AdminUserDetailUiState.Success(
                user = UserModel(name = "Pero Perić", email = "pero@test.com"),
            ),
        )

        composeTestRule.onNodeWithText("PE").assertIsDisplayed()
    }

    // endregion

    // region Success state — sekcije detalja

    @Test
    fun success_state_shows_contact_section() {
        launchScreen(
            AdminUserDetailUiState.Success(
                user = UserModel(name = "Test", email = "test@test.com", phoneNumber = "0911234567"),
            ),
        )

        composeTestRule.onNodeWithText(str(R.string.admin_section_contact)).assertIsDisplayed()
        composeTestRule.onNodeWithText("0911234567").assertIsDisplayed()
    }

    @Test
    fun success_state_shows_address_section() {
        launchScreen(
            AdminUserDetailUiState.Success(
                user =
                    UserModel(
                        name = "Test",
                        email = "test@test.com",
                        street = "Varaždinska 1",
                        city = "Zagreb",
                        postNumber = "10000",
                    ),
            ),
        )

        composeTestRule.onNodeWithText(str(R.string.admin_section_address)).assertIsDisplayed()
        composeTestRule.onNodeWithText("Varaždinska 1").assertIsDisplayed()
        composeTestRule.onNodeWithText("Zagreb").assertIsDisplayed()
        composeTestRule.onNodeWithText("10000").assertIsDisplayed()
    }

    @Test
    fun success_state_shows_account_section_with_role_and_status() {
        launchScreen(
            AdminUserDetailUiState.Success(
                user = UserModel(name = "Test", email = "test@test.com", admin = false, suspended = false),
            ),
        )

        composeTestRule.onNodeWithText(str(R.string.admin_section_account)).assertIsDisplayed()
        composeTestRule.onNodeWithText(str(R.string.value_user)).assertIsDisplayed()
        composeTestRule.onNodeWithText(str(R.string.value_active)).assertIsDisplayed()
    }

    // endregion

    // region Success state — aktivan korisnik

    @Test
    fun active_user_shows_suspend_button() {
        launchScreen(
            AdminUserDetailUiState.Success(
                user = UserModel(name = "Test", email = "test@test.com", suspended = false),
            ),
        )

        composeTestRule
            .onNodeWithText(str(R.string.admin_suspend_user))
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun active_user_shows_active_status() {
        launchScreen(
            AdminUserDetailUiState.Success(
                user = UserModel(name = "Test", email = "test@test.com", suspended = false),
            ),
        )

        composeTestRule.onNodeWithText(str(R.string.value_active)).assertIsDisplayed()
    }

    // endregion

    // region Success state — suspendiran korisnik

    @Test
    fun suspended_user_shows_reactivate_button() {
        launchScreen(
            AdminUserDetailUiState.Success(
                user = UserModel(name = "Test", email = "test@test.com", suspended = true),
            ),
        )

        composeTestRule
            .onNodeWithText(str(R.string.admin_reactivate_user))
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun suspended_user_shows_suspended_status() {
        launchScreen(
            AdminUserDetailUiState.Success(
                user = UserModel(name = "Test", email = "test@test.com", suspended = true),
            ),
        )

        composeTestRule.onNodeWithText(str(R.string.value_suspended)).assertIsDisplayed()
    }

    @Test
    fun suspended_user_shows_suspension_section_with_reason() {
        launchScreen(
            AdminUserDetailUiState.Success(
                user =
                    UserModel(
                        name = "Test",
                        email = "test@test.com",
                        suspended = true,
                        suspendedReason = "Kršenje pravila",
                    ),
            ),
        )

        composeTestRule.onNodeWithText(str(R.string.admin_section_suspension)).performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Kršenje pravila").performScrollTo().assertIsDisplayed()
    }

    // endregion

    // region Success state — admin korisnik

    @Test
    fun admin_user_shows_administrator_role() {
        launchScreen(
            AdminUserDetailUiState.Success(
                user = UserModel(name = "Admin Test", email = "admin@test.com", admin = true),
            ),
        )

        composeTestRule.onNodeWithText(str(R.string.value_admin)).assertIsDisplayed()
    }

    @Test
    fun admin_user_does_not_show_action_button() {
        launchScreen(
            AdminUserDetailUiState.Success(
                user = UserModel(name = "Admin Test", email = "admin@test.com", admin = true),
            ),
        )

        composeTestRule.onNodeWithText(str(R.string.admin_suspend_user)).assertDoesNotExist()
        composeTestRule.onNodeWithText(str(R.string.admin_reactivate_user)).assertDoesNotExist()
    }

    // endregion

    // region Suspend dialog

    @Test
    fun clicking_suspend_button_opens_suspension_dialog() {
        launchScreen(
            AdminUserDetailUiState.Success(
                user = UserModel(name = "Test", email = "test@test.com", suspended = false),
            ),
        )

        composeTestRule.onNodeWithText(str(R.string.admin_suspend_user)).performScrollTo().performClick()
        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithText(str(R.string.admin_suspend_dialog_title))
            .assertIsDisplayed()
    }

    @Test
    fun suspension_dialog_shows_user_name() {
        launchScreen(
            AdminUserDetailUiState.Success(
                user = UserModel(name = "Pero Perić", email = "pero@test.com", suspended = false),
            ),
        )

        composeTestRule.onNodeWithText(str(R.string.admin_suspend_user)).performScrollTo().performClick()
        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithTag("dialog_user_name")
            .assertIsDisplayed()
    }

    @Test
    fun suspension_dialog_dismiss_closes_dialog() {
        val fakeViewModel =
            launchScreen(
                AdminUserDetailUiState.Success(
                    user = UserModel(name = "Test", email = "test@test.com", suspended = false),
                    showSuspendDialog = true,
                ),
            )

        composeTestRule.onNodeWithText(str(R.string.admin_dialog_cancel)).performClick()
        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithText(str(R.string.admin_suspend_dialog_title))
            .assertDoesNotExist()
    }

    @Test
    fun suspension_dialog_confirm_requires_reason() {
        launchScreen(
            AdminUserDetailUiState.Success(
                user = UserModel(name = "Test", email = "test@test.com", suspended = false),
                showSuspendDialog = true,
            ),
        )

        composeTestRule
            .onNodeWithText(str(R.string.admin_suspend_dialog_reason_required), useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun suspension_dialog_confirm_enabled_when_reason_entered() {
        launchScreen(
            AdminUserDetailUiState.Success(
                user = UserModel(name = "Test", email = "test@test.com", suspended = false),
                showSuspendDialog = true,
            ),
        )

        composeTestRule
            .onNodeWithTag("field_suspend_reason")
            .performTextInput("Spam")
        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithTag("btn_suspend_dialog_confirm")
            .assertIsEnabled()
    }

    // endregion

    // region Reactivation dialog

    @Test
    fun clicking_reactivate_button_opens_reactivation_dialog() {
        launchScreen(
            AdminUserDetailUiState.Success(
                user = UserModel(name = "Test", email = "test@test.com", suspended = true),
            ),
        )

        composeTestRule.onNodeWithText(str(R.string.admin_reactivate_user)).performScrollTo().performClick()
        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithText(str(R.string.admin_reactivate_dialog_title))
            .assertIsDisplayed()
    }

    @Test
    fun reactivation_dialog_shows_confirmation_message() {
        launchScreen(
            AdminUserDetailUiState.Success(
                user = UserModel(name = "Test", email = "test@test.com", suspended = true),
                showReactivateDialog = true,
            ),
        )

        composeTestRule
            .onNodeWithText(str(R.string.admin_reactivate_dialog_message))
            .assertIsDisplayed()
    }

    @Test
    fun reactivation_dialog_dismiss_closes_dialog() {
        launchScreen(
            AdminUserDetailUiState.Success(
                user = UserModel(name = "Test", email = "test@test.com", suspended = true),
                showReactivateDialog = true,
            ),
        )

        composeTestRule.onNodeWithText(str(R.string.admin_dialog_cancel)).performClick()
        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithText(str(R.string.admin_reactivate_dialog_title))
            .assertDoesNotExist()
    }

    // endregion

    // region Navigacija

    @Test
    fun back_button_calls_onNavigateBack() {
        var navigatedBack = false
        launchScreen(
            state = AdminUserDetailUiState.Loading,
            onNavigateBack = { navigatedBack = true },
        )

        composeTestRule
            .onNodeWithContentDescription(str(R.string.desc_navigate_back))
            .performClick()
        composeTestRule.waitForIdle()

        assert(navigatedBack)
    }

    @Test
    fun top_bar_shows_screen_title() {
        launchScreen(AdminUserDetailUiState.Loading)

        composeTestRule
            .onNodeWithText(str(R.string.title_admin_user_detail))
            .assertIsDisplayed()
    }

    // endregion
}
