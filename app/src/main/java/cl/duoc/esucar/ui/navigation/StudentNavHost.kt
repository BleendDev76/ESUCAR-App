package cl.duoc.esucar.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import cl.duoc.esucar.AppContainer
import cl.duoc.esucar.domain.model.RolUsuario
import cl.duoc.esucar.ui.components.OfflineBanner
import cl.duoc.esucar.ui.components.StudentBottomBar
import cl.duoc.esucar.ui.screens.notifications.NotificationsScreen
import cl.duoc.esucar.ui.screens.notifications.NotificationsViewModel
import cl.duoc.esucar.ui.screens.student.StudentComposeScreen
import cl.duoc.esucar.ui.screens.student.StudentCoursesScreen
import cl.duoc.esucar.ui.screens.student.StudentDetailScreen
import cl.duoc.esucar.ui.screens.student.StudentDownloadsScreen
import cl.duoc.esucar.ui.screens.student.StudentHomeScreen
import cl.duoc.esucar.ui.screens.student.StudentMaterialsScreen
import cl.duoc.esucar.ui.screens.student.StudentMessagesScreen
import cl.duoc.esucar.ui.screens.student.StudentUnitsScreen
import cl.duoc.esucar.ui.screens.student.StudentViewModel
import cl.duoc.esucar.ui.screens.chat.DirectChatScreen
import cl.duoc.esucar.ui.screens.chat.DirectChatViewModel
import cl.duoc.esucar.ui.theme.AppBackground

