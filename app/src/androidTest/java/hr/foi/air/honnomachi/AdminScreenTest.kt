package hr.foi.air.honnomachi

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import hr.foi.air.honnomachi.data.AdminRepository
import hr.foi.air.honnomachi.model.UserModel
import hr.foi.air.honnomachi.ui.admin.AdminUserListScreen
import hr.foi.air.honnomachi.ui.admin.AdminUserListUiState
import hr.foi.air.honnomachi.ui.admin.AdminUserListViewModel
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

class FakeAdminUserListViewModel(
    repository: AdminRepository,
) : AdminUserListViewModel(repository) {
    fun setState(state: AdminUserListUiState) {
        val field = AdminUserListViewModel::class.java.getDeclaredField("_uiState")
        field.isAccessible = true
        val stateFlow = field.get(this) as MutableStateFlow<AdminUserListUiState>
        stateFlow.value = state
    }

    override fun loadUsers() {
        /** Prazna implementacija — sprječava Firebase pozive. */
    }

    override fun loadMoreUsers() {
        /** Prazna implementacija — sprječava Firebase pozive pri infinite scroll. */
    }

    override fun refreshUsers() {
        /** Prazna implementacija — sprječava Firebase pozive pri refresh. */
    }
}

@RunWith(AndroidJUnit4::class)
class AdminScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var fakeViewModel: FakeAdminUserListViewModel
    private lateinit var mockRepository: AdminRepository

    private fun launchScreen(
        state: AdminUserListUiState,
        onNavigateBack: () -> Unit = {},
        onNavigateToUserDetail: (String) -> Unit = {},
    ) {
        mockRepository = mockk(relaxed = true)
        fakeViewModel = FakeAdminUserListViewModel(mockRepository)
        fakeViewModel.setState(state)

        composeTestRule.setContent {
            AdminUserListScreen(
                onNavigateBack = onNavigateBack,
                onNavigateToUserDetail = onNavigateToUserDetail,
                viewModel = fakeViewModel,
            )
        }
    }

    @Test
    fun user_list_screen_displays_users() {
        val testUsers =
            listOf(
                UserModel(uid = "1", name = "Alice Test", email = "alice@test.com"),
                UserModel(uid = "2", name = "Bob Test", email = "bob@test.com"),
            )

        launchScreen(
            AdminUserListUiState(
                isLoading = false,
                users = testUsers,
            ),
        )

        composeTestRule.onNodeWithText("Alice Test").assertIsDisplayed()
        composeTestRule.onNodeWithText("Bob Test").assertIsDisplayed()
    }

    @Test
    fun empty_state_shows_no_users_message() {
        launchScreen(
            AdminUserListUiState(
                isLoading = false,
                users = emptyList(),
                hasMorePages = false,
            ),
        )

        val context =
            androidx.test.platform.app.InstrumentationRegistry
                .getInstrumentation()
                .targetContext
        val noUsersText = context.getString(R.string.admin_no_users)

        composeTestRule.onNodeWithText(noUsersText).assertIsDisplayed()
    }

    @Test
    fun error_state_shows_retry_button() {
        launchScreen(
            AdminUserListUiState(
                isLoading = false,
                users = emptyList(),
                errorMessage = "Test error",
            ),
        )

        val context =
            androidx.test.platform.app.InstrumentationRegistry
                .getInstrumentation()
                .targetContext
        val errorText = context.getString(R.string.admin_error_loading_users)
        val retryText = context.getString(R.string.admin_retry)

        composeTestRule.onNodeWithText(errorText).assertIsDisplayed()
        composeTestRule.onNodeWithText(retryText).assertIsDisplayed()
    }

    @Test
    fun search_field_is_displayed() {
        launchScreen(
            AdminUserListUiState(
                isLoading = false,
                users = emptyList(),
            ),
        )

        val context =
            androidx.test.platform.app.InstrumentationRegistry
                .getInstrumentation()
                .targetContext
        val searchHint = context.getString(R.string.admin_search_hint)

        composeTestRule.onNodeWithText(searchHint).assertIsDisplayed()
    }

    // region Top bar

    @Test
    fun top_bar_shows_screen_title() {
        launchScreen(AdminUserListUiState(isLoading = false, users = emptyList()))

        val context =
            androidx.test.platform.app.InstrumentationRegistry
                .getInstrumentation()
                .targetContext
        composeTestRule
            .onNodeWithText(context.getString(R.string.title_admin_user_list))
            .assertIsDisplayed()
    }

    @Test
    fun back_button_calls_navigate_back() {
        var navigatedBack = false
        launchScreen(
            state = AdminUserListUiState(isLoading = false, users = emptyList()),
            onNavigateBack = { navigatedBack = true },
        )

        val context =
            androidx.test.platform.app.InstrumentationRegistry
                .getInstrumentation()
                .targetContext
        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.desc_navigate_back))
            .performClick()
        composeTestRule.waitForIdle()

        assert(navigatedBack)
    }

    // endregion

    // region Filter chips

    @Test
    fun filter_chips_all_are_displayed() {
        launchScreen(AdminUserListUiState(isLoading = false, users = emptyList()))

        val context =
            androidx.test.platform.app.InstrumentationRegistry
                .getInstrumentation()
                .targetContext
        composeTestRule.onNodeWithText(context.getString(R.string.admin_filter_all)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.admin_filter_active)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.admin_filter_suspended)).assertIsDisplayed()
    }

    // endregion

    // region Prikaz korisnika u listi

    @Test
    fun user_item_shows_email() {
        launchScreen(
            AdminUserListUiState(
                isLoading = false,
                users = listOf(UserModel(uid = "1", name = "Ana Anić", email = "ana@test.com")),
            ),
        )

        composeTestRule.onNodeWithText("ana@test.com").assertIsDisplayed()
    }

    @Test
    fun active_user_shows_active_status_badge() {
        val context =
            androidx.test.platform.app.InstrumentationRegistry
                .getInstrumentation()
                .targetContext

        launchScreen(
            AdminUserListUiState(
                isLoading = false,
                users = listOf(UserModel(uid = "1", name = "Aktivan Korisnik", email = "a@test.com", suspended = false)),
            ),
        )

        composeTestRule
            .onNodeWithText(context.getString(R.string.value_active))
            .assertIsDisplayed()
    }

    @Test
    fun suspended_user_shows_suspended_status_badge() {
        val context =
            androidx.test.platform.app.InstrumentationRegistry
                .getInstrumentation()
                .targetContext

        launchScreen(
            AdminUserListUiState(
                isLoading = false,
                users = listOf(UserModel(uid = "1", name = "Suspendiran Korisnik", email = "s@test.com", suspended = true)),
            ),
        )

        composeTestRule
            .onNodeWithText(context.getString(R.string.value_suspended))
            .assertIsDisplayed()
    }

    @Test
    fun clicking_user_navigates_to_user_detail() {
        var navigatedToUid: String? = null
        launchScreen(
            state =
                AdminUserListUiState(
                    isLoading = false,
                    users = listOf(UserModel(uid = "uid-42", name = "Klikni Me", email = "klik@test.com")),
                ),
            onNavigateToUserDetail = { navigatedToUid = it },
        )

        composeTestRule.onNodeWithText("Klikni Me").performClick()
        composeTestRule.waitForIdle()

        assert(navigatedToUid == "uid-42")
    }

    // endregion

    // region Stanje učitavanja

    @Test
    fun loading_state_does_not_show_users_or_error() {
        val context =
            androidx.test.platform.app.InstrumentationRegistry
                .getInstrumentation()
                .targetContext

        launchScreen(AdminUserListUiState(isLoading = true, users = emptyList()))

        composeTestRule.onNodeWithText("Alice Test").assertDoesNotExist()
        composeTestRule
            .onNodeWithText(context.getString(R.string.admin_error_loading_users))
            .assertDoesNotExist()
    }

    // endregion
}
