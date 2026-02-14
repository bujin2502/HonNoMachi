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

/**
 * Lažni ViewModel za zaobilaženje Firebase poziva tijekom testiranja.
 *
 *
 * Na ovaj način odvajaš "testiranje UI-a" od "prave baze podataka" (Firebase) te njezine kompleksne "asinkrone logike"...
 *
 * Omogućuje ručno postavljanje stanja ([setState]) i simulaciju poslovne logike bez stvarnih repozitorija.
 *
 */
class FakeProfileViewModel(
    repository: ProfileRepository,
) : ProfileViewModel(repository) {


    /**
     * Pomoćna funkcija za postavljanje privatnog stanja ViewModela (`_uiState`) koristeći refleksiju.
     *
     * Ovo je nužno jer je `_uiState` privatan, a za testiranje trebamo postaviti
     * točno određeno početno stanje (npr. [ProfileUiState.Success] s podacima korisnika).
     */
    fun setState(state: ProfileUiState) {
        val field = ProfileViewModel::class.java.getDeclaredField("_uiState")
        field.isAccessible = true
        val stateFlow = field.get(this) as MutableStateFlow<ProfileUiState>
        stateFlow.value = state


        if (state is ProfileUiState.Success) {      // Popuni stanje forme s podacima korisnika...
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

    /**
     * Simulira logiku spremanja profila.
     *
     * Umjesto slanja podataka na server, ova metoda lokalno provjerava validnost
     * unosa (npr. je li ime prazno) i odmah vraća rezultat.
     *
     * @param onResult Callback funkcija koja vraća uspjeh (true) ili neuspjeh (false).
     */
    override fun saveProfile(onResult: (Boolean, String?) -> Unit) {

        if (formState.value.name.isBlank()) {
            validateName() // ovo ažurira stanje greške (error state)...
            onResult(false, "Error")
            return
        }
        onResult(true, null)
    }
}


/**
 * Instrumentirani testovi za [ProfileScreen].
 *
 * Provjeravaju interakciju korisnika s UI elementima ekrana profila,
 * validaciju unosa i prikaz podataka, koristeći [FakeProfileViewModel].
 */
@RunWith(AndroidJUnit4::class)
class ProfileScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var fakeViewModel: FakeProfileViewModel
    private lateinit var mockRepository: ProfileRepository

    private fun launchScreen() {

        // Postavi početno stanje korisnika
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

        mockRepository = mockk(relaxed = true)
        coEvery { mockRepository.getUserProfile() } returns Result.Success(user)

        fakeViewModel = FakeProfileViewModel(mockRepository)
        fakeViewModel.setState(ProfileUiState.Success(user))

        composeTestRule.setContent {
            ProfileScreen(
                paddingValues = PaddingValues(0.dp),
                onLogout = {},
                onNavigateToChangePassword = {},
                onNavigateToPrivacyPolicy = {},
                onNavigateToAdmin = {},
                profileViewModel = fakeViewModel,
            )
        }
    }


    /**
     * Testira scenarij uspješnog uređivanja profila.
     *
     * Koraci:
     * 1. Prikazuje se ekran s početnim imenom.
     * 2. Korisnik briše staro ime i upisuje novo.
     * 3. Korisnik klika na gumb "Spremi".
     * 4. Očekuje se da nema grešaka (simulacija uspjeha).
     */
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

        // Pronađi polje za unos preko početne vrijednosti, zatim ga očisti
        composeTestRule.onNodeWithText("Initial Name").performTextClearance()

        // Pronađi po oznaci (labeli) sada kada je vrijednost nestala i unesi novu
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


    /**
     * Testira validaciju unosa (neispravan unos).
     *
     * Koraci:
     * 1. Korisnik briše ime (ostavlja prazno polje).
     * 2. Korisnik klikne na gumb "Spremi".
     * 3. Očekuje se prikaz poruke o greški (da je ime obavezno).
     */
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
}
