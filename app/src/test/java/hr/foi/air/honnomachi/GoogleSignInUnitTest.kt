package hr.foi.air.honnomachi

import com.google.firebase.auth.FirebaseAuth
import hr.foi.air.honnomachi.data.AuthRepository
import hr.foi.air.honnomachi.model.UserModel
import hr.foi.air.honnomachi.ui.auth.AuthViewModel
import hr.foi.air.honnomachi.util.Result
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class GoogleSignInUnitTest {
    private lateinit var authViewModel: AuthViewModel
    private val mockAuthRepository: AuthRepository = mockk()
    private val mockFirebaseAuth: FirebaseAuth = mockk(relaxed = true)
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        authViewModel = AuthViewModel(mockAuthRepository, mockFirebaseAuth)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loginWithGoogle success updates uiState`() =
        runTest {
            val idToken = "test_id_token"
            val mockUser = UserModel(uid = "123", email = "test@gmail.com", isVerified = true)
            coEvery { mockAuthRepository.loginWithGoogle(idToken) } returns Result.Success(mockUser)

            authViewModel.loginWithGoogle(idToken)
            testDispatcher.scheduler.advanceUntilIdle()

            val uiState = authViewModel.uiState.first()
            assertEquals(mockUser, uiState.user)
            assertTrue(uiState.isUserLoggedIn)
            assertFalse(uiState.needsVerification)
            assertNull(uiState.errorMessage)
            assertFalse(uiState.isLoading)
            assertTrue(uiState.googleLoginResult?.success == true)
        }

    @Test
    fun `loginWithGoogle failure updates uiState`() =
        runTest {
            val idToken = "test_id_token"
            val error = "Google sign-in failed"
            coEvery { mockAuthRepository.loginWithGoogle(idToken) } returns Result.Error(Exception(error))

            authViewModel.loginWithGoogle(idToken)
            testDispatcher.scheduler.advanceUntilIdle()

            val uiState = authViewModel.uiState.first()
            assertNull(uiState.user)
            assertFalse(uiState.isUserLoggedIn)
            assertEquals(error, uiState.errorMessage)
            assertFalse(uiState.isLoading)
            assertTrue(uiState.googleLoginResult?.success == false)
            assertEquals(error, uiState.googleLoginResult?.message)
        }
}
