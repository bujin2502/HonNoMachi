package hr.foi.air.honnomachi.ui.cart

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.util.Locale
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.rememberPaymentSheet
import hr.foi.air.honnomachi.AppUtil
import hr.foi.air.honnomachi.R
import hr.foi.air.honnomachi.model.CartItemModel
import kotlinx.coroutines.flow.collect

@Composable
fun CartPage(
    paddingValues: PaddingValues,
    viewModel: CartViewModel = hiltViewModel(),
    showImages: Boolean = true,
    onCheckoutSuccess: () -> Unit = {},
    onCheckoutCancel: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()
    val actionMessage by viewModel.actionMessage.collectAsState()
    val isCheckoutInProgress by viewModel.isCheckoutInProgress.collectAsState()
    val secondsUntilExpiry by viewModel.secondsUntilExpiry.collectAsState()
    val pendingCheckout by viewModel.pendingCheckout.collectAsState()
    val context = LocalContext.current

    val paymentSheet = rememberPaymentSheet { result ->
        viewModel.onPaymentSheetResult(result)
    }

    LaunchedEffect(actionMessage) {
        actionMessage?.let {
            AppUtil.showToast(context, it)
            viewModel.consumeMessage()
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.paymentSheetRequests.collect { request ->
            val clientSecret = request.clientSecret
            if (clientSecret.isNullOrBlank()) {
                viewModel.onPaymentSheetPresentationFailed(
                    IllegalStateException("Missing Stripe PaymentIntent client secret."),
                )
                return@collect
            }

            runCatching {
                paymentSheet.presentWithPaymentIntent(
                    clientSecret,
                    PaymentSheet.Configuration(
                        merchantDisplayName = "HonNoMachi",
                    ),
                )
            }.onFailure { error ->
                viewModel.onPaymentSheetPresentationFailed(error)
            }
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.checkoutNavigation.collect { target ->
            when (target) {
                CheckoutNavigationTarget.SUCCESS -> onCheckoutSuccess()
                CheckoutNavigationTarget.CANCELED -> onCheckoutCancel()
            }
        }
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
    ) {
        Text(
            text = stringResource(R.string.cart),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 16.dp),
        )

        when (val state = uiState) {
            is CartUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is CartUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = "Greška: ${state.message}", color = MaterialTheme.colorScheme.error)
                }
            }
            is CartUiState.Success -> {
                if (state.items.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = "Košarica je prazna.")
                    }
                } else {
                    CartContent(
                        items = state.items,
                        totalPrice = state.totalPrice,
                        isCheckoutInProgress = isCheckoutInProgress,
                        onRemoveItem = { viewModel.removeFromCart(it) },
                        onCheckout = { viewModel.checkoutWithStripe() },
                        showImages = showImages,
                        ticker = secondsUntilExpiry,
                    )
                }
            }
        }
    }

    pendingCheckout?.let { checkout ->
        CheckoutConfirmationDialog(
            checkout = checkout,
            onConfirm = { viewModel.confirmCheckoutPayment() },
            onDismiss = { viewModel.dismissCheckoutDialog() },
        )
    }
}

