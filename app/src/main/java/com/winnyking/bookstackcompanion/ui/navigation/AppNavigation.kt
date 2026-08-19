package com.winnyking.bookstackcompanion.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.winnyking.bookstackcompanion.ui.screens.BookDetailScreen
import com.winnyking.bookstackcompanion.ui.screens.BooksScreen
import com.winnyking.bookstackcompanion.ui.screens.DashboardScreen
import com.winnyking.bookstackcompanion.ui.screens.FavoritesScreen
import com.winnyking.bookstackcompanion.ui.screens.PageReaderScreen
import com.winnyking.bookstackcompanion.ui.screens.SearchScreen
import com.winnyking.bookstackcompanion.ui.screens.ServerConnectScreen
import com.winnyking.bookstackcompanion.ui.screens.SettingsScreen
import com.winnyking.bookstackcompanion.ui.screens.ShelvesScreen

@Composable
fun AppNavigation(
    startDestination: String
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = bottomNavItems.any { it.route == currentRoute }
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon ?: Icons.Default.Home, contentDescription = null) },
                            label = { Text(stringResource(screen.titleResId)) },
                            selected = currentRoute == screen.route,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Screen.ServerConnect.route) {
                ServerConnectScreen(
                    onServerConnected = {
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.ServerConnect.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    onNavigateToBooks = { navController.navigate(Screen.Books.route) },
                    onNavigateToShelves = { navController.navigate(Screen.Shelves.route) },
                    onNavigateToSearch = { navController.navigate(Screen.Search.route) },
                    onNavigateToFavorites = { navController.navigate(Screen.Favorites.route) },
                    onNavigateToBookDetail = { bookId -> navController.navigate(Screen.BookDetail.createRoute(bookId)) },
                    onNavigateToPageReader = { pageId -> navController.navigate(Screen.PageReader.createRoute(pageId)) },
                    onNavigateToConnectServer = { navController.navigate(Screen.ServerConnect.route) }
                )
            }

            composable(Screen.Books.route) {
                BooksScreen(
                    onNavigateToBookDetail = { bookId -> navController.navigate(Screen.BookDetail.createRoute(bookId)) }
                )
            }

            composable(
                route = Screen.BookDetail.route,
                arguments = listOf(navArgument("bookId") { type = NavType.LongType })
            ) {
                BookDetailScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToPageReader = { pageId -> navController.navigate(Screen.PageReader.createRoute(pageId)) }
                )
            }

            composable(
                route = Screen.PageReader.route,
                arguments = listOf(navArgument("pageId") { type = NavType.LongType })
            ) {
                PageReaderScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Shelves.route) {
                ShelvesScreen(
                    onNavigateToBookDetail = { bookId -> navController.navigate(Screen.BookDetail.createRoute(bookId)) }
                )
            }

            composable(Screen.Search.route) {
                SearchScreen(
                    onNavigateToPageReader = { pageId -> navController.navigate(Screen.PageReader.createRoute(pageId)) },
                    onNavigateToBookDetail = { bookId -> navController.navigate(Screen.BookDetail.createRoute(bookId)) }
                )
            }

            composable(Screen.Favorites.route) {
                FavoritesScreen(
                    onNavigateToPageReader = { pageId -> navController.navigate(Screen.PageReader.createRoute(pageId)) }
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    onNavigateToConnectServer = { navController.navigate(Screen.ServerConnect.route) }
                )
            }
        }
    }
}
