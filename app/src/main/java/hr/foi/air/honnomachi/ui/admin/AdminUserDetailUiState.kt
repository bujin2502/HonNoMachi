package hr.foi.air.honnomachi.ui.admin

import hr.foi.air.honnomachi.model.UserModel

/**
 * Stanje korisničkog sučelja za ekran s detaljima korisnika.
 *
 * Modelira tri moguća stanja: učitavanje, uspješan dohvat ili greška.
 */
sealed interface AdminUserDetailUiState {
    /** Podaci korisnika se učitavaju. */
    data object Loading : AdminUserDetailUiState

    /**
     * Podaci korisnika uspješno dohvaćeni.
     *
     * @param user Dohvaćeni model korisnika.
     */
    data class Success(val user: UserModel) : AdminUserDetailUiState

    /**
     * Greška pri dohvatu podataka korisnika.
     *
     * @param message Poruka greške.
     */
    data class Error(val message: String) : AdminUserDetailUiState
}
