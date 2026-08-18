package com.winnyking.bookstackcompanion.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector? = null) {
    object ServerConnect : Screen("server_connect", "Connexion Serveur")
    object Dashboard : Screen("dashboard", "Tableau de bord", Icons.Default.Dashboard)
    object Books : Screen("books", "Livres", Icons.Default.Book)
    object Shelves : Screen("shelves", "Étagères", Icons.Default.FolderSpecial)
    object Search : Screen("search", "Recherche", Icons.Default.Search)
    object Favorites : Screen("favorites", "Favoris", Icons.Default.Bookmark)
    object Settings : Screen("settings", "Paramètres", Icons.Default.Settings)

    object BookDetail : Screen("book_detail/{bookId}", "Détail du livre") {
        fun createRoute(bookId: Long) = "book_detail/$bookId"
    }

    object PageReader : Screen("page_reader/{pageId}", "Lecture de page") {
        fun createRoute(pageId: Long) = "page_reader/$pageId"
    }
}

val bottomNavItems = listOf(
    Screen.Dashboard,
    Screen.Books,
    Screen.Shelves,
    Screen.Search,
    Screen.Favorites,
    Screen.Settings
)
