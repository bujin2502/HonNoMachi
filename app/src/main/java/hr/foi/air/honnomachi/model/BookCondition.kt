package hr.foi.air.honnomachi.model

import androidx.annotation.StringRes
import hr.foi.air.honnomachi.R

enum class BookCondition(
    @param:StringRes val resourceId: Int,
) {
    NEW(R.string.condition_new),
    LIKE_NEW(R.string.condition_like_new),
    USED(R.string.condition_used),
    DAMAGED(R.string.condition_damaged),
}
