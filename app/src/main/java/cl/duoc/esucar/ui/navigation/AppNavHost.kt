package cl.duoc.esucar.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import cl.duoc.esucar.AppContainer
import cl.duoc.esucar.ESUCARApplication
import cl.duoc.esucar.domain.model.RolUsuario
import cl.duoc.esucar.ui.components.OfflineBanner
import cl.duoc.esucar.ui.components.TeacherBottomBar
import cl.duoc.esucar.ui.screens.RegisterScreen
import cl.duoc.esucar.ui.screens.auth.AuthViewModel
import cl.duoc.esucar.ui.screens.login.LoginScreen
import cl.duoc.esucar.ui.screens.notifications.NotificationsScreen
import cl.duoc.esucar.ui.screens.notifications.NotificationsViewModel
import cl.duoc.esucar.ui.screens.teacher.editor.TeacherEditorScreen
import cl.duoc.esucar.ui.screens.teacher.editor.TeacherEditorViewModel
import cl.duoc.esucar.ui.screens.teacher.inbox.TeacherInboxScreen
import cl.duoc.esucar.ui.screens.teacher.inbox.TeacherInboxViewModel
import cl.duoc.esucar.ui.screens.chat.DirectChatScreen
import cl.duoc.esucar.ui.screens.chat.DirectChatViewModel
import cl.duoc.esucar.ui.screens.teacher.materials.TeacherMaterialsScreen
import cl.duoc.esucar.ui.screens.teacher.materials.TeacherMaterialsViewModel
import cl.duoc.esucar.ui.screens.teacher.reply.TeacherReplyScreen
import cl.duoc.esucar.ui.screens.teacher.reply.TeacherReplyViewModel
import cl.duoc.esucar.ui.theme.AppBackground
import cl.duoc.esucar.ui.theme.Green400
import cl.duoc.esucar.ui.theme.Navy950
import kotlinx.coroutines.launch

private sealed interface SessionRootState {
    data object Loading : SessionRootState
    data class Ready(
        val rol: RolUsuario?,
        val userName: String?
    ) : SessionRootState
}

private enum class AuthScreen {
    LOGIN,
    REGISTER
}

/** Sondeo de sincronización mientras hay una sesión activa. */
private const val SYNC_INTERVAL_MS = 30_000L

