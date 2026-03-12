package hr.foi.air.honnomachi.ui.cart

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import hr.foi.air.honnomachi.R

@Composable
fun CheckoutSuccessScreen(onNavigateHome: () -> Unit) {
    CheckoutResultScreenContent(
        icon = {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 4.dp),
            )
        },
        title = stringResource(R.string.checkout_success_title),
        description = stringResource(R.string.checkout_success_message),
        onNavigateHome = onNavigateHome,
    )
}

@Composable
fun CheckoutCancelScreen(onNavigateHome: () -> Unit) {
    CheckoutResultScreenContent(
        icon = {
            Icon(
                imageVector = Icons.Default.Cancel,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = 4.dp),
            )
        },
        title = stringResource(R.string.checkout_cancel_title),
        description = stringResource(R.string.checkout_cancel_message),
        onNavigateHome = onNavigateHome,
    )
}

@Composable
private fun CheckoutResultScreenContent(
    icon: @Composable () -> Unit,
    title: String,
    description: String,
    onNavigateHome: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        icon()
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onNavigateHome,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = stringResource(R.string.button_back_home))
        }
    }
}
