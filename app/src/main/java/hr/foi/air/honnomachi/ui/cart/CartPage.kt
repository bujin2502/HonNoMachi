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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import hr.foi.air.honnomachi.AppUtil
import hr.foi.air.honnomachi.R
import hr.foi.air.honnomachi.model.CartItemModel

@Composable
fun CartPage(
    paddingValues: PaddingValues,
    viewModel: CartViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val actionMessage by viewModel.actionMessage.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(actionMessage) {
        actionMessage?.let {
            AppUtil.showToast(context, it)
            viewModel.consumeMessage()
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
                        onRemoveItem = { viewModel.removeFromCart(it) },
                    )
                }
            }
        }
    }
}

@Composable
fun CartContent(
    items: List<CartItemModel>,
    totalPrice: Double,
    onRemoveItem: (String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(items) { item ->
                CartItemRow(item = item, onRemove = onRemoveItem)
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
                text = String.format("%.2f EUR", totalPrice),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
fun CartItemRow(
    item: CartItemModel,
    onRemove: (String) -> Unit,
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(100.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
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