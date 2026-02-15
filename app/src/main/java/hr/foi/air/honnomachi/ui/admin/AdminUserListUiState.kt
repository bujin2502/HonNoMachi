package hr.foi.air.honnomachi.ui.admin

import hr.foi.air.honnomachi.model.UserModel

/**
 * Stanje korisničkog sučelja za ekran s listom korisnika.
 *
 * @param isLoading Inicijalno učitavanje prve stranice.
 * @param users Lista učitanih korisnika.
 * @param errorMessage Poruka greške ako dohvat ne uspije.
 * @param isLoadingMore Učitavanje sljedeće stranice (infinite scroll).
 * @param hasMorePages Postoje li još stranice za učitavanje.
 * @param isRefreshing Pull-to-refresh u tijeku.
 */
data class AdminUserListUiState(
    val isLoading: Boolean = true,
    val users: List<UserModel> = emptyList(),
    val errorMessage: String? = null,
    val isLoadingMore: Boolean = false,
    val hasMorePages: Boolean = true,
    val isRefreshing: Boolean = false,
)
