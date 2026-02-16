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
     * @param showSuspendDialog Prikazuje li se dijalog za suspenziju.
     * @param showReactivateDialog Prikazuje li se dijalog za reaktivaciju.
     * @param isActionLoading Izvršava li se trenutno akcija suspenzije/reaktivacije.
     * @param actionMessage Poruka za Snackbar nakon akcije, ili null.
     */
    data class Success(
        val user: UserModel,
        val showSuspendDialog: Boolean = false,
        val showReactivateDialog: Boolean = false,
        val isActionLoading: Boolean = false,
        val actionMessage: String? = null,
    ) : AdminUserDetailUiState

    /**
     * Greška pri dohvatu podataka korisnika.
     *
     * @param message Poruka greške.
     */
    data class Error(
        val message: String,
    ) : AdminUserDetailUiState
}
