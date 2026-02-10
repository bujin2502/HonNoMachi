package hr.foi.air.honnomachi.viewmodel

import com.google.firebase.auth.FirebaseAuth
import hr.foi.air.honnomachi.data.AuthRepository
import hr.foi.air.honnomachi.model.UserModel
import hr.foi.air.honnomachi.ui.auth.AuthViewModel
import hr.foi.air.honnomachi.util.Result
import io.mockk.coEvery
import io.mockk.coVerify
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
class AuthViewModelTest {
    private lateinit var authViewModel: AuthViewModel
    private val mockAuthRepository: AuthRepository = mockk(relaxed = true)
    private val mockFirebaseAuth: FirebaseAuth = mockk(relaxed = true)
    private val testDispatcher = StandardTestDispatcher()

    private val testEmail = "test@example.com"
    private val testPassword = "password123"
    private val testName = "Test User"

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
    fun `signup success updates uiState with user and needsVerification`() =
        runTest {
            val mockUser = UserModel(uid = "123", email = testEmail, isVerified = false)
            coEvery { mockAuthRepository.register(testName, testEmail, testPassword) } returns Result.Success(mockUser)

            authViewModel.signup(testName, testEmail, testPassword)
            testDispatcher.scheduler.advanceUntilIdle()

            val uiState = authViewModel.uiState.first()
            assertEquals(mockUser, uiState.user)
            assertTrue(uiState.needsVerification)
            assertNull(uiState.errorMessage)
            assertFalse(uiState.isLoading)
        }

    @Test
    fun `signup failure updates uiState with error`() =
        runTest {
            val errorMessage = "Email already in use"
            coEvery { mockAuthRepository.register(testName, testEmail, testPassword) } returns Result.Error(Exception(errorMessage))

            authViewModel.signup(testName, testEmail, testPassword)
            testDispatcher.scheduler.advanceUntilIdle()

            val uiState = authViewModel.uiState.first()
            assertNull(uiState.user)
            assertEquals(errorMessage, uiState.errorMessage)
            assertFalse(uiState.isLoading)
        }

    @Test
    fun `login with unverified user sets needsVerification`() =
        runTest {
            val unverifiedUser = UserModel(uid = "123", email = testEmail, isVerified = false)
            coEvery { mockAuthRepository.login(testEmail, testPassword) } returns Result.Success(unverifiedUser)

            authViewModel.login(testEmail, testPassword)
            testDispatcher.scheduler.advanceUntilIdle()

            val uiState = authViewModel.uiState.first()
            assertEquals(unverifiedUser, uiState.user)
            assertFalse(uiState.isUserLoggedIn)
            assertTrue(uiState.needsVerification)
            assertEquals(AuthViewModel.ErrorMessages.VERIFY_EMAIL, uiState.errorMessage)
        }

    @Test
    fun `forgotPassword success clears error`() =
        runTest {
            coEvery { mockAuthRepository.sendPasswordResetEmail(testEmail) } returns Result.Success(Unit)

            authViewModel.forgotPassword(testEmail)
            testDispatcher.scheduler.advanceUntilIdle()

            val uiState = authViewModel.uiState.first()
            assertNull(uiState.errorMessage)
            assertFalse(uiState.isLoading)
        }

    @Test
    fun `forgotPassword failure sets error`() =
        runTest {
            val errorMessage = "User not found"
            coEvery { mockAuthRepository.sendPasswordResetEmail(testEmail) } returns Result.Error(Exception(errorMessage))

            authViewModel.forgotPassword(testEmail)
            testDispatcher.scheduler.advanceUntilIdle()

            val uiState = authViewModel.uiState.first()
            assertEquals(errorMessage, uiState.errorMessage)
            assertFalse(uiState.isLoading)
        }

    @Test
    fun `checkSession success sets user logged in`() =
        runTest {
            val mockUser = UserModel(uid = "123", email = testEmail, isVerified = true)
            coEvery { mockAuthRepository.checkSession() } returns Result.Success(mockUser)

            authViewModel.checkSession()
            testDispatcher.scheduler.advanceUntilIdle()

            val uiState = authViewModel.uiState.first()
            assertEquals(mockUser, uiState.user)
            assertTrue(uiState.isUserLoggedIn)
            assertFalse(uiState.needsVerification)
            assertFalse(uiState.isLoading)
        }

    @Test
    fun `checkSession failure sets not logged in`() =
        runTest {
            val errorMessage = "Session expired"
            coEvery { mockAuthRepository.checkSession() } returns Result.Error(Exception(errorMessage))

            authViewModel.checkSession()
            testDispatcher.scheduler.advanceUntilIdle()

            val uiState = authViewModel.uiState.first()
            assertFalse(uiState.isUserLoggedIn)
            assertFalse(uiState.needsVerification)
            assertEquals(errorMessage, uiState.errorMessage)
            assertFalse(uiState.isLoading)
        }

    @Test
    fun `testSecureRead success sets secureReadResult`() =
        runTest {
            coEvery { mockAuthRepository.testSecureRead() } returns Result.Success("secure data")

            authViewModel.testSecureRead()
            testDispatcher.scheduler.advanceUntilIdle()

            val uiState = authViewModel.uiState.first()
            assertEquals("secure data", uiState.secureReadResult)
        }

    @Test
    fun `testSecureRead failure sets error`() =
        runTest {
            val errorMessage = "Access denied"
            coEvery { mockAuthRepository.testSecureRead() } returns Result.Error(Exception(errorMessage))

            authViewModel.testSecureRead()
            testDispatcher.scheduler.advanceUntilIdle()

            val uiState = authViewModel.uiState.first()
            assertNull(uiState.secureReadResult)
            assertEquals(errorMessage, uiState.errorMessage)
        }

