package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.ui.screens.*
import com.example.ui.theme.KamickTheme
import com.example.ui.viewmodel.MainViewModel

sealed class Screen(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    object Library : Screen("library", "Library", Icons.Filled.MenuBook, Icons.Outlined.MenuBook)
    object Updates : Screen("updates", "Updates", Icons.Filled.Sync, Icons.Outlined.Sync)
    object Browse : Screen("browse", "Browse", Icons.Filled.Explore, Icons.Outlined.Explore)
    object Trackers : Screen("trackers", "Trackers", Icons.Filled.CloudSync, Icons.Outlined.CloudSync)
    object Settings : Screen("settings", "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
    object Detail : Screen("detail/{mangaId}", "Detail", Icons.Filled.MenuBook, Icons.Outlined.MenuBook) {
        fun createRoute(mangaId: Long) = "detail/$mangaId"
    }
    object Reader : Screen("reader/{mangaId}/{chapterId}?page={page}", "Reader", Icons.Filled.MenuBook, Icons.Outlined.MenuBook) {
        fun createRoute(mangaId: Long, chapterId: Long, page: Int = 0) = "reader/$mangaId/$chapterId?page=$page"
    }
}

val bottomNavItems = listOf(
    Screen.Library,
    Screen.Updates,
    Screen.Browse,
    Screen.Trackers,
    Screen.Settings
)

@Composable
fun KamickApp(
    viewModel: MainViewModel = viewModel()
) {
    var showSplashScreen by rememberSaveable { mutableStateOf(true) }

    if (showSplashScreen) {
        SplashScreen(onFinish = { showSplashScreen = false })
        return
    }

    val navController = rememberNavController()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val isReaderScreen = currentRoute?.startsWith("reader") == true

    KamickTheme(themeMode = uiState.settings.themeMode) {
        Scaffold(
            bottomBar = {
                if (!isReaderScreen) {
                    NavigationBar(
                        windowInsets = WindowInsets.navigationBars,
                        modifier = Modifier.testTag("bottom_navigation_bar")
                    ) {
                        bottomNavItems.forEach { screen ->
                            val isSelected = currentRoute == screen.route
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (isSelected) screen.selectedIcon else screen.unselectedIcon,
                                        contentDescription = screen.title
                                    )
                                },
                                label = { Text(screen.title) }
                            )
                        }
                    }
                }
            },
            contentWindowInsets = if (isReaderScreen) WindowInsets(0.dp) else ScaffoldDefaults.contentWindowInsets
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Screen.Library.route,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(Screen.Library.route) {
                    LibraryScreen(
                        viewModel = viewModel,
                        uiState = uiState,
                        onMangaClick = { mangaId ->
                            navController.navigate(Screen.Detail.createRoute(mangaId))
                        }
                    )
                }

                composable(Screen.Updates.route) {
                    UpdatesScreen(
                        viewModel = viewModel,
                        uiState = uiState,
                        onMangaClick = { mangaId ->
                            navController.navigate(Screen.Detail.createRoute(mangaId))
                        }
                    )
                }

                composable(Screen.Browse.route) {
                    BrowseScreen(
                        viewModel = viewModel,
                        onOpenMangaDetail = { mangaId ->
                            navController.navigate(Screen.Detail.createRoute(mangaId))
                        },
                        onReadChapter = { mangaId, chapterId, page ->
                            navController.navigate(Screen.Reader.createRoute(mangaId, chapterId, page))
                        }
                    )
                }

                composable(Screen.Trackers.route) {
                    TrackersScreen(
                        viewModel = viewModel,
                        uiState = uiState
                    )
                }

                composable(Screen.Settings.route) {
                    SettingsScreen(
                        viewModel = viewModel,
                        uiState = uiState
                    )
                }

                composable(
                    route = Screen.Detail.route,
                    arguments = listOf(navArgument("mangaId") { type = NavType.LongType })
                ) { backStackEntry ->
                    val mangaId = backStackEntry.arguments?.getLong("mangaId") ?: 0L
                    MangaDetailScreen(
                        mangaId = mangaId,
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() },
                        onReadChapter = { mId, chId, page ->
                            navController.navigate(Screen.Reader.createRoute(mId, chId, page))
                        }
                    )
                }

                composable(
                    route = Screen.Reader.route,
                    arguments = listOf(
                        navArgument("mangaId") { type = NavType.LongType },
                        navArgument("chapterId") { type = NavType.LongType },
                        navArgument("page") {
                            type = NavType.IntType
                            defaultValue = 0
                        }
                    )
                ) { backStackEntry ->
                    val mangaId = backStackEntry.arguments?.getLong("mangaId") ?: 0L
                    val chapterId = backStackEntry.arguments?.getLong("chapterId") ?: 0L
                    val page = backStackEntry.arguments?.getInt("page") ?: 0
                    ReaderScreen(
                        mangaId = mangaId,
                        chapterId = chapterId,
                        startPage = page,
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() },
                        onNavigateToChapter = { nextChapterId ->
                            navController.navigate(Screen.Reader.createRoute(mangaId, nextChapterId, 0)) {
                                popUpTo(Screen.Detail.createRoute(mangaId))
                            }
                        }
                    )
                }
            }
        }
    }
}
