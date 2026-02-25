package hr.foi.air.honnomachi.ui.admin

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import hr.foi.air.honnomachi.R

/**
 * Dijalog za potvrdu suspenzije korisnika.
 *
 * Sadrži polje za unos razloga suspenzije i gumbe za potvrdu ili odustajanje.
 * Gumb za potvrdu je onemogućen dok razlog nije unesen ili dok traje akcija.
 *
 * @param userName Ime korisnika koji se suspendira.
 * @param isLoading Izvršava li se trenutno akcija suspenzije.
 * @param onConfirm Callback s unesenim razlogom nakon potvrde.
 * @param onDismiss Callback za zatvaranje dijaloga.
 */
@Composable
fun SuspensionDialog(
    userName: String,
    isLoading: Boolean,
    onConfirm: (reason: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var reason by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = {
            Text(
                text = stringResource(R.string.admin_suspend_dialog_title),
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column {
                Text(
                    text = userName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text(stringResource(R.string.admin_suspend_dialog_reason_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                    minLines = 2,
                    maxLines = 4,
                    supportingText = {
                        if (reason.isBlank()) {
                            Text(stringResource(R.string.admin_suspend_dialog_reason_required))
                        }
                    },
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(reason.trim()) },
                enabled = reason.isNotBlank() && !isLoading,
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                } else {
                    Text(
                        text = stringResource(R.string.admin_suspend_user),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading,
            ) {
                Text(stringResource(R.string.admin_dialog_cancel))
            }
        },
    )
}
