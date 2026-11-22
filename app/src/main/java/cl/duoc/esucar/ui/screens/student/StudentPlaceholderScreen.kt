package cl.duoc.esucar.ui.screens.student

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExitToApp
import androidx.compose.material.icons.outlined.Person
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import cl.duoc.esucar.ui.components.AppHeader
import cl.duoc.esucar.ui.components.EmptyState
import cl.duoc.esucar.ui.components.HeaderIconButton
import cl.duoc.esucar.ui.theme.AppBackground

@Composable
fun StudentPlaceholderScreen(onLogout: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .navigationBarsPadding()
    ) {
        AppHeader(
            titulo = "Sección Estudiante",
            subtitle = "Acceso provisional",
            actions = {
                HeaderIconButton(
                    icon = Icons.Outlined.ExitToApp,
                    contentDescription = "Cerrar sesión",
                    onClick = onLogout
                )
            }
        )

        EmptyState(
            icon = Icons.Outlined.Person,
            titulo = "Sección en construcción",
            message = "El apartado del estudiante se implementará en la siguiente etapa. Mientras tanto, puedes cerrar sesión y entrar como docente.",
            actionLabel = "Cerrar sesión",
            onAction = onLogout
        )
    }
}
