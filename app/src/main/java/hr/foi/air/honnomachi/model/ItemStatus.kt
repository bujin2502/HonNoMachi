package hr.foi.air.honnomachi.model

import androidx.annotation.StringRes
import hr.foi.air.honnomachi.R

enum class ItemStatus(
    @param:StringRes val resourceId: Int,
) {
    AVAILABLE(R.string.status_available),
    SOLD(R.string.status_sold),
    PENDING_SALE(R.string.status_pending_sale),
    INACTIVE(R.string.status_inactive),
}