@Composable
fun ESUCARRoot() {
    val context = LocalContext.current
    val application = context.applicationContext as ESUCARApplication
    val container = application.container
    val sessionStore = container.sessionStore
    val scope = rememberCoroutineScope()
    val authViewModel: AuthViewModel = viewModel(
        factory = AuthViewModel.fabrica(
            userRepository = container.userRepository,
            supabaseAuthRepository = container.supabaseAuthRepository,
            sessionStore = sessionStore
        )
    )
    val authState by authViewModel.uiState.collectAsStateWithLifecycle()

    var authScreen by rememberSaveable {
        mutableStateOf(AuthScreen.LOGIN)
    }

    val sessionState by produceState<SessionRootState>(
        initialValue = SessionRootState.Loading,
        key1 = sessionStore
    ) {
        kotlinx.coroutines.flow.combine(
            sessionStore.rol,
            sessionStore.userName
        ) { rol, userName ->
            SessionRootState.Ready(rol = rol, userName = userName)
        }.collect { state ->
            value = state
        }
    }

    when (val state = sessionState) {
        SessionRootState.Loading -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Navy950)
                    .statusBarsPadding(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Green400)
            }
        }

        is SessionRootState.Ready -> {
            // Refresco periódico: mantiene la bandeja y el progreso al día sin
            // depender de Realtime (WebSocket). 30 s es suficiente para la demo.
            LaunchedEffect(state.rol) {
                if (state.rol == null) return@LaunchedEffect
                while (true) {
                    container.repository.sincronizar()
                    kotlinx.coroutines.delay(SYNC_INTERVAL_MS)
                }
            }

            when (state.rol) {
                null -> {
                    when (authScreen) {
                        AuthScreen.LOGIN -> {
                            LoginScreen(
                                error = authState.error,
                                isLoading = authState.cargando,
                                onLogin = authViewModel::iniciarSesion,
                                onRegisterClick = {
                                    authViewModel.limpiarError()
                                    authScreen = AuthScreen.REGISTER
                                },
                                onInputChanged = authViewModel::limpiarError
                            )
                        }

                        AuthScreen.REGISTER -> {
                            RegisterScreen(
                                error = authState.error,
                                isLoading = authState.cargando,
                                onRegister = authViewModel::registrar,
                                onBackToLogin = {
                                    authViewModel.limpiarError()
                                    authScreen = AuthScreen.LOGIN
                                },
                                onInputChanged = authViewModel::limpiarError
                            )
                        }
                    }
                }

                RolUsuario.DOCENTE -> {
                    TeacherNavHost(
                        container = container,
                        onLogout = {
                            scope.launch {
                                val remoto = container.supabaseAuthRepository
                                if (remoto != null) remoto.cerrarSesion() else sessionStore.clear()
                                authScreen = AuthScreen.LOGIN
                            }
                        }
                    )
                }

                RolUsuario.ESTUDIANTE -> {
                    StudentNavHost(
                        container = container,
                        userName = state.userName.orEmpty(),
                        onLogout = {
                            scope.launch {
                                val remoto = container.supabaseAuthRepository
                                if (remoto != null) remoto.cerrarSesion() else sessionStore.clear()
                                authScreen = AuthScreen.LOGIN
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun TeacherNavHost(
    container: AppContainer,
    onLogout: () -> Unit
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val isOnline by container.connectivityObserver.isOnline
        .collectAsStateWithLifecycle(initialValue = true)

    // Refresco de la caché al navegar entre las pantallas principales.
    LaunchedEffect(currentRoute) {
        if (currentRoute == Routes.TEACHER_INBOX || currentRoute == Routes.TEACHER_MATERIALS) {
            container.repository.sincronizar()
        }
    }

    val showBottomBar =
        currentRoute == Routes.TEACHER_INBOX ||
                currentRoute == Routes.TEACHER_MATERIALS

    Scaffold(
        containerColor = AppBackground,
        contentWindowInsets = WindowInsets.safeDrawing.only(
            WindowInsetsSides.Bottom
        ),
        bottomBar = {
            if (showBottomBar) {
                TeacherBottomBar(
                    currentRoute = currentRoute,
                    onInbox = {
                        navController.navigate(Routes.TEACHER_INBOX) {
                            popUpTo(
                                navController.graph.findStartDestination().id
                            ) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onMaterials = {
                        navController.navigate(Routes.TEACHER_MATERIALS) {
                            popUpTo(
                                navController.graph.findStartDestination().id
                            ) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (!isOnline) {
                OfflineBanner()
            }

            NavHost(
                navController = navController,
                startDestination = Routes.TEACHER_INBOX,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                composable(Routes.TEACHER_INBOX) {
                    val viewModel: TeacherInboxViewModel = viewModel(
                        factory = TeacherInboxViewModel.fabrica(
                            container.repository
                        )
                    )

                    TeacherInboxScreen(
                        viewModel = viewModel,
                        onDirectChat = { navController.navigate(Routes.DIRECT_CHAT) },
                        onMessageClick = { messageId ->
                            navController.navigate(
                                Routes.teacherReply(messageId)
                            )
                        },
                        onNotifications = {
                            navController.navigate(Routes.NOTIFICATIONS)
                        },
                        onLogout = onLogout
                    )
                }

                composable(Routes.DIRECT_CHAT) {
                    val chatViewModel: DirectChatViewModel = viewModel(
                        factory = DirectChatViewModel.fabrica(
                            sync = container.syncRepository,
                            messageDao = container.directMessageDao,
                            courseDao = container.courseDao,
                            sessionStore = container.sessionStore,
                            role = RolUsuario.DOCENTE
                        )
                    )
                    DirectChatScreen(
                        viewModel = chatViewModel,
                        role = RolUsuario.DOCENTE,
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(Routes.TEACHER_MATERIALS) {
                    val viewModel: TeacherMaterialsViewModel = viewModel(
                        factory = TeacherMaterialsViewModel.fabrica(
                            container.repository
                        )
                    )

                    TeacherMaterialsScreen(
                        viewModel = viewModel,
                        onPublish = {
                            navController.navigate(
                                Routes.teacherEditor()
                            )
                        },
                        onEdit = { asignaturaId, unidadId, materialId ->
                            navController.navigate(
                                Routes.teacherEditor(
                                    asignaturaId = asignaturaId,
                                    unidadId = unidadId,
                                    materialId = materialId
                                )
                            )
                        },
                        onNotifications = {
                            navController.navigate(Routes.NOTIFICATIONS)
                        },
                        onLogout = onLogout
                    )
                }

                composable(
                    route = Routes.TEACHER_REPLY,
                    arguments = listOf(
                        navArgument("messageId") {
                            type = NavType.StringType
                        }
                    )
                ) { entry ->
                    val messageId =
                        entry.arguments?.getString("messageId").orEmpty()

                    val viewModel: TeacherReplyViewModel = viewModel(
                        factory = TeacherReplyViewModel.fabrica(
                            repository = container.repository,
                            mensajeId = messageId
                        )
                    )

                    TeacherReplyScreen(
                        viewModel = viewModel,
                        onBack = {
                            navController.popBackStack()
                        }
                    )
                }

                composable(
                    route = Routes.TEACHER_EDITOR,
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
                        }
                    )
                ) { entry ->
                    val asignaturaId =
                        entry.arguments?.getString("asignaturaId")
                    val unidadId =
                        entry.arguments?.getString("unidadId")
                    val materialId =
                        entry.arguments?.getString("materialId")

                    val viewModel: TeacherEditorViewModel = viewModel(
                        factory = TeacherEditorViewModel.fabrica(
                            repository = container.repository,
                            asignaturaId = asignaturaId,
                            unidadId = unidadId,
                            materialId = materialId
                        )
                    )

                    TeacherEditorScreen(
                        viewModel = viewModel,
                        onBack = {
                            navController.popBackStack()
                        },
                        onBackToMaterials = {
                            if (
                                !navController.popBackStack(
                                    Routes.TEACHER_MATERIALS,
                                    false
                                )
                            ) {
                                navController.navigate(
                                    Routes.TEACHER_MATERIALS
                                ) {
                                    launchSingleTop = true
                                }
                            }
                        }
                    )
                }

                composable(Routes.NOTIFICATIONS) {
                    val viewModel: NotificationsViewModel = viewModel(
                        factory = NotificationsViewModel.fabrica(
                            repository = container.repository,
                            rol = RolUsuario.DOCENTE
                        )
                    )

                    NotificationsScreen(
                        viewModel = viewModel,
                        onBack = {
                            navController.popBackStack()
                        }
                    )
                }
            }
        }
    }
}
