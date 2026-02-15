package hr.foi.air.honnomachi.ui.admin

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import hr.foi.air.honnomachi.R

/**
 * Ekran za detaljan prikaz podataka o korisniku.
 *
 * Trenutno prikazuje samo poruku "Dolazi uskoro" s navigacijskom trakom.
 * Bit će u potpunosti implementiran u HNM-293 s prikazom svih
 * korisničkih podataka (ime, email, telefon, adresa, status računa).
 *
 * @param userId Jedinstveni identifikator korisnika čiji se podaci prikazuju.
 * @param onNavigateBack Callback za povratak na listu korisnika.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminUserDetailScreen(
    userId: String?,
    onNavigateBack: () -> Unit,
) {
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
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.admin_coming_soon),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}
