package hr.foi.air.honnomachi.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import hr.foi.air.honnomachi.R
import hr.foi.air.honnomachi.model.UserModel
import hr.foi.air.honnomachi.ui.theme.LabelGray
import hr.foi.air.honnomachi.ui.theme.StatusActive
import hr.foi.air.honnomachi.ui.theme.StatusSuspended

/**
 * Ekran za detaljan prikaz podataka o korisniku.
 *
 * Prikazuje sve dostupne podatke korisnika podijeljene u sekcije:
 * header s inicijalima, račun (uloga, status, verifikacija),
 * kontakt (email, telefon) i adresa (ulica, grad, poštanski broj).
 *
 * @param onNavigateBack Callback za povratak na listu korisnika.
 * @param viewModel ViewModel za dohvat podataka korisnika.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminUserDetailScreen(
    onNavigateBack: () -> Unit,
    viewModel: AdminUserDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_admin_user_detail)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.desc_navigate_back),
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        Box(
            modifier =
                Modifier
                    .padding(paddingValues)
                    .fillMaxSize(),
        ) {
            when (val state = uiState) {
                is AdminUserDetailUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is AdminUserDetailUiState.Error -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.admin_error_loading_user),
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(modifier = Modifier.size(16.dp))
                        Button(onClick = { viewModel.loadUser() }) {
                            Text(stringResource(R.string.admin_retry))
                        }
                    }
                }
                is AdminUserDetailUiState.Success -> {
                    UserDetailContent(user = state.user)
                }
            }
        }
    }
}

/**
 * Sadržaj ekrana s detaljima korisnika.
 *
 * Scrollable column s header sekcijom i karticama za
 * račun, kontakt i adresu.
 *
 * @param user Model korisnika za prikaz.
 */
@Composable
private fun UserDetailContent(user: UserModel) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier =
                Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = user.name.take(2).uppercase(),
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = user.name,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = user.email,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(24.dp))

        DetailSection(title = stringResource(R.string.admin_section_account)) {
            val roleText =
                if (user.admin == true) {
                    stringResource(R.string.value_admin)
                } else {
                    stringResource(R.string.value_user)
                }
            DetailRow(label = stringResource(R.string.label_role), value = roleText)

            val isSuspended = user.suspended == true
            val statusText =
                if (isSuspended) stringResource(R.string.value_suspended) else stringResource(R.string.value_active)
            val statusColor = if (isSuspended) StatusSuspended else StatusActive
            DetailRow(
                label = stringResource(R.string.label_status),
                value = statusText,
                valueColor = statusColor,
            )

            val verifiedText =
                if (user.isVerified) {
                    stringResource(R.string.admin_value_yes)
                } else {
                    stringResource(R.string.admin_value_no)
                }
            DetailRow(label = stringResource(R.string.admin_label_verified), value = verifiedText)
        }

        Spacer(modifier = Modifier.height(12.dp))

        DetailSection(title = stringResource(R.string.admin_section_contact)) {
            DetailRow(label = stringResource(R.string.label_email), value = user.email)
            DetailRow(
                label = stringResource(R.string.label_phone),
                value = user.phoneNumber ?: "—",
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        DetailSection(title = stringResource(R.string.admin_section_address)) {
            DetailRow(
                label = stringResource(R.string.label_street),
                value = user.street ?: "—",
            )
            DetailRow(
                label = stringResource(R.string.label_city),
                value = user.city ?: "—",
            )
            DetailRow(
                label = stringResource(R.string.label_zip),
                value = user.postNumber ?: "—",
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

/**
 * Sekcija s naslovom i sadržajem unutar kartice.
 *
 * @param title Naslov sekcije prikazan iznad kartice.
 * @param content Sadržaj sekcije unutar kartice.
 */
@Composable
private fun DetailSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                content()
            }
        }
    }
}

/**
 * Red s labelom i vrijednošću za prikaz pojedinog podatka.
 *
 * @param label Naziv podatka (npr. "Uloga:").
 * @param value Vrijednost podatka (npr. "Administrator").
 * @param valueColor Boja teksta vrijednosti, ili null za zadanu boju.
 */
@Composable
private fun DetailRow(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color? = null,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = LabelGray,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = valueColor ?: MaterialTheme.colorScheme.onSurface,
        )
    }
}
