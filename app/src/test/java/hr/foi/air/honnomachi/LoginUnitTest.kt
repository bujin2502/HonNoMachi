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
class LoginUnitTest {
    private lateinit var authViewModel: AuthViewModel
    private val mockAuthRepository: AuthRepository = mockk()
    private val mockFirebaseAuth: FirebaseAuth = mockk(relaxed = true)
    private val testDispatcher = StandardTestDispatcher()

    private val testEmail = "test@example.com"
    private val testPassword = "password123"

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
    fun `login success updates uiState with user`() =
        runTest {
            // Arrange
            val mockUser = UserModel(uid = "123", email = testEmail, isVerified = true)
            coEvery { mockAuthRepository.login(testEmail, testPassword) } returns Result.Success(mockUser)

            // Act
            authViewModel.login(testEmail, testPassword)
            testDispatcher.scheduler.advanceUntilIdle()

            // Assert
            val uiState = authViewModel.uiState.first()
            assertEquals(mockUser, uiState.user)
            assertTrue(uiState.isUserLoggedIn)
            assertFalse(uiState.needsVerification)
            assertNull(uiState.errorMessage)
            assertFalse(uiState.isLoading)
        }

    @Test
    fun `login failure updates uiState with error`() =
        runTest {
            // Arrange
            val errorMessage = "Invalid credentials"
            coEvery { mockAuthRepository.login(testEmail, testPassword) } returns Result.Error(Exception(errorMessage))

            // Act
            authViewModel.login(testEmail, testPassword)
            testDispatcher.scheduler.advanceUntilIdle()

            // Assert
            val uiState = authViewModel.uiState.first()
            assertNull(uiState.user)
            assertFalse(uiState.isUserLoggedIn)
            assertEquals(errorMessage, uiState.errorMessage)
            assertFalse(uiState.isLoading)
        }
}