@Composable
fun CartContent(
    items: List<CartItemModel>,
    totalPrice: Double,
    isCheckoutInProgress: Boolean,
    onRemoveItem: (String) -> Unit,
    onCheckout: () -> Unit,
    showImages: Boolean,
    ticker: Long? = null,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        ticker?.let { seconds ->
            if (seconds > 0) {
                val minutes = seconds / 60
                val secs = seconds % 60
                val label = String.format(Locale.getDefault(), "%02d:%02d", minutes, secs)
                val chipColor = when {
                    seconds > 300 -> Color(0xFF4CAF50)
                    seconds > 60  -> Color(0xFFFF9800)
                    else          -> Color(0xFFF44336)
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = chipColor.copy(alpha = 0.15f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Rezervacija istječe za $label",
                            style = MaterialTheme.typography.labelLarge,
                            color = chipColor,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(items) { item ->
                CartItemRow(item = item, onRemove = onRemoveItem, showImages = showImages)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider()
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Ukupno:",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = String.format(java.util.Locale.getDefault(), "%.2f EUR", totalPrice),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = onCheckout,
            enabled = !isCheckoutInProgress,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .testTag("pay_with_stripe_button"),
            shape = RoundedCornerShape(8.dp),
        ) {
            if (isCheckoutInProgress) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                )
            } else {
                Text(text = stringResource(R.string.button_pay))
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun CheckoutConfirmationDialog(
    checkout: hr.foi.air.honnomachi.data.CheckoutPaymentIntentModel,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val totalText = formatMinorAmount(checkout.totalAmountMinor, checkout.currency)
    val walletText = formatMinorAmount(checkout.walletContributionMinor, checkout.currency)
    val stripeText = formatMinorAmount(checkout.amountMinor, checkout.currency)

    val infoMessage =
        when {
            checkout.checkoutCompleted && !checkout.requiresPaymentSheet ->
                stringResource(R.string.checkout_dialog_message_wallet_complete)
            checkout.walletContributionMinor > 0 && checkout.amountMinor > 0 ->
                stringResource(
                    R.string.checkout_dialog_message_split_payment,
                    walletText,
                    stripeText,
                )
            checkout.requiresPaymentSheet ->
                stringResource(R.string.checkout_dialog_message_stripe_only, stripeText)
            else ->
                stringResource(R.string.checkout_dialog_message_wallet_only)
        }

    val confirmText =
        when {
            checkout.checkoutCompleted && !checkout.requiresPaymentSheet ->
                stringResource(R.string.checkout_dialog_continue)
            checkout.requiresPaymentSheet && checkout.walletContributionMinor > 0 ->
                stringResource(R.string.checkout_dialog_confirm_split, stripeText)
            checkout.requiresPaymentSheet ->
                stringResource(R.string.checkout_dialog_confirm_stripe, stripeText)
            else ->
                stringResource(R.string.checkout_dialog_confirm_wallet)
        }

    AlertDialog(
        onDismissRequest = {
            if (checkout.requiresPaymentSheet && !checkout.checkoutCompleted) {
                onDismiss()
            }
        },
        title = { Text(text = stringResource(R.string.checkout_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = infoMessage)
                Text(text = stringResource(R.string.checkout_dialog_total_line, totalText))
                Text(text = stringResource(R.string.checkout_dialog_wallet_line, walletText))
                if (checkout.amountMinor > 0) {
                    Text(text = stringResource(R.string.checkout_dialog_stripe_line, stripeText))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = confirmText)
            }
        },
        dismissButton = {
            if (checkout.requiresPaymentSheet && !checkout.checkoutCompleted) {
                TextButton(onClick = onDismiss) {
                    Text(text = stringResource(R.string.checkout_dialog_cancel))
                }
            }
        },
    )
}

private fun formatMinorAmount(amountMinor: Int, currency: String): String {
    val majorAmount = amountMinor / 100.0
    return String.format(Locale.getDefault(), "%.2f %s", majorAmount, currency.uppercase(Locale.getDefault()))
}

@Composable
fun CartItemRow(
    item: CartItemModel,
    onRemove: (String) -> Unit,
    showImages: Boolean,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (showImages) {
                AsyncImage(
                    model = item.imageUrl,
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier =
                        Modifier
                            .size(80.dp)
                            .padding(8.dp),
                    error = painterResource(R.drawable.baseline_broken_image_24),
                    placeholder = painterResource(R.drawable.baseline_change_circle_24),
                )
            } else {
                Spacer(Modifier.size(80.dp))
            }

            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                )
                Text(
                    text = item.author,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    maxLines = 1,
                )
                Text(
                    text = "${item.price} ${item.currency}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                )
            }

            IconButton(onClick = { onRemove(item.id) }) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Remove",
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}
