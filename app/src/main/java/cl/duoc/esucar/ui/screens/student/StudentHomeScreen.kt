package cl.duoc.esucar.ui.screens.student

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ExitToApp
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cl.duoc.esucar.domain.model.Asignatura
import cl.duoc.esucar.domain.model.porcentajeProgreso
import cl.duoc.esucar.domain.model.cantidadEstudiados
import cl.duoc.esucar.domain.model.cantidadTotal
import cl.duoc.esucar.ui.components.EmptyState
import cl.duoc.esucar.ui.components.HeaderIconButton
import cl.duoc.esucar.ui.components.LinearProgressBar
import cl.duoc.esucar.ui.components.NotificationBell
import cl.duoc.esucar.ui.components.SectionLabel
import cl.duoc.esucar.ui.theme.AppBackground
import cl.duoc.esucar.ui.theme.Blue200
import cl.duoc.esucar.ui.theme.Blue50
import cl.duoc.esucar.ui.theme.Blue700
import cl.duoc.esucar.ui.theme.Green400
import cl.duoc.esucar.ui.theme.Green50
import cl.duoc.esucar.ui.theme.Green700
import cl.duoc.esucar.ui.theme.GreenBorder
import cl.duoc.esucar.ui.theme.Navy700
import cl.duoc.esucar.ui.theme.Navy800
import cl.duoc.esucar.ui.theme.Navy950
import cl.duoc.esucar.ui.theme.Slate200
import cl.duoc.esucar.ui.theme.Slate400
import cl.duoc.esucar.ui.theme.Slate500
import cl.duoc.esucar.ui.util.parseCourseColor

@Composable
fun StudentHomeScreen(
    viewModel: StudentViewModel,
    userName: String,
    onCourseClick: (String) -> Unit,
    onMessages: () -> Unit,
    onDownloads: () -> Unit,
    onNotifications: () -> Unit,
    onLogout: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
    ) {
        Surface(color = Navy950) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "ESUCAR · Semestre I-2025",
                            style = MaterialTheme.typography.bodySmall,
                            color = Green400
                        )
                        Text(
                            text = "Hola, ${userName.ifBlank { "Estudiante" }}",
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    NotificationBell(unreadCount = uiState.noLeidas, onClick = onNotifications)
                    Spacer(modifier = Modifier.width(8.dp))
                    HeaderIconButton(
                        icon = Icons.AutoMirrored.Outlined.ExitToApp,
                        contentDescription = "Cerrar sesión",
                        onClick = onLogout
                    )
                }
            }
        }

        if (uiState.cargando) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Green700)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                Surface(
                    color = Navy800,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Progreso General",
                                style = MaterialTheme.typography.titleSmall,
                                color = Color.White,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "${uiState.porcentajeGeneral}%",
                                style = MaterialTheme.typography.headlineMedium,
                                color = Green400
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        LinearProgressBar(
                            progress = uiState.porcentajeGeneral / 100f,
                            color = Green400,
                            trackColor = Navy700,
                            height = 8.dp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${uiState.totalEstudiados} de ${uiState.totalMateriales} materiales revisados",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate400
                        )
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))
                SectionLabel(text = "Mis Asignaturas")
                Spacer(modifier = Modifier.height(10.dp))

                if (uiState.asignaturas.isEmpty()) {
                    EmptyState(
                        icon = Icons.Outlined.Description,
                        titulo = "Sin asignaturas",
                        message = "Aún no tienes asignaturas disponibles."
                    )
                } else {
                    uiState.asignaturas.forEach { asignatura ->
                        StudentCourseRow(
                            asignatura = asignatura,
                            onClick = { onCourseClick(asignatura.id) }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                SectionLabel(text = "Acceso Rápido")
                Spacer(modifier = Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    QuickAccessCard(
                        titulo = "Consultas",
                        subtitle = if (uiState.consultasPendientes == 0) {
                            "Todo respondido"
                        } else {
                            "${uiState.consultasPendientes} sin respuesta"
                        },
                        icon = Icons.Outlined.Forum,
                        background = Blue50,
                        border = Blue200,
                        accent = Blue700,
                        onClick = onMessages,
                        modifier = Modifier.weight(1f)
                    )
                    QuickAccessCard(
                        titulo = "Descargas",
                        subtitle = "Disponible sin conexión",
                        icon = Icons.Outlined.Description,
                        background = Green50,
                        border = GreenBorder,
                        accent = Green700,
                        onClick = onDownloads,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun StudentCourseRow(
    asignatura: Asignatura,
    onClick: () -> Unit
) {
    val colorAsignatura = parseCourseColor(asignatura.color)
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Slate200),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colorAsignatura),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = asignatura.codigo.take(3),
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = asignatura.nombre,
                    style = MaterialTheme.typography.titleSmall,
                    color = Navy950,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = asignatura.nombreDocente,
                    style = MaterialTheme.typography.labelSmall,
                    color = Slate500,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressBar(
                    progress = asignatura.porcentajeProgreso() / 100f,
                    color = colorAsignatura
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${asignatura.cantidadEstudiados()}/${asignatura.cantidadTotal()} estudiados",
                    style = MaterialTheme.typography.labelSmall,
                    color = colorAsignatura
                )
            }

            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = Slate400
            )
        }
    }
}

@Composable
private fun QuickAccessCard(
    titulo: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    background: Color,
    border: Color,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = background,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, border),
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = titulo,
                style = MaterialTheme.typography.titleSmall,
                color = accent
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = Slate500
            )
        }
    }
}
