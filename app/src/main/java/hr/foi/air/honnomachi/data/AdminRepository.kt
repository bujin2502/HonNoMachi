package hr.foi.air.honnomachi.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import hr.foi.air.honnomachi.CrashlyticsManager
import hr.foi.air.honnomachi.model.UserModel
import hr.foi.air.honnomachi.util.Result
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * Stranica korisnika s referencom na zadnji dokument za straničenje.
 *
 * @param users Lista korisnika na stranici.
 * @param lastDocSnapshot Zadnji dokument stranice, koristi se za dohvat sljedeće stranice.
 */
data class UserPage(
    val users: List<UserModel>,
    val lastDocSnapshot: DocumentSnapshot?,
)

/**
 * Sučelje repozitorija za administratorske operacije nad korisnicima.
 *
 * Definira metode za dohvat, pretragu i filtriranje korisnika
 * koje su dostupne isključivo administratorima.
 */
interface AdminRepository {
    /**
     * Provjerava ima li trenutno prijavljeni korisnik administratorske ovlasti.
     *
     * @return [Result.Success] s `true` ako je korisnik admin, `false` ako nije,
     *         ili [Result.Error] ako korisnik nije prijavljen ili dođe do greške.
     */
    suspend fun isCurrentUserAdmin(): Result<Boolean>

    /**
     * Dohvaća stranicu korisnika s "cursor-based" straničenjem.
     *
     * Korisnici su sortirani po imenu. Za sljedeću stranicu,
     * proslijediti [lastDocSnapshot] zadnjeg korisnika s prethodne stranice.
     *
     * @param pageSize Broj korisnika po stranici.
     * @param lastDocSnapshot Zadnji dokument prethodne stranice za nastavak, ili `null` za prvu stranicu.
     * @return [UserPage] s listom korisnika i referencom na zadnji dokument.
     */
    suspend fun getAllUsers(
        pageSize: Int = 20,
        lastDocSnapshot: DocumentSnapshot? = null,
    ): Result<UserPage>

    /**
     * Dohvaća podatke o korisniku prema jedinstvenom identifikatoru.
     *
     * @param userId Firestore UID korisnika.
     */
    suspend fun getUserById(userId: String): Result<UserModel>

    /**
     * Pretražuje korisnike po imenu ili email adresi (case-insensitive).
     *
     * Dohvaća sve korisnike i filtrira lokalno jer Firestore
     * ne podržava case-insensitive pretragu.
     *
     * @param query Tekst za pretragu.
     */
    suspend fun searchUsers(query: String): Result<List<UserModel>>

    /**
     * Filtrira korisnike prema statusu računa.
     *
     * @param isSuspended `true` za suspendirane, `false` za aktivne korisnike.
     */
    suspend fun getUsersByStatus(isSuspended: Boolean): Result<List<UserModel>>
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

        override suspend fun getAllUsers(
            pageSize: Int,
            lastDocSnapshot: DocumentSnapshot?,
        ): Result<UserPage> =
            try {
                var query: Query =
                    firestore
                        .collection("users")
                        .orderBy("name")
                        .limit(pageSize.toLong())

                if (lastDocSnapshot != null) {
                    query = query.startAfter(lastDocSnapshot)
                }

                val snapshot = query.get().await()

                val users =
                    snapshot.documents.mapNotNull { doc ->
                        doc.toObject(UserModel::class.java)?.copy(uid = doc.id)
                    }
                Result.Success(UserPage(users, snapshot.documents.lastOrNull()))
            } catch (e: Exception) {
                CrashlyticsManager.instance.logException(e)
                Result.Error(e)
            }

        override suspend fun getUserById(userId: String): Result<UserModel> =
            try {
                val document =
                    firestore
                        .collection("users")
                        .document(userId)
                        .get()
                        .await()

                if (document.exists()) {
                    val user = document.toObject(UserModel::class.java)?.copy(uid = document.id)
                    if (user != null) {
                        Result.Success(user)
                    } else {
                        Result.Error(Exception("Failed to parse user data."))
                    }
                } else {
                    Result.Error(Exception("User not found."))
                }
            } catch (e: Exception) {
                CrashlyticsManager.instance.logException(e)
                Result.Error(e)
            }

        override suspend fun searchUsers(query: String): Result<List<UserModel>> =
            try {
                val snapshot =
                    firestore
                        .collection("users")
                        .get()
                        .await()

                val allUsers =
                    snapshot.documents.mapNotNull { doc ->
                        doc.toObject(UserModel::class.java)?.copy(uid = doc.id)
                    }

                val filtered =
                    allUsers.filter { user ->
                        user.name.contains(query, ignoreCase = true) ||
                            user.email.contains(query, ignoreCase = true)
                    }
                Result.Success(filtered)
            } catch (e: Exception) {
                CrashlyticsManager.instance.logException(e)
                Result.Error(e)
            }

        override suspend fun getUsersByStatus(isSuspended: Boolean): Result<List<UserModel>> =
            try {
                val snapshot =
                    firestore
                        .collection("users")
                        .get()
                        .await()

                val allUsers =
                    snapshot.documents.mapNotNull { doc ->
                        doc.toObject(UserModel::class.java)?.copy(uid = doc.id)
                    }

                val filtered =
                    if (isSuspended) {
                        allUsers.filter { it.suspended == true }
                    } else {
                        allUsers.filter { it.suspended != true }
                    }
                Result.Success(filtered)
            } catch (e: Exception) {
                CrashlyticsManager.instance.logException(e)
                Result.Error(e)
            }
    }
