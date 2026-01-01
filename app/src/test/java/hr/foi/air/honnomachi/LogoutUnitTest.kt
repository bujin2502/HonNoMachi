package hr.foi.air.honnomachi

import com.google.firebase.auth.FirebaseAuth
import hr.foi.air.honnomachi.data.AuthRepository
import hr.foi.air.honnomachi.ui.auth.AuthViewModel
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class LogoutUnitTest {
    private lateinit var authViewModel: AuthViewModel
    private val mockAuthRepository: AuthRepository = mockk(relaxed = true)
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
    fun `signOut calls repository signOut`() =
        runTest {
            // When
            authViewModel.signOut()
            testDispatcher.scheduler.advanceUntilIdle()

            // Then
            coVerify { mockAuthRepository.signOut() }
        }
}
