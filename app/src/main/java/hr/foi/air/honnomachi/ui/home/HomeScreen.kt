package hr.foi.air.honnomachi.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import hr.foi.air.honnomachi.R
import hr.foi.air.honnomachi.ui.add.AddPage
import hr.foi.air.honnomachi.ui.auth.AuthViewModel
import hr.foi.air.honnomachi.ui.cart.CartPage
import hr.foi.air.honnomachi.ui.cart.CartUiState
import hr.foi.air.honnomachi.ui.cart.CartViewModel
import hr.foi.air.honnomachi.ui.profile.ProfileScreen
import hr.foi.air.honnomachi.ui.profile.ProfileUiState
import hr.foi.air.honnomachi.ui.profile.ProfileViewModel
import hr.foi.air.honnomachi.ui.shelf.ShelfPage
import hr.foi.air.honnomachi.ui.suspension.LocalIsSuspended
import hr.foi.air.honnomachi.ui.suspension.LocalSuspendedReason
import hr.foi.air.honnomachi.ui.theme.StatusSuspended

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    authViewModel: AuthViewModel,
    homeViewModel: HomeViewModel,
    @Suppress("DEPRECATION")
    profileViewModel: ProfileViewModel = hiltViewModel(),
    @Suppress("DEPRECATION")
    cartViewModel: CartViewModel = hiltViewModel(),
) {
    val isSuspended = LocalIsSuspended.current
    val suspendedReason = LocalSuspendedReason.current
    val profileUiState by profileViewModel.uiState.collectAsState()
    val cartUiState by cartViewModel.uiState.collectAsState()

    var wasSuspended by rememberSaveable { mutableStateOf(isSuspended) }
    var showSuspensionDialog by rememberSaveable { mutableStateOf(isSuspended) }
    LaunchedEffect(isSuspended) {
        if (isSuspended && !wasSuspended) {
            cartViewModel.clearCart()
            showSuspensionDialog = true
        }
        wasSuspended = isSuspended
    }

    if (showSuspensionDialog) {
        AlertDialog(
            onDismissRequest = { showSuspensionDialog = false },
            title = { Text(text = stringResource(R.string.suspended_title)) },
            text = {
                Column {
                    if (!suspendedReason.isNullOrBlank()) {
                        Text(text = stringResource(R.string.suspended_reason_label, suspendedReason))
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    Text(text = stringResource(R.string.suspended_contact_message))
                }
            },
            confirmButton = {
                TextButton(onClick = { showSuspensionDialog = false }) {
                    Text(text = stringResource(android.R.string.ok))
                }
            },
        )
    }

    val profileIcon =
        if (profileUiState is ProfileUiState.Success && (profileUiState as ProfileUiState.Success).user.admin == true) {
            Icons.Default.ManageAccounts
        } else {
            Icons.Default.Person
        }

    val navItemList =
        listOf(
            NavItem(label = stringResource(R.string.home), icon = Icons.AutoMirrored.Outlined.MenuBook),
            NavItem(label = stringResource(R.string.shelf), icon = Icons.AutoMirrored.Filled.LibraryBooks),
            NavItem(label = stringResource(R.string.add), icon = Icons.Default.AddCircle),
            NavItem(label = stringResource(R.string.cart), icon = Icons.Default.ShoppingCart),
            NavItem(label = stringResource(R.string.profile), icon = profileIcon),
        )

    var selectedIndex by rememberSaveable { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                expandedHeight = 48.dp,
                title = {
                    Text(text = navItemList[selectedIndex].label)
                },
                actions = {
                    TextButton(
                        onClick = {
                            navController.navigate(ROUTE_WALLET) {
                                launchSingleTop = true
                            }
                        },
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AccountBalanceWallet,
                                contentDescription = stringResource(R.string.wallet_title),
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = stringResource(R.string.wallet_title))
                        }
                    }
                },
            )
        },
        bottomBar = {
            NavigationBar {
                navItemList.forEachIndexed { index, navItem ->
                    val badgeCount =
                        if (index == 3 && cartUiState is CartUiState.Success) {
                            (cartUiState as CartUiState.Success).items.size
                        } else {
                            0
                        }

                    NavigationBarItem(
                        selected = index == selectedIndex,
                        onClick = { selectedIndex = index },
                        label = { Text(text = navItem.label) },
                        icon = {
                            if (badgeCount > 0) {
                                BadgedBox(
                                    badge = {
                                        Badge {
                                            Text(text = badgeCount.toString())
                                        }
                                    },
                                ) {
                                    Icon(
                                        imageVector = navItem.icon,
                                        contentDescription = navItem.label,
                                    )
                                }
                            } else {
                                Icon(imageVector = navItem.icon, contentDescription = navItem.label)
                            }
                        },
                    )
                }
            }
        },
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            if (isSuspended) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .background(StatusSuspended),
                )
            }
            ContentScreen(
                paddingValues = PaddingValues(0.dp),
                selectedIndex,
                navController,
                authViewModel,
                homeViewModel,
                profileViewModel,
                cartViewModel,
            )
        }
    }
}

@Composable
fun ContentScreen(
    paddingValues: PaddingValues,
    selectedIndex: Int,
    navController: NavController,
    authViewModel: AuthViewModel,
    homeViewModel: HomeViewModel,
    profileViewModel: ProfileViewModel,
    cartViewModel: CartViewModel,
) {
    when (selectedIndex) {
        0 -> {
            HomePage(
                paddingValues = paddingValues,
                navController = navController,
                viewModel = homeViewModel,
            )
        }

        1 -> {
            ShelfPage(paddingValues = paddingValues)
        }

        2 -> {
            AddPage(paddingValues = paddingValues)
        }

        3 -> {
            CartPage(
                paddingValues = paddingValues,
                viewModel = cartViewModel,
                onCheckoutSuccess = {
                    navController.navigate(ROUTE_CHECKOUT_SUCCESS) {
                        launchSingleTop = true
                    }
                },
                onCheckoutCancel = {
                    navController.navigate(ROUTE_CHECKOUT_CANCEL) {
                        launchSingleTop = true
                    }
                },
            )
        }

        4 -> {
            ProfileScreen(
                paddingValues = paddingValues,
                onLogout = {
                    authViewModel.signOut()
                    navController.navigate("auth") {
                        popUpTo("home") { inclusive = true }
                    }
                },
                onNavigateToChangePassword = {
                    navController.navigate("changePassword")
                },
                onNavigateToPrivacyPolicy = {
                    navController.navigate("privacyPolicy")
                },
                onNavigateToAdmin = {
                    navController.navigate("admin")
                },
                profileViewModel = profileViewModel,
            )
        }
    }
}

data class NavItem(
    val label: String,
    val icon: ImageVector,
)

private const val ROUTE_CHECKOUT_SUCCESS = "checkout/success"
private const val ROUTE_CHECKOUT_CANCEL = "checkout/cancel"
private const val ROUTE_WALLET = "wallet"
