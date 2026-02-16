package hr.foi.air.honnomachi.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import hr.foi.air.honnomachi.R
import hr.foi.air.honnomachi.model.UserModel

/**
 * Ekran za prikaz liste korisnika u admin panel sekciji.
 *
 * Prikazuje korisnike u LazyColumn listi s podrškom za
 * infinite scroll straničenje, pull-to-refresh osvježavanje,
 * pretragu po imenu/emailu s debounce logikom i
 * filtriranje prema statusu računa (svi/aktivni/suspendirani).
 *
 * @param onNavigateBack Callback za povratak na prethodni ekran.
 * @param onNavigateToUserDetail Callback za navigaciju na detalje korisnika.
 * @param viewModel ViewModel za upravljanje listom korisnika.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminUserListScreen(
    onNavigateBack: () -> Unit,
    onNavigateToUserDetail: (String) -> Unit,
    viewModel: AdminUserListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    val isSearchOrFilterActive =
        uiState.searchQuery.isNotBlank() || uiState.selectedFilter != UserFilter.ALL

    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisible =
                listState.layoutInfo.visibleItemsInfo
                    .lastOrNull()
                    ?.index ?: 0
            lastVisible >= listState.layoutInfo.totalItemsCount - 3
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            viewModel.loadMoreUsers()
        }
    }

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
        Column(
            modifier =
                Modifier
                    .padding(paddingValues)
                    .fillMaxSize(),
        ) {
            SearchAndFilterSection(
                searchQuery = uiState.searchQuery,
                selectedFilter = uiState.selectedFilter,
                resultCount = if (isSearchOrFilterActive) uiState.users.size else null,
                onSearchQueryChanged = viewModel::onSearchQueryChanged,
                onFilterSelected = viewModel::onFilterSelected,
            )

            PullToRefreshBox(
                isRefreshing = uiState.isRefreshing,
                onRefresh = { viewModel.refreshUsers() },
                modifier = Modifier.fillMaxSize(),
            ) {
                when {
                    uiState.isLoading && uiState.users.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    uiState.errorMessage != null && uiState.users.isEmpty() -> {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text(
                                text = stringResource(R.string.admin_error_loading_users),
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center,
                            )
                            Spacer(modifier = Modifier.size(16.dp))
                            Button(onClick = { viewModel.loadUsers() }) {
                                Text(stringResource(R.string.admin_retry))
                            }
                        }
                    }
                    !uiState.isLoading && uiState.users.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = stringResource(R.string.admin_no_users),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                    }
                    else -> {
                        LazyColumn(
                            state = listState,
                            modifier =
                                Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(vertical = 8.dp),
                        ) {
                            items(uiState.users, key = { it.uid }) { user ->
                                UserListItem(
                                    user = user,
                                    onClick = { onNavigateToUserDetail(user.uid) },
                                )
                            }
                            if (uiState.isLoadingMore) {
                                item {
                                    Box(
                                        modifier =
                                            Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        CircularProgressIndicator(modifier = Modifier.size(32.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Sekcija s poljem za pretragu, filterima i brojem rezultata.
 *
 * @param searchQuery Trenutni tekst pretrage.
 * @param selectedFilter Odabrani filter statusa.
 * @param resultCount Broj rezultata za prikaz, ili null ako nije aktivan search/filter.
 * @param onSearchQueryChanged Callback pri promjeni teksta pretrage.
 * @param onFilterSelected Callback pri odabiru filtera.
 */
@Composable
private fun SearchAndFilterSection(
    searchQuery: String,
    selectedFilter: UserFilter,
    resultCount: Int?,
    onSearchQueryChanged: (String) -> Unit,
    onFilterSelected: (UserFilter) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChanged,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(stringResource(R.string.admin_search_hint)) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChanged("") }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = stringResource(R.string.admin_clear_search),
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
        )

        Spacer(modifier = Modifier.size(8.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = selectedFilter == UserFilter.ALL,
                onClick = { onFilterSelected(UserFilter.ALL) },
                label = { Text(stringResource(R.string.admin_filter_all)) },
            )
            FilterChip(
                selected = selectedFilter == UserFilter.ACTIVE,
                onClick = { onFilterSelected(UserFilter.ACTIVE) },
                label = { Text(stringResource(R.string.admin_filter_active)) },
            )
            FilterChip(
                selected = selectedFilter == UserFilter.SUSPENDED,
                onClick = { onFilterSelected(UserFilter.SUSPENDED) },
                label = { Text(stringResource(R.string.admin_filter_suspended)) },
            )
        }

        if (resultCount != null) {
            Spacer(modifier = Modifier.size(4.dp))
            Text(
                text = stringResource(R.string.admin_result_count, resultCount),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(modifier = Modifier.size(4.dp))
    }
}

/**
 * Stavka liste koja prikazuje osnovne podatke o korisniku.
 *
 * Sadrži krug s inicijalima, ime, email i statusnu oznaku.
 *
 * @param user Model korisnika za prikaz.
 * @param onClick Callback pri kliku na stavku.
 */
@Composable
private fun UserListItem(
    user: UserModel,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = user.name.take(2).uppercase(),
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = user.email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            StatusBadge(isSuspended = user.suspended == true)
        }
    }
}

/**
 * Oznaka statusa korisničkog računa.
 *
 * Prikazuje obojenu točku i tekst statusa
 * (zelena za aktivne, crvena za suspendirane).
 *
 * @param isSuspended Je li korisnik suspendiran.
 */
@Composable
private fun StatusBadge(isSuspended: Boolean) {
    val color = if (isSuspended) Color(0xFFE53935) else Color(0xFF43A047)
    val textRes = if (isSuspended) R.string.value_suspended else R.string.value_active

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier =
                Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color),
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = stringResource(textRes),
            style = MaterialTheme.typography.labelSmall,
            color = color,
        )
    }
}