    @Test
    fun `resendVerificationEmail success sets result`() =
        runTest {
            coEvery { mockAuthRepository.resendVerificationEmail(testEmail, testPassword) } returns Result.Success(Unit)

            authViewModel.resendVerificationEmail(testEmail, testPassword)
            testDispatcher.scheduler.advanceUntilIdle()

            val uiState = authViewModel.uiState.first()
            assertTrue(uiState.verificationEmailResult?.success == true)
            assertEquals(AuthViewModel.SuccessMessages.VERIFICATION_EMAIL_SENT, uiState.verificationEmailResult?.message)
            assertNull(uiState.errorMessage)
            assertFalse(uiState.isLoading)
        }

    @Test
    fun `resendVerificationEmail failure sets error`() =
        runTest {
            val errorMessage = "Failed to send"
            coEvery { mockAuthRepository.resendVerificationEmail(testEmail, testPassword) } returns Result.Error(Exception(errorMessage))

            authViewModel.resendVerificationEmail(testEmail, testPassword)
            testDispatcher.scheduler.advanceUntilIdle()

            val uiState = authViewModel.uiState.first()
            assertFalse(uiState.verificationEmailResult?.success == true)
            assertEquals(errorMessage, uiState.errorMessage)
            assertFalse(uiState.isLoading)
        }

    @Test
    fun `checkVerificationStatus success signs out and sets result`() =
        runTest {
            val mockUser = UserModel(uid = "123", email = testEmail, isVerified = true)
            coEvery { mockAuthRepository.syncVerificationStatus() } returns Result.Success(mockUser)

            authViewModel.checkVerificationStatus()
            testDispatcher.scheduler.advanceUntilIdle()

            val uiState = authViewModel.uiState.first()
            assertFalse(uiState.isUserLoggedIn)
            assertFalse(uiState.needsVerification)
            assertNull(uiState.user)
            assertTrue(uiState.verificationStatusResult?.success == true)
            assertEquals(AuthViewModel.SuccessMessages.EMAIL_VERIFIED, uiState.verificationStatusResult?.message)
            coVerify { mockAuthRepository.signOut() }
        }

    @Test
    fun `checkVerificationStatus failure sets error`() =
        runTest {
            val errorMessage = "Not verified yet"
            coEvery { mockAuthRepository.syncVerificationStatus() } returns Result.Error(Exception(errorMessage))

            authViewModel.checkVerificationStatus()
            testDispatcher.scheduler.advanceUntilIdle()

            val uiState = authViewModel.uiState.first()
            assertFalse(uiState.verificationStatusResult?.success == true)
            assertEquals(errorMessage, uiState.errorMessage)
            assertFalse(uiState.isLoading)
        }

    @Test
    fun `signOut resets uiState to default`() =
        runTest {
            authViewModel.signOut()
            testDispatcher.scheduler.advanceUntilIdle()

            val uiState = authViewModel.uiState.first()
            assertNull(uiState.user)
            assertFalse(uiState.isUserLoggedIn)
            assertFalse(uiState.needsVerification)
            assertNull(uiState.errorMessage)
            assertFalse(uiState.isLoading)
        }

    @Test
    fun `consumeErrorMessage clears error`() =
        runTest {
            coEvery { mockAuthRepository.login(testEmail, testPassword) } returns Result.Error(Exception("error"))
            authViewModel.login(testEmail, testPassword)
            testDispatcher.scheduler.advanceUntilIdle()

            authViewModel.consumeErrorMessage()

            assertNull(authViewModel.uiState.first().errorMessage)
        }

    @Test
    fun `consumeSecureReadResult clears result`() =
        runTest {
            coEvery { mockAuthRepository.testSecureRead() } returns Result.Success("data")
            authViewModel.testSecureRead()
            testDispatcher.scheduler.advanceUntilIdle()

            authViewModel.consumeSecureReadResult()

            assertNull(authViewModel.uiState.first().secureReadResult)
        }

    @Test
    fun `consumeGoogleLoginResult clears result`() =
        runTest {
            val mockUser = UserModel(uid = "123", email = testEmail, isVerified = true)
            coEvery { mockAuthRepository.loginWithGoogle("token") } returns Result.Success(mockUser)
            authViewModel.loginWithGoogle("token")
            testDispatcher.scheduler.advanceUntilIdle()

            authViewModel.consumeGoogleLoginResult()

            assertNull(authViewModel.uiState.first().googleLoginResult)
        }

    @Test
    fun `consumeVerificationEmailResult clears result`() =
        runTest {
            coEvery { mockAuthRepository.resendVerificationEmail(testEmail, testPassword) } returns Result.Success(Unit)
            authViewModel.resendVerificationEmail(testEmail, testPassword)
            testDispatcher.scheduler.advanceUntilIdle()

            authViewModel.consumeVerificationEmailResult()

            assertNull(authViewModel.uiState.first().verificationEmailResult)
        }

    @Test
    fun `consumeVerificationStatusResult clears result`() =
        runTest {
            val mockUser = UserModel(uid = "123", email = testEmail, isVerified = true)
            coEvery { mockAuthRepository.syncVerificationStatus() } returns Result.Success(mockUser)
            authViewModel.checkVerificationStatus()
            testDispatcher.scheduler.advanceUntilIdle()

            authViewModel.consumeVerificationStatusResult()

            assertNull(authViewModel.uiState.first().verificationStatusResult)
        }
}
