package com.example.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Leaderboard
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.UserProgressRepository
import com.example.data.VocabularyRepository
import com.example.model.VocabularyCategory
import com.example.tts.TextToSpeechHelper
import com.example.ui.home.HomeScreen
import com.example.ui.home.HomeViewModel
import com.example.ui.learning.LearningScreen
import com.example.ui.learning.LearningViewModel
import com.example.ui.levels.LevelDetailsScreen
import com.example.ui.levels.LevelsScreen
import com.example.ui.progress.ProgressScreen
import com.example.ui.revision.RevisionScreen
import com.example.ui.revision.RevisionViewModel
import com.example.ui.settings.SettingsScreen
import com.example.ui.test.MasteryTestViewModel
import com.example.ui.test.TestResultsScreen
import com.example.ui.test.TestScreen
import com.example.ui.theme.TealContainerLight
import com.example.ui.theme.TealPrimary

sealed interface AppScreen {
    object Home : AppScreen
    object Revision : AppScreen
    object Progress : AppScreen
    object Settings : AppScreen

    data class Levels(val category: VocabularyCategory) : AppScreen
    data class LevelDetails(val category: VocabularyCategory, val levelNumber: Int) : AppScreen
    data class Learning(val category: VocabularyCategory, val levelNumber: Int) : AppScreen
    data class Test(val category: VocabularyCategory, val levelNumber: Int) : AppScreen
    data class TestResults(
        val category: VocabularyCategory,
        val levelNumber: Int,
        val totalQuestions: Int,
        val mistakesCount: Int,
        val mistakeIds: List<String>
    ) : AppScreen
}

enum class BottomTab(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    HOME("Home", Icons.Filled.Home, Icons.Outlined.Home),
    REVISION("Revision", Icons.Filled.Star, Icons.Outlined.StarOutline),
    PROGRESS("Progress", Icons.Filled.Leaderboard, Icons.Outlined.Leaderboard)
}

