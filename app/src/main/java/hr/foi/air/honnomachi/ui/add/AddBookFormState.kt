package hr.foi.air.honnomachi.ui.add

import android.net.Uri
import hr.foi.air.honnomachi.model.BookCondition
import hr.foi.air.honnomachi.model.BookGenre
import hr.foi.air.honnomachi.model.Currency
import hr.foi.air.honnomachi.model.Language

data class AddBookFormState(
    val title: String = "",
    val authors: String = "",
    val price: String = "",
    val publisher: String = "",
    val publicationYear: String = "",
    val isbn: String = "",
    val description: String = "",
    val imageUris: List<Uri> = emptyList(),
    val selectedGenre: BookGenre = BookGenre.OTHER,
    val selectedCondition: BookCondition = BookCondition.USED,
    val selectedLanguage: Language = Language.HR,
    val selectedCurrency: Currency = Currency.EUR,
    val titleError: Int? = null,
    val authorsError: Int? = null,
    val priceError: Int? = null,
    val yearError: Int? = null,
)
