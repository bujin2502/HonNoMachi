package hr.foi.air.honnomachi.ui.wallet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.rememberPaymentSheet
import hr.foi.air.honnomachi.AppUtil
import hr.foi.air.honnomachi.R
import hr.foi.air.honnomachi.data.WalletHistoryItemModel
import hr.foi.air.honnomachi.data.WalletHistoryType
import hr.foi.air.honnomachi.data.WalletTopupStatus
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

private const val MIN_TOPUP_AMOUNT_MINOR = 100
private const val MAX_TOPUP_AMOUNT_MINOR = 100000
private val PREDEFINED_TOPUP_AMOUNTS_MINOR = listOf(500, 1000, 2000)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletScreen(
    onNavigateBack: () -> Unit,
    viewModel: WalletViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val actionMessage by viewModel.actionMessage.collectAsState()

    var customAmountInput by rememberSaveable { mutableStateOf("") }
    var customAmountError by rememberSaveable { mutableStateOf<String?>(null) }

    val paymentSheet = rememberPaymentSheet { result ->
        viewModel.onPaymentSheetResult(result)
    }

    LaunchedEffect(viewModel) {
        viewModel.paymentSheetRequests.collect { request ->
            runCatching {
                paymentSheet.presentWithPaymentIntent(
                    request.clientSecret,
                    PaymentSheet.Configuration(
                        merchantDisplayName = "HonNoMachi",
                    ),
                )
            }.onFailure { error ->
                viewModel.onPaymentSheetPresentationFailed(error)
            }
        }
    }

    LaunchedEffect(actionMessage) {
        actionMessage?.let {
            AppUtil.showToast(context, it)
            viewModel.consumeMessage()
        }
    }

    val currencyCode = uiState.currency.uppercase(Locale.ROOT)
    val balanceText = formatAmount(uiState.balanceMinor, uiState.currency, includeSign = false)
    val selectedAmountText =
        formatAmount(
            amountMinor = uiState.selectedTopupAmountMinor,
            currency = uiState.currency,
            includeSign = false,
        )
    val customAmountInvalidText = stringResource(R.string.wallet_custom_amount_invalid)
    val customAmountRangeText =
        stringResource(
            R.string.wallet_custom_amount_range,
            MIN_TOPUP_AMOUNT_MINOR / 100.0,
            MAX_TOPUP_AMOUNT_MINOR / 100.0,
        )

    val isBusy = uiState.isCreatingTopup || uiState.activeTopupId != null

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.wallet_title),
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.desc_navigate_back),
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding =
                PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = paddingValues.calculateTopPadding(),
                    bottom = paddingValues.calculateBottomPadding() + 16.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.wallet_balance, balanceText),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            item {
                Text(
                    text = stringResource(R.string.wallet_topup_select_amount),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    PREDEFINED_TOPUP_AMOUNTS_MINOR.forEach { amountMinor ->
                        val isSelected =
                            customAmountInput.isBlank() && amountMinor == uiState.selectedTopupAmountMinor
                        val label =
                            String.format(
                                Locale.getDefault(),
                                "%.2f %s",
                                amountMinor / 100.0,
                                currencyCode,
                            )
                        if (isSelected) {
                            Button(
                                onClick = {
                                    customAmountInput = ""
                                    customAmountError = null
                                    viewModel.selectTopupAmount(amountMinor)
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(16.dp),
                            ) {
                                Text(text = label)
                            }
                        } else {
                            OutlinedButton(
                                onClick = {
                                    customAmountInput = ""
                                    customAmountError = null
                                    viewModel.selectTopupAmount(amountMinor)
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(16.dp),
                            ) {
                                Text(text = label)
                            }
                        }
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = customAmountInput,
                    onValueChange = {
                        customAmountInput = it
                        customAmountError = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(text = stringResource(R.string.wallet_custom_amount_label)) },
                    placeholder = { Text(text = stringResource(R.string.wallet_custom_amount_placeholder)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    isError = customAmountError != null,
                )
                if (customAmountError != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = customAmountError.orEmpty(),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            item {
                if (uiState.activeTopupStatus == WalletTopupStatus.PENDING || uiState.isCreatingTopup) {
                    Text(
                        text = stringResource(R.string.wallet_topup_pending_status),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                Button(
                    onClick = {
                        val customMinor = customAmountInput.toMinorAmount()
                        if (customAmountInput.isNotBlank()) {
                            if (customMinor == null) {
                                customAmountError = customAmountInvalidText
                                return@Button
                            }
                            if (customMinor !in MIN_TOPUP_AMOUNT_MINOR..MAX_TOPUP_AMOUNT_MINOR) {
                                customAmountError = customAmountRangeText
                                return@Button
                            }
                            viewModel.selectTopupAmount(customMinor)
                        }
                        customAmountError = null
                        viewModel.startTopup()
                    },
                    enabled = !isBusy,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                ) {
                    if (uiState.isCreatingTopup) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        val ctaAmount =
                            if (customAmountInput.isNotBlank() && customAmountInput.toMinorAmount() != null) {
                                formatAmount(
                                    amountMinor = customAmountInput.toMinorAmount() ?: uiState.selectedTopupAmountMinor,
                                    currency = uiState.currency,
                                    includeSign = false,
                                )
                            } else {
                                selectedAmountText
                            }
                        Text(text = stringResource(R.string.wallet_add_funds_cta, ctaAmount))
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.wallet_history_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }

            if (uiState.isHistoryLoading && uiState.history.isEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
            } else if (uiState.history.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.wallet_history_empty),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            } else {
                items(uiState.history, key = { it.id }) { entry ->
                    WalletHistoryRow(entry = entry)
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun WalletHistoryRow(entry: WalletHistoryItemModel) {
    val title =
        when (entry.type) {
            WalletHistoryType.TOPUP -> stringResource(R.string.wallet_history_type_topup)
            WalletHistoryType.REFUND -> stringResource(R.string.wallet_history_type_refund)
            WalletHistoryType.PURCHASE -> stringResource(R.string.wallet_history_type_purchase)
            WalletHistoryType.SALE -> stringResource(R.string.wallet_history_type_sale)
        }
    val timestampText =
        entry.createdAtEpochMillis?.let { createdAt ->
            DateFormat
                .getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, Locale.getDefault())
                .format(Date(createdAt))
        } ?: "-"
    val amountText = formatAmount(entry.amountMinor, entry.currency, includeSign = true)
    val amountColor =
        if (entry.amountMinor >= 0) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.error
        }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = timestampText,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = amountText,
            color = amountColor,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.End,
        )
    }
}

private fun formatAmount(
    amountMinor: Int,
    currency: String,
    includeSign: Boolean,
): String {
    val absoluteMinor = abs(amountMinor)
    val sign =
        if (!includeSign) {
            ""
        } else if (amountMinor >= 0) {
            "+"
        } else {
            "-"
        }
    return String.format(
        Locale.getDefault(),
        "%s%.2f %s",
        sign,
        absoluteMinor / 100.0,
        currency.uppercase(Locale.ROOT),
    )
}

private fun String.toMinorAmount(): Int? {
    val normalized = trim().replace(",", ".")
    if (normalized.isBlank()) {
        return null
    }
    val value = normalized.toDoubleOrNull() ?: return null
    if (value <= 0) {
        return null
    }
    return (value * 100).roundToInt()
}
