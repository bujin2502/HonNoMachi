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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
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
    val snackbarHostState = remember { SnackbarHostState() }

    val suspendSuccessMsg = stringResource(R.string.admin_suspend_success)
    val suspendErrorMsg = stringResource(R.string.admin_suspend_error)
    val reactivateSuccessMsg = stringResource(R.string.admin_reactivate_success)
    val reactivateErrorMsg = stringResource(R.string.admin_reactivate_error)

    if (uiState is AdminUserDetailUiState.Success) {
        val successState = uiState as AdminUserDetailUiState.Success

        successState.actionMessage?.let { message ->
            LaunchedEffect(message) {
                snackbarHostState.showSnackbar(message)
                viewModel.consumeActionMessage()
            }
        }

        if (successState.showSuspendDialog) {
            SuspensionDialog(
                userName = successState.user.name,
                isLoading = successState.isActionLoading,
                onConfirm = { reason ->
                    viewModel.confirmSuspend(reason, suspendSuccessMsg, suspendErrorMsg)
                },
                onDismiss = viewModel::dismissDialog,
            )
        }

        if (successState.showReactivateDialog) {
            ReactivationDialog(
                userName = successState.user.name,
                isLoading = successState.isActionLoading,
                onConfirm = {
                    viewModel.confirmReactivate(reactivateSuccessMsg, reactivateErrorMsg)
                },
                onDismiss = viewModel::dismissDialog,
            )
        }
    }

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
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
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
                    UserDetailContent(
                        user = state.user,
                        isActionLoading = state.isActionLoading,
                        onSuspendClick = viewModel::onSuspendClick,
                        onReactivateClick = viewModel::onReactivateClick,
                    )
                }
            }
        }
    }
}

/**
 * Sadržaj ekrana s detaljima korisnika.
 *
 * Scrollable column s header sekcijom, karticama za račun, kontakt i adresu,
 * te gumbima za suspenziju ili reaktivaciju korisnika.
 *
 * @param user Model korisnika za prikaz.
 * @param isActionLoading Izvršava li se trenutno akcija suspenzije/reaktivacije.
 * @param onSuspendClick Callback za otvaranje dijaloga suspenzije.
 * @param onReactivateClick Callback za otvaranje dijaloga reaktivacije.
 */
@Composable
private fun UserDetailContent(
    user: UserModel,
    isActionLoading: Boolean,
    onSuspendClick: () -> Unit,
    onReactivateClick: () -> Unit,
) {
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

        if (user.suspended == true) {
            Spacer(modifier = Modifier.height(12.dp))

            DetailSection(title = stringResource(R.string.admin_section_suspension)) {
                user.suspendedReason?.let { reason ->
                    DetailRow(
                        label = stringResource(R.string.admin_label_suspended_reason),
                        value = reason,
                    )
                }
                user.suspendedAt?.let { timestamp ->
                    DetailRow(
                        label = stringResource(R.string.admin_label_suspended_at),
                        value =
                            java.text
                                .SimpleDateFormat(
                                    "dd.MM.yyyy. HH:mm",
                                    java.util.Locale.getDefault(),
                                ).format(timestamp.toDate()),
                    )
                }
            }
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

        if (user.admin != true) {
            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(24.dp))

            if (user.suspended == true) {
                Button(
                    onClick = onReactivateClick,
                    enabled = !isActionLoading,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = StatusActive),
                ) {
                    if (isActionLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(24.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = stringResource(R.string.admin_reactivate_user),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                    )
                }
            } else {
                Button(
                    onClick = onSuspendClick,
                    enabled = !isActionLoading,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = StatusSuspended),
                ) {
                    if (isActionLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(24.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = stringResource(R.string.admin_suspend_user),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
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
    valueColor: Color? = null,
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
