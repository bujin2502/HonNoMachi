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
import androidx.compose.foundation.lazy.LazyListState
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
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
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import hr.foi.air.honnomachi.R
import hr.foi.air.honnomachi.model.UserModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminUserListScreen(
    onNavigateBack: () -> Unit,
    onNavigateToUserDetail: (String) -> Unit,
    viewModel: AdminUserListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    LifecycleResumeEffect(Unit) {
        viewModel.refreshUsers()
        onPauseOrDispose {}
    }

    val isSearchOrFilterActive =
        uiState.searchQuery.isNotBlank() || uiState.selectedFilter != UserFilter.ALL

    LaunchedEffect(uiState.scrollToTopTrigger) {
        listState.scrollToItem(0)
    }

    LaunchedEffect(listState) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisibleIndex =
                layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            lastVisibleIndex to totalItems
        }.collect { (lastVisible, total) ->
            if (total > 0 && lastVisible >= total - 3) {
                viewModel.loadMoreUsers()
            }
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
                UserListContent(
                    uiState = uiState,
                    listState = listState,
                    onNavigateToUserDetail = onNavigateToUserDetail,
                    onRetry = viewModel::loadUsers,
                )
            }
        }
    }
}

@Composable
private fun UserListContent(
    uiState: AdminUserListUiState,
    listState: LazyListState,
    onNavigateToUserDetail: (String) -> Unit,
    onRetry: () -> Unit,
) {
    when {
        uiState.isLoading && uiState.users.isEmpty() -> LoadingContent()
        uiState.errorMessage != null && uiState.users.isEmpty() -> ErrorContent(onRetry = onRetry)
        !uiState.isLoading && uiState.users.isEmpty() -> EmptyUsersContent()
        else ->
            UserListWithPagination(
                uiState = uiState,
                listState = listState,
                onNavigateToUserDetail = onNavigateToUserDetail,
            )
    }
}

@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorContent(onRetry: () -> Unit) {
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
        Button(onClick = onRetry) {
            Text(stringResource(R.string.admin_retry))
        }
    }
}

@Composable
private fun EmptyUsersContent() {
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

@Composable
private fun UserListWithPagination(
    uiState: AdminUserListUiState,
    listState: LazyListState,
    onNavigateToUserDetail: (String) -> Unit,
) {
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
