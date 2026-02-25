package hr.foi.air.honnomachi.ui.admin

import hr.foi.air.honnomachi.model.UserModel

enum class UserFilter {
    ALL,
    ACTIVE,
    SUSPENDED,
}

/**
 * Stanje korisničkog sučelja za ekran s listom korisnika.
 *
 * @param isLoading Inicijalno učitavanje prve stranice.
 * @param users Lista učitanih korisnika.
 * @param errorMessage Poruka greške ako dohvat ne uspije.
 * @param isLoadingMore Učitavanje sljedeće stranice (infinite scroll).
 * @param hasMorePages Postoje li još stranice za učitavanje.
 * @param isRefreshing Pull-to-refresh u tijeku.
 * @param searchQuery Trenutni tekst pretrage.
 * @param selectedFilter Odabrani filter statusa korisnika.
 * @param scrollToTopTrigger Brojač koji signalizira UI-u da scrolla na vrh liste.
 */
data class AdminUserListUiState(
    val isLoading: Boolean = true,
    val users: List<UserModel> = emptyList(),
    val errorMessage: String? = null,
    val isLoadingMore: Boolean = false,
    val hasMorePages: Boolean = true,
    val isRefreshing: Boolean = false,
    val searchQuery: String = "",
    val selectedFilter: UserFilter = UserFilter.ALL,
    val scrollToTopTrigger: Int = 0,
)
