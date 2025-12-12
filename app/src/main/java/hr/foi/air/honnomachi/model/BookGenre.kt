package hr.foi.air.honnomachi.model

import androidx.annotation.StringRes
import hr.foi.air.honnomachi.R

enum class BookGenre(
    @param:StringRes val resourceId: Int,
) {
    FANTASY(R.string.genre_fantasy),
    SCIENCE_FICTION(R.string.genre_science_fiction),
    MYSTERY(R.string.genre_mystery),
    ROMANCE(R.string.genre_romance),
    THRILLER(R.string.genre_thriller),
    HORROR(R.string.genre_horror),
    NON_FICTION(R.string.genre_non_fiction),
    HISTORY(R.string.genre_history),
    BIOGRAPHY(R.string.genre_biography),
    COMPUTER_SCIENCE(R.string.genre_computer_science),
    SELF_HELP(R.string.genre_self_help),
    TRAVEL(R.string.genre_travel),
    COOKING(R.string.genre_cooking),
    FICTION(R.string.genre_fiction),
    MARTIAL_ARTS(R.string.genre_martial_arts),
    HEALTH(R.string.genre_health),
    FITNESS(R.string.genre_fitness),
    OTHER(R.string.genre_other),
}
