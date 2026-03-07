package hr.foi.air.honnomachi.ui.shelf

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import hr.foi.air.honnomachi.R
import hr.foi.air.honnomachi.ui.components.BookItemView

@Composable
fun ShelfPage(
    paddingValues: PaddingValues,
    @Suppress("DEPRECATION")
    viewModel: ShelfViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        PrimaryTabRow(selectedTabIndex = state.selectedTab.ordinal) {
            ShelfTab.entries.forEach { tab ->
                Tab(
                    selected = state.selectedTab == tab,
                    onClick = { viewModel.selectTab(tab) },
                    text = {
                        Text(text = when (tab) {
                            ShelfTab.PURCHASED -> stringResource(R.string.shelf_purchased)
                            ShelfTab.SOLD -> stringResource(R.string.shelf_sold)
                        })
                    }
                )
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (state.errorMessage != null) {
                Text(
                    text = state.errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center).padding(16.dp)
                )
            } else {
                if (state.books.isEmpty()) {
                    val emptyMsg = if (state.selectedTab == ShelfTab.PURCHASED)
                        stringResource(R.string.shelf_no_purchased)
                    else stringResource(R.string.shelf_no_sold)

                    Text(text = emptyMsg, modifier = Modifier.align(Alignment.Center))
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(state.books) { book ->
                            BookItemView(book = book, onBookClick = { /* Navigacija */ })
                        }
                    }
                }
            }
        }
    }
}