@Composable
fun StudentNavHost(
    container: AppContainer,
    userName: String,
    onLogout: () -> Unit
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val isOnline by container.connectivityObserver.isOnline
        .collectAsStateWithLifecycle(initialValue = true)

    // Refresco de la caché al navegar entre las pantallas principales.
    LaunchedEffect(currentRoute) {
        if (currentRoute in STUDENT_SYNC_ROUTES) {
            container.repository.sincronizar()
        }
    }

    val showBottomBar =
        currentRoute == Routes.STUDENT_HOME ||
                currentRoute == Routes.STUDENT_COURSES ||
                currentRoute == Routes.STUDENT_MESSAGES

    Scaffold(
        containerColor = AppBackground,
        contentWindowInsets = WindowInsets.safeDrawing.only(
            WindowInsetsSides.Bottom
        ),
        bottomBar = {
            if (showBottomBar) {
                StudentBottomBar(
                    currentRoute = currentRoute,
                    onHome = {
                        navController.navigateToStudentTab(Routes.STUDENT_HOME)
                    },
                    onCourses = {
                        navController.navigateToStudentTab(Routes.STUDENT_COURSES)
                    },
                    onMessages = {
                        navController.navigateToStudentTab(Routes.STUDENT_MESSAGES)
                    }
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AppBackground)
                .padding(innerPadding)
        ) {
            if (!isOnline) {
                OfflineBanner()
            }

            NavHost(
                navController = navController,
                startDestination = Routes.STUDENT_GRAPH,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                navigation(
                    startDestination = Routes.STUDENT_HOME,
                    route = Routes.STUDENT_GRAPH
                ) {
                    composable(Routes.STUDENT_HOME) {
                        StudentHomeScreen(
                            viewModel = studentViewModel(
                                navController = navController,
                                container = container,
                                userName = userName
                            ),
                            userName = userName,
                            onCourseClick = { asignaturaId ->
                                navController.navigate(
                                    Routes.studentUnits(asignaturaId)
                                )
                            },
                            onMessages = {
                                navController.navigateToStudentTab(
                                    Routes.STUDENT_MESSAGES
                                )
                            },
                            onDownloads = {
                                navController.navigate(Routes.STUDENT_DOWNLOADS)
                            },
                            onNotifications = {
                                navController.navigate(Routes.NOTIFICATIONS)
                            },
                            onLogout = onLogout
                        )
                    }

                    composable(Routes.STUDENT_COURSES) {
                        StudentCoursesScreen(
                            viewModel = studentViewModel(
                                navController = navController,
                                container = container,
                                userName = userName
                            ),
                            onCourseClick = { asignaturaId ->
                                navController.navigate(
                                    Routes.studentUnits(asignaturaId)
                                )
                            }
                        )
                    }

                    composable(
                        route = Routes.STUDENT_UNITS,
                        arguments = listOf(
                            navArgument("asignaturaId") {
                                type = NavType.StringType
                            }
                        )
                    ) { entry ->
                        val asignaturaId =
                            entry.arguments?.getString("asignaturaId").orEmpty()

                        StudentUnitsScreen(
                            viewModel = studentViewModel(
                                navController = navController,
                                container = container,
                                userName = userName
                            ),
                            asignaturaId = asignaturaId,
                            onUnitClick = { unidadId ->
                                navController.navigate(
                                    Routes.studentMaterials(asignaturaId, unidadId)
                                )
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable(
                        route = Routes.STUDENT_MATERIALS,
                        arguments = listOf(
                            navArgument("asignaturaId") {
                                type = NavType.StringType
                            },
                            navArgument("unidadId") {
                                type = NavType.StringType
                            }
                        )
                    ) { entry ->
                        val asignaturaId =
                            entry.arguments?.getString("asignaturaId").orEmpty()
                        val unidadId =
                            entry.arguments?.getString("unidadId").orEmpty()

                        StudentMaterialsScreen(
                            viewModel = studentViewModel(
                                navController = navController,
                                container = container,
                                userName = userName
                            ),
                            asignaturaId = asignaturaId,
                            unidadId = unidadId,
                            onMaterialClick = { materialId ->
                                navController.navigate(
                                    Routes.studentDetail(
                                        asignaturaId = asignaturaId,
                                        unidadId = unidadId,
                                        materialId = materialId
                                    )
                                )
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable(
                        route = Routes.STUDENT_DETAIL,
                        arguments = listOf(
                            navArgument("asignaturaId") {
                                type = NavType.StringType
                            },
                            navArgument("unidadId") {
                                type = NavType.StringType
                            },
                            navArgument("materialId") {
                                type = NavType.StringType
                            }
                        )
                    ) { entry ->
                        val asignaturaId =
                            entry.arguments?.getString("asignaturaId").orEmpty()
                        val unidadId =
                            entry.arguments?.getString("unidadId").orEmpty()
                        val materialId =
                            entry.arguments?.getString("materialId").orEmpty()

                        StudentDetailScreen(
                            viewModel = studentViewModel(
                                navController = navController,
                                container = container,
                                userName = userName
                            ),
                            asignaturaId = asignaturaId,
                            unidadId = unidadId,
                            materialId = materialId,
                            onAskTeacher = { cId, uId, mId ->
                                navController.navigate(
                                    Routes.studentCompose(
                                        asignaturaId = cId,
                                        unidadId = uId,
                                        materialId = mId
                                    )
                                )
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable(Routes.STUDENT_MESSAGES) {
                        StudentMessagesScreen(
                            viewModel = studentViewModel(
                                navController = navController,
                                container = container,
                                userName = userName
                            ),
                            onNewQuery = {
                                navController.navigate(
                                    Routes.studentCompose()
                                )
                            },
                            onMessageClick = { mensajeId ->
                                navController.navigate(
                                    Routes.studentCompose(messageId = mensajeId)
                                )
                            },
                            onDirectChat = { navController.navigate(Routes.DIRECT_CHAT) }
                        )
                    }

                    composable(Routes.DIRECT_CHAT) {
                        val chatViewModel: DirectChatViewModel = viewModel(
                            factory = DirectChatViewModel.fabrica(
                                sync = container.syncRepository,
                                messageDao = container.directMessageDao,
                                courseDao = container.courseDao,
                                sessionStore = container.sessionStore,
                                role = RolUsuario.ESTUDIANTE
                            )
                        )
                        DirectChatScreen(
                            viewModel = chatViewModel,
                            role = RolUsuario.ESTUDIANTE,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable(
                        route = Routes.STUDENT_COMPOSE,
                        arguments = listOf(
                            navArgument("asignaturaId") {
                                type = NavType.StringType
                                nullable = true
                                defaultValue = null
                            },
                            navArgument("unidadId") {
                                type = NavType.StringType
                                nullable = true
                                defaultValue = null
                            },
                            navArgument("materialId") {
                                type = NavType.StringType
                                nullable = true
                                defaultValue = null
                            },
                            navArgument("messageId") {
                                type = NavType.StringType
                                nullable = true
                                defaultValue = null
                            }
                        )
                    ) { entry ->
                        val viewModel = studentViewModel(
                            navController = navController,
                            container = container,
                            userName = userName
                        )

                        StudentComposeScreen(
                            viewModel = viewModel,
                            initialCourseId =
                                entry.arguments?.getString("asignaturaId"),
                            initialUnitId =
                                entry.arguments?.getString("unidadId"),
                            initialMaterialId =
                                entry.arguments?.getString("materialId"),
                            messageId =
                                entry.arguments?.getString("messageId"),
                            onVerConsultas = {
                                navController.navigateToStudentTab(
                                    Routes.STUDENT_MESSAGES
                                )
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable(Routes.STUDENT_DOWNLOADS) {
                        StudentDownloadsScreen(
                            viewModel = studentViewModel(
                                navController = navController,
                                container = container,
                                userName = userName
                            ),
                            onMaterialClick = { asignaturaId, unidadId, materialId ->
                                navController.navigate(
                                    Routes.studentDetail(
                                        asignaturaId = asignaturaId,
                                        unidadId = unidadId,
                                        materialId = materialId
                                    )
                                )
                            },
                            onExplore = {
                                navController.navigateToStudentTab(
                                    Routes.STUDENT_COURSES
                                )
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }
                }

                composable(Routes.NOTIFICATIONS) {
                    val viewModel: NotificationsViewModel = viewModel(
                        factory = NotificationsViewModel.fabrica(
                            repository = container.repository,
                            rol = RolUsuario.ESTUDIANTE
                        )
                    )

                    NotificationsScreen(
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}

@Composable
private fun studentViewModel(
    navController: NavHostController,
    container: AppContainer,
    userName: String
): StudentViewModel {
    val graphEntry = remember(navController) {
        navController.getBackStackEntry(Routes.STUDENT_GRAPH)
    }
    return viewModel(
        viewModelStoreOwner = graphEntry,
        factory = StudentViewModel.fabrica(
            repository = container.repository,
            nombreEstudiante = userName
        )
    )
}

private fun NavHostController.navigateToStudentTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

/** Pantallas que refrescan datos desde Supabase al abrirse. */
private val STUDENT_SYNC_ROUTES = setOf(
    Routes.STUDENT_HOME,
    Routes.STUDENT_COURSES,
    Routes.STUDENT_MESSAGES,
    Routes.DIRECT_CHAT
)
