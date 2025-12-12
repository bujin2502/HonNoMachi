package hr.foi.air.honnomachi.model

import androidx.annotation.StringRes
import hr.foi.air.honnomachi.R

enum class Currency(
    @param:StringRes val resourceId: Int,
) {
    EUR(R.string.currency_eur),
    USD(R.string.currency_usd),
}
