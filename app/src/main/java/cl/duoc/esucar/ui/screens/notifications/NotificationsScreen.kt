package cl.duoc.esucar.ui.screens.notifications

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Reply
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cl.duoc.esucar.core.time.formatShortDateTime
import cl.duoc.esucar.domain.model.Notificacion
import cl.duoc.esucar.domain.model.TipoNotificacion
import cl.duoc.esucar.ui.components.AppHeader
import cl.duoc.esucar.ui.components.EmptyState
import cl.duoc.esucar.ui.theme.AppBackground
import cl.duoc.esucar.ui.theme.Blue200
import cl.duoc.esucar.ui.theme.Blue50
import cl.duoc.esucar.ui.theme.Blue700
import cl.duoc.esucar.ui.theme.Green400
import cl.duoc.esucar.ui.theme.Green50
import cl.duoc.esucar.ui.theme.Green700
import cl.duoc.esucar.ui.theme.Navy800
import cl.duoc.esucar.ui.theme.Navy950
import cl.duoc.esucar.ui.theme.Slate200
import cl.duoc.esucar.ui.theme.Slate400
import cl.duoc.esucar.ui.theme.Slate500
import cl.duoc.esucar.ui.theme.Slate600

@Composable
fun NotificationsScreen(
    viewModel: NotificationsViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedNotification by remember { mutableStateOf<Notificacion?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
    ) {
        AppHeader(
            titulo = "Notificaciones",
            subtitle = if (uiState.noLeidas > 0) "${uiState.noLeidas} sin leer" else "Todo al día",
            onBack = onBack,
            actions = {
                if (uiState.noLeidas > 0) {
                    Surface(
                        color = Navy800,
                        contentColor = Green400,
                        shape = CircleShape,
                        modifier = Modifier.clickable(onClick = viewModel::marcarTodasLeidas)
                    ) {
                        Box(
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Leer todas",
                                style = MaterialTheme.typography.labelMedium,
                                color = Green400
                            )
                        }
                    }
                }
            }
        )

        when {
            uiState.cargando -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Green700)
                }
            }

            uiState.notificaciones.isEmpty() -> {
                EmptyState(
                    icon = Icons.Outlined.Notifications,
                    titulo = "Sin notificaciones",
                    message = "Aquí aparecerán respuestas, consultas y nuevos materiales."
                )
            }

            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    uiState.notificaciones.forEach { notificacion ->
                        NotificationCard(
                            notificacion = notificacion,
                            onClick = {
                                selectedNotification = notificacion
                                if (!notificacion.leida) viewModel.marcarLeida(notificacion.id)
                            }
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
            }
        }
    }

    selectedNotification?.let { notification ->
        AlertDialog(
            onDismissRequest = { selectedNotification = null },
            icon = {
                Icon(
                    imageVector = when (notification.tipo) {
                        TipoNotificacion.MATERIAL -> Icons.Outlined.Description
                        TipoNotificacion.CONSULTA -> Icons.Outlined.ChatBubbleOutline
                        TipoNotificacion.RESPUESTA -> Icons.AutoMirrored.Outlined.Reply
                    },
                    contentDescription = null
                )
            },
            title = { Text(notification.titulo) },
            text = {
                Column {
                    Text(notification.descripcion)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = formatShortDateTime(notification.fecha),
                        style = MaterialTheme.typography.labelMedium,
                        color = Slate500
                    )
                }
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { selectedNotification = null }) {
                    Text("Entendido")
                }
            }
        )
    }
}

@Composable
private fun NotificationCard(
    notificacion: Notificacion,
    onClick: () -> Unit
) {
    val isMaterial = notificacion.tipo == TipoNotificacion.MATERIAL
    val accent = if (isMaterial) Green700 else Blue700
    val iconBackground = if (isMaterial) Green50 else Blue50
    val icon = when (notificacion.tipo) {
        TipoNotificacion.MATERIAL -> Icons.Outlined.Description
        TipoNotificacion.CONSULTA -> Icons.Outlined.ChatBubbleOutline
        TipoNotificacion.RESPUESTA -> Icons.AutoMirrored.Outlined.Reply
    }

    Surface(
        color = if (notificacion.leida) Color.White else Blue50,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(
            width = 1.dp,
            color = if (notificacion.leida) Slate200 else Blue200
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(modifier = Modifier.padding(14.dp)) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(iconBackground),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = notificacion.titulo,
                        style = MaterialTheme.typography.titleSmall,
                        color = Navy950,
                        fontWeight = if (notificacion.leida) FontWeight.Normal else FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    if (!notificacion.leida) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(Blue700)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = notificacion.descripcion,
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate600
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = formatShortDateTime(notificacion.fecha),
                    style = MaterialTheme.typography.labelSmall,
                    color = Slate400
                )
            }
        }
    }
}
