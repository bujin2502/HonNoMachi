package hr.foi.air.honnomachi.ui.add

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import hr.foi.air.honnomachi.R
import hr.foi.air.honnomachi.model.BookCondition
import hr.foi.air.honnomachi.model.BookGenre
import hr.foi.air.honnomachi.model.Currency
import hr.foi.air.honnomachi.model.Language

private object FormDimensions {
    val FieldSpacing = 12.dp
    val DescriptionHeight = 120.dp
}

@Composable
fun TitleField(
    value: String,
    onValueChange: (String) -> Unit,
    error: Int?,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(stringResource(R.string.field_title)) },
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        isError = error != null,
        supportingText = {
            error?.let {
                Text(
                    text = stringResource(it),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
    )
}

@Composable
fun AuthorsField(
    value: String,
    onValueChange: (String) -> Unit,
    error: Int?,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(stringResource(R.string.field_authors)) },
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        isError = error != null,
        supportingText = {
            if (error != null) {
                Text(
                    text = stringResource(error),
                    color = MaterialTheme.colorScheme.error,
                )
            } else {
                Text(text = stringResource(R.string.hint_authors))
            }
        },
    )
}

@Composable
fun PriceWithCurrencyRow(
    price: String,
    onPriceChange: (String) -> Unit,
    priceError: Int?,
    selectedCurrency: Currency,
    onCurrencyChange: (Currency) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = price,
            onValueChange = onPriceChange,
            label = { Text(stringResource(R.string.field_price)) },
            modifier = Modifier.weight(1f),
            singleLine = true,
            keyboardOptions =
                KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Next,
                ),
            isError = priceError != null,
            supportingText = {
                priceError?.let {
                    Text(
                        text = stringResource(it),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
        )
        Spacer(modifier = Modifier.width(FormDimensions.FieldSpacing))
        DropdownField(
            label = stringResource(R.string.field_currency),
            options = Currency.entries,
            selectedOption = selectedCurrency,
            optionLabel = { stringResource(it.resourceId) },
            onOptionSelected = onCurrencyChange,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
fun GenreField(
    selectedGenre: BookGenre,
    onGenreChange: (BookGenre) -> Unit,
    modifier: Modifier = Modifier,
) {
    DropdownField(
        label = stringResource(R.string.field_genre),
        options = BookGenre.entries,
        selectedOption = selectedGenre,
        optionLabel = { stringResource(it.resourceId) },
        onOptionSelected = onGenreChange,
        modifier = modifier,
    )
}

@Composable
fun ConditionField(
    selectedCondition: BookCondition,
    onConditionChange: (BookCondition) -> Unit,
    modifier: Modifier = Modifier,
) {
    DropdownField(
        label = stringResource(R.string.field_condition),
        options = BookCondition.entries,
        selectedOption = selectedCondition,
        optionLabel = { stringResource(it.resourceId) },
        onOptionSelected = onConditionChange,
        modifier = modifier,
    )
}

@Composable
fun LanguageField(
    selectedLanguage: Language,
    onLanguageChange: (Language) -> Unit,
    modifier: Modifier = Modifier,
) {
    DropdownField(
        label = stringResource(R.string.field_language),
        options = Language.entries,
        selectedOption = selectedLanguage,
        optionLabel = { stringResource(it.resourceId) },
        onOptionSelected = onLanguageChange,
        modifier = modifier,
    )
}

@Composable
fun PublisherField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(stringResource(R.string.field_publisher)) },
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
    )
}

@Composable
fun PublicationYearField(
    value: String,
    onValueChange: (String) -> Unit,
    error: Int?,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(stringResource(R.string.field_publication_year)) },
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions =
            KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Next,
            ),
        isError = error != null,
        supportingText = {
            error?.let {
                Text(
                    text = stringResource(it),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
    )
}

@Composable
fun IsbnField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(stringResource(R.string.field_isbn)) },
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions =
            KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Next,
            ),
    )
}

@Composable
fun DescriptionField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(stringResource(R.string.field_description)) },
        modifier =
            modifier
                .fillMaxWidth()
                .height(FormDimensions.DescriptionHeight),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> DropdownField(
    label: String,
    options: List<T>,
    selectedOption: T,
    optionLabel: @Composable (T) -> String,
    onOptionSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = optionLabel(selectedOption),
            onValueChange = {},
            label = { Text(label) },
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true),
            singleLine = true,
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(optionLabel(option)) },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    },
                )
            }
        }
    }
}
