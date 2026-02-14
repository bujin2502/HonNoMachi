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
 * Placeholder ekran za prikaz liste korisnika u admin sekciji.
 *
 * Trenutno prikazuje samo poruku "Dolazi uskoro" s navigacijskom trakom.
 * Bit će u potpunosti implementiran u HNM-291 s LazyColumn listom,
 * infinite scroll straničenjem i prikazom korisničkih podataka.
 *
 * @param onNavigateBack Callback za povratak na prethodni ekran.
 * @param onNavigateToUserDetail Callback za navigaciju na detalje korisnika,
 *        prima [userId] kao argument.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminUserListScreen(
    onNavigateBack: () -> Unit,
    onNavigateToUserDetail: (String) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_admin_user_list)) },
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
