package hr.foi.air.honnomachi.screen

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import hr.foi.air.honnomachi.R
import hr.foi.air.honnomachi.pages.AddPage
import hr.foi.air.honnomachi.pages.CartPage
import hr.foi.air.honnomachi.pages.HomePage
import hr.foi.air.honnomachi.pages.ProfilePage
import hr.foi.air.honnomachi.pages.ShelfPage
import hr.foi.air.honnomachi.viewmodel.AuthViewModel
import hr.foi.air.honnomachi.viewmodel.HomeViewModel
import hr.foi.air.honnomachi.viewmodel.ProfileUiState
import hr.foi.air.honnomachi.viewmodel.ProfileViewModel
import hr.foi.air.honnomachi.viewmodel.ViewModelFactory

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    navController: NavController,
    authViewModel: AuthViewModel,
    homeViewModel: HomeViewModel,
    profileViewModel: ProfileViewModel = viewModel(factory = ViewModelFactory())
) {
    val profileUiState by profileViewModel.uiState.collectAsState()

    val profileIcon = if (profileUiState is ProfileUiState.Success && (profileUiState as ProfileUiState.Success).user.admin == true) {
        Icons.Default.ManageAccounts
    } else {
        Icons.Default.Person
    }

    val navItemList = listOf(
        NavItem(label = stringResource(R.string.home), icon = Icons.AutoMirrored.Outlined.MenuBook),
        NavItem(label = stringResource(R.string.shelf), icon = Icons.AutoMirrored.Filled.LibraryBooks),
        NavItem(label = stringResource(R.string.add), icon = Icons.Default.AddCircle),
        NavItem(label = stringResource(R.string.cart), icon = Icons.Default.ShoppingCart),
        NavItem(label = stringResource(R.string.profile), icon = profileIcon)
    )

    var selectedIndex by rememberSaveable { mutableIntStateOf(0) }


    Scaffold(
        bottomBar = {
            NavigationBar {
                navItemList.forEachIndexed { index, navItem ->
                    NavigationBarItem(
                        selected = index == selectedIndex,
                        onClick = { selectedIndex = index },
                        label = { Text(text = navItem.label) },
                        icon = {
                            Icon(imageVector = navItem.icon, contentDescription = navItem.label)
                        },
                    )
                }
            }
        },
    ) { paddingValues ->
        ContentScreen(
            paddingValues = paddingValues,
            selectedIndex,
            navController,
            authViewModel,
            homeViewModel,
            profileViewModel
        )

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
) {
    when (selectedIndex) {
        0 -> HomePage(
            paddingValues = paddingValues,
            navController = navController,
            viewModel = homeViewModel
        )
        1 -> ShelfPage(paddingValues = paddingValues)
        2 -> AddPage(paddingValues = paddingValues)
        3 -> CartPage(paddingValues = paddingValues)
        4 -> ProfileScreen(
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
            profileViewModel = profileViewModel
        )
    }
}

data class NavItem(
    val label: String,
    val icon: ImageVector,
)