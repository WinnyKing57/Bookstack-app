package com.winnyking.bookstackcompanion.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.winnyking.bookstackcompanion.R

sealed class Screen(val route: String, @StringRes val titleResId: Int, val icon: ImageVector? = null) {
    object ServerConnect : Screen("server_connect", R.string.nav_server_connect)
    object Dashboard : Screen("dashboard", R.string.nav_dashboard, Icons.Default.Dashboard)
    object Books : Screen("books", R.string.nav_books, Icons.Default.Book)
    object Shelves : Screen("shelves", R.string.nav_shelves, Icons.Default.FolderSpecial)
    object Search : Screen("search", R.string.nav_search, Icons.Default.Search)
    object Favorites : Screen("favorites", R.string.nav_favorites, Icons.Default.Bookmark)
    object Settings : Screen("settings", R.string.nav_settings, Icons.Default.Settings)

    object BookDetail : Screen("book_detail/{bookId}", R.string.nav_book_detail) {
        fun createRoute(bookId: Long) = "book_detail/$bookId"
    }

    object PageReader : Screen("page_reader/{pageId}", R.string.nav_page_reader) {
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
