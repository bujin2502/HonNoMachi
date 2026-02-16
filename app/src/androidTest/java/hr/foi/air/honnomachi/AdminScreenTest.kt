package hr.foi.air.honnomachi

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
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

/**
 * Lažni ViewModel za testiranje AdminUserListScreen bez Firebase poziva.
 *
 * Koristi reflection za postavljanje privatnog stanja (_uiState)
 * i nadjačava init metode da spriječi stvarne dohvate.
 */
class FakeAdminUserListViewModel(
    repository: AdminRepository,
) : AdminUserListViewModel(repository) {
    /**
     * Postavlja stanje ekrana putem reflection-a.
     */
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
}

/**
 * Instrumentirani testovi za [AdminUserListScreen].
 *
 * Provjeravaju prikaz različitih stanja ekrana (lista korisnika, prazno stanje,
 * greška, polje za pretragu) koristeći [FakeAdminUserListViewModel].
 */
@RunWith(AndroidJUnit4::class)
class AdminScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var fakeViewModel: FakeAdminUserListViewModel
    private lateinit var mockRepository: AdminRepository

    private fun launchScreen(state: AdminUserListUiState) {
        mockRepository = mockk(relaxed = true)
        fakeViewModel = FakeAdminUserListViewModel(mockRepository)
        fakeViewModel.setState(state)

        composeTestRule.setContent {
            AdminUserListScreen(
                onNavigateBack = {},
                onNavigateToUserDetail = {},
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
}
