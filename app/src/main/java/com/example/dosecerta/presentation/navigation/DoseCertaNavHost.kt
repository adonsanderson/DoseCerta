package com.example.dosecerta.presentation.navigation

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.R
import com.example.dosecerta.presentation.screens.addtreatment.AddTreatmentRoute
import com.example.dosecerta.presentation.screens.drugsearch.DrugSearchRoute
import com.example.dosecerta.presentation.screens.home.HomeRoute
import com.example.dosecerta.presentation.screens.treatmentdetail.TreatmentDetailRoute

private data class TopLevelDestination(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val TOP_LEVEL_DESTINATIONS = listOf(
    TopLevelDestination(
        route = DoseCertaRoute.HOME,
        labelRes = R.string.nav_schedule,
        selectedIcon = Icons.Filled.Medication,
        unselectedIcon = Icons.Outlined.Medication,
        testTag = "nav_tab_home"
    ),
    TopLevelDestination(
        route = DoseCertaRoute.DRUG_SEARCH,
        labelRes = R.string.nav_drugs,
        selectedIcon = Icons.AutoMirrored.Filled.MenuBook,
        unselectedIcon = Icons.AutoMirrored.Outlined.MenuBook,
        testTag = "nav_tab_drugs"
    )
)

@Composable
fun DoseCertaNavHost(
    initialTreatmentId: Long? = null,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val showTopLevelNav = currentRoute == DoseCertaRoute.HOME ||
        currentRoute == DoseCertaRoute.DRUG_SEARCH

    LaunchedEffect(initialTreatmentId) {
        if (initialTreatmentId != null && initialTreatmentId > 0L) {
            navController.navigate(DoseCertaRoute.treatmentDetail(initialTreatmentId)) {
                launchSingleTop = true
            }
        }
    }

    val navigateToTopLevel: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val useNavRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                if (showTopLevelNav && !useNavRail) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        TOP_LEVEL_DESTINATIONS.forEach { destination ->
                            val selected = currentRoute == destination.route
                            val label = stringResource(destination.labelRes)
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateToTopLevel(destination.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) {
                                            destination.selectedIcon
                                        } else {
                                            destination.unselectedIcon
                                        },
                                        contentDescription = label
                                    )
                                },
                                label = { Text(text = label) },
                                modifier = Modifier.testTag(destination.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (showTopLevelNav && useNavRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        TOP_LEVEL_DESTINATIONS.forEach { destination ->
                            val selected = currentRoute == destination.route
                            val label = stringResource(destination.labelRes)
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateToTopLevel(destination.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) {
                                            destination.selectedIcon
                                        } else {
                                            destination.unselectedIcon
                                        },
                                        contentDescription = label
                                    )
                                },
                                label = { Text(text = label) },
                                modifier = Modifier.testTag(destination.testTag)
                            )
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = DoseCertaRoute.HOME,
                    modifier = Modifier.weight(1f)
                ) {
                    composable(DoseCertaRoute.HOME) {
                        HomeRoute(
                            onAddTreatmentClick = {
                                navController.navigate(DoseCertaRoute.ADD_TREATMENT)
                            },
                            onTreatmentClick = { treatmentId ->
                                navController.navigate(DoseCertaRoute.treatmentDetail(treatmentId))
                            }
                        )
                    }

                    composable(DoseCertaRoute.ADD_TREATMENT) {
                        AddTreatmentRoute(
                            onBackClick = { navController.popBackStack() },
                            onSavedSuccess = { navController.popBackStack() }
                        )
                    }

                    composable(
                        route = DoseCertaRoute.TREATMENT_DETAIL_PATTERN,
                        arguments = listOf(
                            navArgument(DoseCertaRoute.ARG_TREATMENT_ID) {
                                type = NavType.LongType
                            }
                        )
                    ) { entry ->
                        val treatmentId = entry.arguments?.getLong(DoseCertaRoute.ARG_TREATMENT_ID)
                            ?: -1L
                        TreatmentDetailRoute(
                            treatmentId = treatmentId,
                            onBackClick = { navController.popBackStack() }
                        )
                    }

                    composable(DoseCertaRoute.DRUG_SEARCH) {
                        DrugSearchRoute()
                    }
                }
            }
        }
    }
}
