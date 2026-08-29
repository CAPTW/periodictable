package com.chemtable.interactive.navigation

import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.chemtable.interactive.feature.minigame.dex.MoleculeDexScreen
import com.chemtable.interactive.feature.minigame.MoleculeGameScreen
import com.chemtable.interactive.feature.minigame.reactor.ReactorFoundationScreen
import com.chemtable.interactive.feature.visualization.VisualizationScreen
import com.chemtable.interactive.feature.periodictable.PeriodicTableScreen
import com.chemtable.interactive.feature.search.SearchScreen
import com.chemtable.interactive.feature.settings.SettingsScreen

private sealed class BottomTab(
    val screen: Screen,
    val icon: ImageVector,
    val label: String,
    // screen.route: Jetpack Navigation Graph 등록을 위한 라우트 패턴 (예: "calculator?formula={formula}")
    // navRoute: 하단 탭 버튼을 눌렀을 때 실제로 이동할 구체적 경로 (예: 계산기는 빈 폼으로 진입하기 위해 "calculator" 사용)
    val navRoute: String = screen.route,
) {
    object Table : BottomTab(Screen.PeriodicTable, Icons.Filled.ViewModule, "주기율표")
    object Search : BottomTab(Screen.Search, Icons.Filled.Search, "검색")
    object Visual : BottomTab(Screen.Visualization, Icons.AutoMirrored.Filled.List, "시각화")
    object Calc : BottomTab(
        Screen.Calculator,
        Icons.Filled.Calculate,
        "계산기",
        navRoute = Screen.Calculator.createRoute(),
    )
    object Dict : BottomTab(Screen.Glossary, Icons.AutoMirrored.Filled.MenuBook, "사전")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChemTableNavHost(
    navController: NavHostController = rememberNavController()
) {
    val tabs = remember { listOf(BottomTab.Table, BottomTab.Search, BottomTab.Visual, BottomTab.Calc, BottomTab.Dict) }
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.PeriodicTable.route

    // 게임 플레이 화면(풀스크린)에서는 하단바를 숨긴다.
    val showBottomBar = currentRoute?.startsWith("game/") != true

    Scaffold(
        topBar = {
            if (showBottomBar) {
                TopAppBar(
                    title = { Text("ChemTable Interactive") },
                    actions = {
                        IconButton(onClick = {
                            navController.navigate(Screen.Settings.route) { launchSingleTop = true }
                        }) {
                            Icon(Icons.Filled.Settings, contentDescription = "설정")
                        }
                    },
                )
            }
        },
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    for (tab in tabs) {
                        val selected = currentRoute == tab.screen.route ||
                            (tab.screen == Screen.Visualization &&
                                currentRoute in setOf(
                                    Screen.MoleculeDex.route,
                                    Screen.ReactorFoundation.route,
                                ))
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(tab.navRoute) {
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
                SearchScreen(
                    innerPadding = innerPadding,
                    onElementClick = { atomicNumber ->
                        navController.navigate(Screen.ElementDetail.createRoute(atomicNumber))
                    }
                )
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
                    },
                    onOpenGlossaryTerm = { termId ->
                        navController.navigate(Screen.GlossaryDetail.createRoute(termId))
                    },
                    onPlayMiniGame = { id ->
                        navController.navigate(Screen.MoleculeGame.createRoute(id)) {
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(Screen.Visualization.route) {
                VisualizationScreen(
                    innerPadding = innerPadding,
                    onPlayMiniGame = {
                        navController.navigate(Screen.MoleculeGame.createRoute()) {
                            launchSingleTop = true
                        }
                    },
                    onOpenMoleculeDex = {
                        navController.navigate(Screen.MoleculeDex.route) {
                            launchSingleTop = true
                        }
                    },
                    onOpenReactorFoundation = {
                        navController.navigate(Screen.ReactorFoundation.route) {
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable(Screen.ReactorFoundation.route) {
                ReactorFoundationScreen(
                    innerPadding = innerPadding,
                    onNavigateBack = { navController.popBackStack() },
                )
            }
            composable(Screen.MoleculeDex.route) {
                MoleculeDexScreen(
                    innerPadding = innerPadding,
                    onNavigateBack = { navController.popBackStack() },
                    onPlayMiniGame = {
                        navController.navigate(Screen.MoleculeGame.createRoute()) {
                            launchSingleTop = true
                        }
                    },
                    onOpenCalculator = { formula ->
                        navController.navigate(Screen.Calculator.createRoute(formula)) {
                            launchSingleTop = true
                        }
                    },
                    onOpenElement = { atomicNumber ->
                        navController.navigate(Screen.ElementDetail.createRoute(atomicNumber))
                    },
                    onOpenGlossary = { termId ->
                        navController.navigate(Screen.GlossaryDetail.createRoute(termId))
                    }
                )
            }
            composable(
                route = Screen.MoleculeGame.route,
                arguments = listOf(navArgument("atomicNumber") {
                    type = NavType.IntType
                    defaultValue = -1
                })
            ) {
                MoleculeGameScreen(
                    onExit = { navController.popBackStack() },
                    // 게임 → 계산기: 식 프리필 전달. 게임 라우트는 백스택에 남아 back 시 결과로 복귀.
                    onOpenCalculator = { formula ->
                        navController.navigate(Screen.Calculator.createRoute(formula)) {
                            launchSingleTop = true
                        }
                    },
                    onOpenElement = { atomicNumber ->
                        navController.navigate(Screen.ElementDetail.createRoute(atomicNumber))
                    },
                    onOpenGlossary = { termId ->
                        navController.navigate(Screen.GlossaryDetail.createRoute(termId))
                    }
                )
            }
            composable(
                route = Screen.Calculator.route,
                arguments = listOf(navArgument(Screen.Calculator.ARG_FORMULA) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                })
            ) {
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
                    termId = Uri.decode(termId),
                    onNavigateBack = { navController.popBackStack() },
                    onOpenTerm = { selectedTermId ->
                        navController.navigate(Screen.GlossaryDetail.createRoute(selectedTermId))
                    },
                    onOpenElement = { atomicNumber ->
                        navController.navigate(Screen.ElementDetail.createRoute(atomicNumber))
                    }
                )
            }
            composable(Screen.Settings.route) {
                SettingsScreen(innerPadding = innerPadding)
            }
        }
    }
}