@Composable
fun AppNavigation(
    vocabularyRepository: VocabularyRepository,
    userProgressRepository: UserProgressRepository,
    ttsHelper: TextToSpeechHelper
) {
    val homeViewModel: HomeViewModel = viewModel(
        factory = HomeViewModel.Factory(vocabularyRepository, userProgressRepository)
    )
    val revisionViewModel: RevisionViewModel = viewModel(
        factory = RevisionViewModel.Factory(vocabularyRepository, userProgressRepository, ttsHelper)
    )

    val backStack = remember { mutableStateListOf<AppScreen>(AppScreen.Home) }

    fun navigateTo(screen: AppScreen) {
        backStack.add(screen)
    }

    fun popBack(): Boolean {
        return if (backStack.size > 1) {
            backStack.removeAt(backStack.size - 1)
            homeViewModel.refreshDashboard()
            true
        } else {
            false
        }
    }

    val currentScreen = backStack.lastOrNull() ?: AppScreen.Home

    // Handle system back button across the whole application
    BackHandler(enabled = backStack.size > 1) {
        popBack()
    }

    // Determine current active bottom tab from screen state
    val currentTab = when (currentScreen) {
        is AppScreen.Revision -> BottomTab.REVISION
        is AppScreen.Progress -> BottomTab.PROGRESS
        else -> BottomTab.HOME
    }

    val showBottomBar = currentScreen is AppScreen.Home ||
            currentScreen is AppScreen.Revision ||
            currentScreen is AppScreen.Progress

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    modifier = Modifier
                        .navigationBarsPadding()
                        .testTag("bottom_nav_bar")
                ) {
                    BottomTab.entries.forEach { tab ->
                        val isSelected = currentTab == tab

                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                when (tab) {
                                    BottomTab.HOME -> {
                                        if (currentScreen !is AppScreen.Home) {
                                            // Pop everything until we reach the root Home
                                            while (backStack.size > 1) {
                                                backStack.removeAt(backStack.size - 1)
                                            }
                                            homeViewModel.refreshDashboard()
                                        }
                                    }
                                    BottomTab.REVISION -> {
                                        if (currentScreen !is AppScreen.Revision) {
                                            navigateTo(AppScreen.Revision)
                                            revisionViewModel.loadWordsForTab()
                                        }
                                    }
                                    BottomTab.PROGRESS -> {
                                        if (currentScreen !is AppScreen.Progress) {
                                            navigateTo(AppScreen.Progress)
                                        }
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = tab.title
                                )
                            },
                            label = { Text(tab.title) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = TealPrimary,
                                selectedTextColor = TealPrimary,
                                indicatorColor = TealContainerLight
                            ),
                            modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                is AppScreen.Home -> {
                    HomeScreen(
                        viewModel = homeViewModel,
                        onNavigateToCategory = { cat ->
                            navigateTo(AppScreen.Levels(cat))
                        },
                        onNavigateToLevel = { cat, lvl ->
                            navigateTo(AppScreen.LevelDetails(cat, lvl))
                        },
                        onNavigateToRevision = {
                            navigateTo(AppScreen.Revision)
                            revisionViewModel.loadWordsForTab()
                        },
                        onNavigateToSettings = {
                            navigateTo(AppScreen.Settings)
                        }
                    )
                }

                is AppScreen.Levels -> {
                    LevelsScreen(
                        category = currentScreen.category,
                        vocabularyRepository = vocabularyRepository,
                        userProgressRepository = userProgressRepository,
                        onLevelClick = { lvl ->
                            navigateTo(AppScreen.LevelDetails(currentScreen.category, lvl))
                        },
                        onBackClick = { popBack() }
                    )
                }

                is AppScreen.LevelDetails -> {
                    LevelDetailsScreen(
                        category = currentScreen.category,
                        levelNumber = currentScreen.levelNumber,
                        vocabularyRepository = vocabularyRepository,
                        userProgressRepository = userProgressRepository,
                        onStartLearning = {
                            navigateTo(AppScreen.Learning(currentScreen.category, currentScreen.levelNumber))
                        },
                        onStartTest = {
                            navigateTo(AppScreen.Test(currentScreen.category, currentScreen.levelNumber))
                        },
                        onBackClick = { popBack() }
                    )
                }

                is AppScreen.Learning -> {
                    val learningViewModel: LearningViewModel = viewModel(
                        key = "Learning_${currentScreen.category.id}_${currentScreen.levelNumber}",
                        factory = LearningViewModel.Factory(
                            category = currentScreen.category,
                            levelNumber = currentScreen.levelNumber,
                            vocabularyRepository = vocabularyRepository,
                            userProgressRepository = userProgressRepository,
                            ttsHelper = ttsHelper
                        )
                    )
                    LearningScreen(
                        viewModel = learningViewModel,
                        userProgressRepository = userProgressRepository,
                        onNavigateToTest = {
                            backStack.removeAt(backStack.size - 1) // Replace learning with test
                            navigateTo(AppScreen.Test(currentScreen.category, currentScreen.levelNumber))
                        },
                        onBackClick = { popBack() }
                    )
                }

                is AppScreen.Test -> {
                    val testViewModel: MasteryTestViewModel = viewModel(
                        key = "Test_${currentScreen.category.id}_${currentScreen.levelNumber}",
                        factory = MasteryTestViewModel.Factory(
                            category = currentScreen.category,
                            levelNumber = currentScreen.levelNumber,
                            vocabularyRepository = vocabularyRepository,
                            userProgressRepository = userProgressRepository,
                            ttsHelper = ttsHelper
                        )
                    )
                    TestScreen(
                        viewModel = testViewModel,
                        onTestFinished = { totalQ, mistakes, mistakeIds ->
                            backStack.removeAt(backStack.size - 1) // Replace test with results
                            navigateTo(
                                AppScreen.TestResults(
                                    category = currentScreen.category,
                                    levelNumber = currentScreen.levelNumber,
                                    totalQuestions = totalQ,
                                    mistakesCount = mistakes,
                                    mistakeIds = mistakeIds
                                )
                            )
                        },
                        onBackClick = { popBack() }
                    )
                }

                is AppScreen.TestResults -> {
                    TestResultsScreen(
                        category = currentScreen.category,
                        levelNumber = currentScreen.levelNumber,
                        totalQuestions = currentScreen.totalQuestions,
                        mistakesCount = currentScreen.mistakesCount,
                        mistakeWordIds = currentScreen.mistakeIds,
                        vocabularyRepository = vocabularyRepository,
                        onRetryTest = {
                            backStack.removeAt(backStack.size - 1)
                            navigateTo(AppScreen.Test(currentScreen.category, currentScreen.levelNumber))
                        },
                        onOpenRevision = {
                            backStack.clear()
                            backStack.add(AppScreen.Home)
                            backStack.add(AppScreen.Revision)
                            revisionViewModel.loadWordsForTab()
                        },
                        onReturnHome = {
                            while (backStack.size > 1) {
                                backStack.removeAt(backStack.size - 1)
                            }
                            homeViewModel.refreshDashboard()
                        }
                    )
                }

                is AppScreen.Revision -> {
                    RevisionScreen(viewModel = revisionViewModel)
                }

                is AppScreen.Progress -> {
                    ProgressScreen(
                        homeViewModel = homeViewModel,
                        userProgressRepository = userProgressRepository
                    )
                }

                is AppScreen.Settings -> {
                    SettingsScreen(
                        userProgressRepository = userProgressRepository,
                        ttsHelper = ttsHelper,
                        onBackClick = { popBack() }
                    )
                }
            }
        }
    }
}
