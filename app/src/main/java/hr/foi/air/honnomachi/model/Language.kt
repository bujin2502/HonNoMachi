package hr.foi.air.honnomachi.model

import androidx.annotation.StringRes
import hr.foi.air.honnomachi.R

enum class Language(@param:StringRes val resourceId: Int) {
    HR(R.string.language_hr),
    EN(R.string.language_en),
    DE(R.string.language_de),
    FR(R.string.language_fr),
    IT(R.string.language_it),
    ES(R.string.language_es),
    JA(R.string.language_ja)
}
