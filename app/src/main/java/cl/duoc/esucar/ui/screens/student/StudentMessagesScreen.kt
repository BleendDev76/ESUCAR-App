package cl.duoc.esucar.ui.screens.student

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cl.duoc.esucar.core.time.formatShortDateTime
import cl.duoc.esucar.domain.model.Mensaje
import cl.duoc.esucar.ui.components.AppHeader
import cl.duoc.esucar.ui.components.EmptyState
import cl.duoc.esucar.ui.components.PrimaryActionButton
import cl.duoc.esucar.ui.components.SectionLabel
import cl.duoc.esucar.ui.components.StatusPill
import cl.duoc.esucar.ui.theme.AppBackground
import cl.duoc.esucar.ui.theme.Green50
import cl.duoc.esucar.ui.theme.Green700
import cl.duoc.esucar.ui.theme.GreenBorder
import cl.duoc.esucar.ui.theme.Navy950
import cl.duoc.esucar.ui.theme.Slate200
import cl.duoc.esucar.ui.theme.Slate400
import cl.duoc.esucar.ui.theme.Slate100
import cl.duoc.esucar.ui.theme.Slate500

@Composable
fun StudentMessagesScreen(
    viewModel: StudentViewModel,
    onNewQuery: () -> Unit,
    onMessageClick: (mensajeId: String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
    ) {
        val totalConsultas = uiState.mensajes.size
        val pendientes = uiState.consultasPendientes

        AppHeader(
            titulo = "Mis Consultas",
            subtitle = when {
                pendientes == 0 -> "$totalConsultas consultas enviadas"
                pendientes == 1 -> "$totalConsultas consultas enviadas · 1 pendiente"
                else -> "$totalConsultas consultas enviadas · $pendientes pendientes"
            }
        )

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
                PrimaryActionButton(
                    text = "Nueva Consulta",
                    onClick = onNewQuery
                )

                Spacer(modifier = Modifier.height(20.dp))

                if (uiState.mensajes.isEmpty()) {
                    EmptyState(
                        icon = Icons.Outlined.Forum,
                        titulo = "Sin consultas",
                        message = "Cuando tengas dudas, envía una consulta al docente."
                    )
                } else {
                    SectionLabel(text = "Historial")
                    Spacer(modifier = Modifier.height(10.dp))

                    uiState.mensajes.forEach { consulta ->
                        MessageCard(consulta = consulta, onClick = { onMessageClick(consulta.id) })
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun MessageCard(consulta: Mensaje, onClick: () -> Unit) {
    val answered = consulta.respuesta != null

    Surface(
        color = Color.White,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Slate200),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = consulta.nombreAsignatura,
                        style = MaterialTheme.typography.labelSmall,
                        color = Slate400
                    )
                    Text(
                        text = consulta.asunto,
                        style = MaterialTheme.typography.titleSmall,
                        color = Navy950
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                StatusPill(
                    text = if (answered) "Respondida" else "Pendiente",
                    background = if (answered) Green50 else Slate100,
                    textColor = if (answered) Green700 else Slate500
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = listOfNotNull(
                    "Unidad: ${consulta.tituloUnidad}",
                    consulta.tituloMaterial?.let { "Material: $it" }
                ).joinToString(" · "),
                style = MaterialTheme.typography.labelSmall,
                color = Slate500
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = consulta.contenido,
                style = MaterialTheme.typography.bodySmall,
                color = Navy950
            )

            Text(
                text = formatShortDateTime(consulta.fecha),
                style = MaterialTheme.typography.labelSmall,
                color = Slate400,
                modifier = Modifier.padding(top = 6.dp)
            )

            if (consulta.respuesta != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    color = Green50,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, GreenBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Respuesta del docente",
                            style = MaterialTheme.typography.labelSmall,
                            color = Green700
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = consulta.respuesta,
                            style = MaterialTheme.typography.bodySmall,
                            color = Navy950
                        )
                        if (consulta.fechaRespuesta != null) {
                            Text(
                                text = formatShortDateTime(consulta.fechaRespuesta),
                                style = MaterialTheme.typography.labelSmall,
                                color = Slate500,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
