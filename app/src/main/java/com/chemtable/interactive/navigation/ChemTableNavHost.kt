package com.chemtable.interactive.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.chemtable.interactive.feature.elementdetail.ElementDetailScreen
import com.chemtable.interactive.feature.glossary.GlossaryDetailScreen
import com.chemtable.interactive.feature.glossary.GlossaryScreen
import com.chemtable.interactive.feature.notes.NoteEditorScreen
import com.chemtable.interactive.feature.notes.NotesListScreen
import com.chemtable.interactive.feature.calculator.CalculatorScreen
import com.chemtable.interactive.feature.visualization.VisualizationScreen
import com.chemtable.interactive.feature.periodictable.PeriodicTableScreen
import com.chemtable.interactive.feature.search.SearchScreen
import com.chemtable.interactive.feature.settings.SettingsScreen

private sealed class BottomTab(
    val screen: Screen,
    val icon: ImageVector,
    val label: String
) {
    object Table : BottomTab(Screen.PeriodicTable, Icons.Filled.ViewModule, "주기율표")
    object Search : BottomTab(Screen.Search, Icons.Filled.Search, "검색")
    object Visual : BottomTab(Screen.Visualization, Icons.AutoMirrored.Filled.List, "시각화")
    object Calc : BottomTab(Screen.Calculator, Icons.Filled.Calculate, "계산기")
    object Dict : BottomTab(Screen.Glossary, Icons.Filled.MenuBook, "사전")
}

@Composable
fun ChemTableNavHost(
    navController: NavHostController = rememberNavController()
) {
    val tabs = remember { listOf(BottomTab.Table, BottomTab.Search, BottomTab.Visual, BottomTab.Calc, BottomTab.Dict) }
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.PeriodicTable.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                for (tab in tabs) {
                    NavigationBarItem(
                        selected = currentRoute == tab.screen.route,
                        onClick = {
                            navController.navigate(tab.screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(navController = navController, startDestination = Screen.PeriodicTable.route) {
            composable(Screen.PeriodicTable.route) {
                PeriodicTableScreen(
                    innerPadding = innerPadding,
                    onElementSelected = { atomicNumber ->
                        navController.navigate(Screen.ElementDetail.createRoute(atomicNumber))
                    }
                )
            }
            composable(Screen.Search.route) {
                SearchScreen(innerPadding = innerPadding)
            }
            composable(
                route = Screen.ElementDetail.route,
                arguments = listOf(navArgument("atomicNumber") { type = NavType.IntType })
            ) { entry ->
                val atomicNumber = entry.arguments?.getInt("atomicNumber") ?: 1
                ElementDetailScreen(
                    atomicNumber = atomicNumber,
                    onOpenNotes = { id ->
                        navController.navigate(Screen.Notes.createRoute(id))
                    },
                    onAddNote = { id ->
                        navController.navigate(Screen.NoteEditor.createRoute(noteId = null, elementId = id))
                    }
                )
            }
            composable(Screen.Visualization.route) {
                VisualizationScreen(innerPadding = innerPadding)
            }
            composable(Screen.Calculator.route) {
                CalculatorScreen(innerPadding = innerPadding, onNavigateToPeriodicTable = {
                    navController.navigate(Screen.PeriodicTable.route) {
                        launchSingleTop = true
                    }
                })
            }
            composable(Screen.Glossary.route) {
                GlossaryScreen(
                    innerPadding = innerPadding,
                    onTermClick = { termId ->
                        navController.navigate(Screen.GlossaryDetail.createRoute(termId))
                    }
                )
            }
            composable(Screen.Notes.route, arguments = listOf(navArgument("elementId") {
                type = NavType.IntType
                defaultValue = -1
            })) { entry ->
                val elementId = entry.arguments?.getInt("elementId") ?: -1
                NotesListScreen(
                    innerPadding = innerPadding,
                    elementId = elementId,
                    onOpenEditor = { selectedNoteId, targetElement ->
                        navController.navigate(Screen.NoteEditor.createRoute(selectedNoteId, targetElement))
                    },
                    onOpenNote = { selectedNoteId, targetElement ->
                        navController.navigate(Screen.NoteEditor.createRoute(selectedNoteId, targetElement))
                    }
                )
            }
            composable(
                route = Screen.NoteEditor.route,
                arguments = listOf(
                    navArgument("noteId") {
                        type = NavType.LongType
                        defaultValue = -1L
                    },
                    navArgument("elementId") {
                        type = NavType.IntType
                        defaultValue = -1
                    }
                )
            ) { entry ->
                val noteId = entry.arguments?.getLong("noteId") ?: -1L
                val elementId = entry.arguments?.getInt("elementId") ?: -1
                NoteEditorScreen(
                    innerPadding = innerPadding,
                    noteId = if (noteId < 0L) null else noteId,
                    elementId = if (elementId < 0) null else elementId,
                    onBack = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() }
                )
            }
            composable(
                route = Screen.GlossaryDetail.route,
                arguments = listOf(navArgument("termId") { type = NavType.StringType })
            ) { entry ->
                val termId = entry.arguments?.getString("termId") ?: ""
                GlossaryDetailScreen(
                    innerPadding = innerPadding,
                    termId = termId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Settings.route) {
                SettingsScreen(innerPadding = innerPadding)
            }
        }
    }
}
