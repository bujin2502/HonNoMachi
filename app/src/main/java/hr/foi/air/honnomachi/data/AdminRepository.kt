package hr.foi.air.honnomachi.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import hr.foi.air.honnomachi.CrashlyticsManager
import hr.foi.air.honnomachi.model.UserModel
import hr.foi.air.honnomachi.util.Result
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * Sučelje repozitorija za administratorske operacije.
 *
 * Definira metode za upravljanje korisnicima koje su dostupne
 * isključivo administratorima. Implementacija koristi Firestore
 * kao izvor podataka.
 *
 * Bit će prošireno u HNM-290 s metodama za dohvat svih korisnika,
 * pretragu, filtriranje i straničenje.
 */
interface AdminRepository {
    /**
     * Provjerava ima li trenutno prijavljeni korisnik administratorske ovlasti.
     *
     * Dohvaća korisnički dokument iz Firestore-a i provjerava
     * polje `admin`. Koristi se za zaštitu admin ruta u navigaciji.
     *
     * @return [Result.Success] s `true` ako je korisnik admin, `false` ako nije,
     *         ili [Result.Error] ako korisnik nije prijavljen ili dođe do greške.
     */
    suspend fun isCurrentUserAdmin(): Result<Boolean>
}

/**
 * Implementacija [AdminRepository] koja koristi Firebase Firestore.
 *
 * Sve greške se logiraju putem [CrashlyticsManager].
 *
 * @param auth Firebase Authentication instanca za dohvat trenutnog korisnika.
 * @param firestore Firebase Firestore instanca za pristup bazi podataka.
 */
class AdminRepositoryImpl
    @Inject
    constructor(
        private val auth: FirebaseAuth,
        private val firestore: FirebaseFirestore,
    ) : AdminRepository {
        override suspend fun isCurrentUserAdmin(): Result<Boolean> =
            try {
                val currentUser = auth.currentUser
                if (currentUser == null) {
                    Result.Error(Exception("No user logged in."))
                } else {
                    // Dohvati dokument trenutnog korisnika iz Firestore-a
                    val document =
                        firestore
                            .collection("users")
                            .document(currentUser.uid)
                            .get()
                            .await()

                    val user = document.toObject(UserModel::class.java)
                    Result.Success(user?.admin == true)
                }
            } catch (e: Exception) {
                CrashlyticsManager.instance.logException(e)
                Result.Error(e)
            }
    }
