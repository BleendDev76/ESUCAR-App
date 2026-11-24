package cl.duoc.esucar.ui.screens.teacher.inbox

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cl.duoc.esucar.core.time.formatShortDateTime
import cl.duoc.esucar.domain.model.Asignatura
import cl.duoc.esucar.domain.model.Mensaje
import cl.duoc.esucar.domain.model.Unidad
import cl.duoc.esucar.ui.components.DropdownField
import cl.duoc.esucar.ui.components.EmptyState
import cl.duoc.esucar.ui.components.SectionLabel
import cl.duoc.esucar.ui.components.StatCard
import cl.duoc.esucar.ui.components.StatusPill
import cl.duoc.esucar.ui.components.TeacherPanelHeader
import cl.duoc.esucar.ui.theme.Amber100
import cl.duoc.esucar.ui.theme.Amber200
import cl.duoc.esucar.ui.theme.Amber600
import cl.duoc.esucar.ui.theme.Amber700
import cl.duoc.esucar.ui.theme.AppBackground
import cl.duoc.esucar.ui.theme.Blue50
import cl.duoc.esucar.ui.theme.Blue700
import cl.duoc.esucar.ui.theme.Green50
import cl.duoc.esucar.ui.theme.Green700
import cl.duoc.esucar.ui.theme.GreenBorder
import cl.duoc.esucar.ui.theme.Navy950
import cl.duoc.esucar.ui.theme.Slate200
import cl.duoc.esucar.ui.theme.Slate400
import cl.duoc.esucar.ui.theme.Slate500
import cl.duoc.esucar.ui.theme.Slate600

@Composable
fun TeacherInboxScreen(
    viewModel: TeacherInboxViewModel,
    onMessageClick: (String) -> Unit,
    onDirectChat: () -> Unit,
    onNotifications: () -> Unit,
    onLogout: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
    ) {
        TeacherPanelHeader(
            label = "Panel Docente",
            titulo = "Bandeja de Consultas",
            unreadCount = uiState.noLeidas,
            onNotifications = onNotifications,
            onLogout = onLogout
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
                androidx.compose.material3.Button(
                    onClick = onDirectChat,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Abrir chat directo") }

                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DropdownField(
                        label = "Curso",
                        selectedText = uiState.asignaturas
                            .firstOrNull { it.id == uiState.selectedCourseId }
                            ?.nombre
                            ?: "Todas las asignaturas",
                        options = listOf<Asignatura?>(null) + uiState.asignaturas,
                        optionLabel = { asignatura -> asignatura?.nombre ?: "Todas las asignaturas" },
                        onOptionSelected = { asignatura -> viewModel.onCourseSelected(asignatura?.id) },
                        modifier = Modifier.weight(1f)
                    )
                    DropdownField(
                        label = "Unidad",
                        selectedText = uiState.unidades
                            .firstOrNull { it.id == uiState.selectedUnitId }
                            ?.let { "Unidad ${it.numero}" }
                            ?: "Todas las unidades",
                        options = listOf<Unidad?>(null) + uiState.unidades,
                        optionLabel = { unidad ->
                            if (unidad == null) "Todas las unidades" else "Unidad ${unidad.numero}"
                        },
                        onOptionSelected = { unidad -> viewModel.onUnitSelected(unidad?.id) },
                        enabled = uiState.selectedCourseId != null,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard(
                        value = uiState.pendingCount,
                        label = "Sin respuesta",
                        background = Amber100,
                        accent = Amber700,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        value = uiState.answeredCount,
                        label = "Respondidas",
                        background = Green50,
                        accent = Green700,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        value = uiState.totalCount,
                        label = "Total",
                        background = Blue50,
                        accent = Blue700,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(22.dp))

                if (uiState.totalCount == 0) {
                    EmptyState(
                        icon = Icons.Outlined.ChatBubbleOutline,
                        titulo = "Sin consultas",
                        message = if (uiState.asignaturas.isEmpty()) {
                            "No hay asignaturas ni consultas registradas."
                        } else {
                            "No hay consultas que coincidan con los filtros seleccionados."
                        }
                    )
                } else {
                    if (uiState.pending.isNotEmpty()) {
                        SectionLabel(text = "Pendientes de respuesta", color = Amber700)
                        Spacer(modifier = Modifier.height(10.dp))
                        uiState.pending.forEach { consulta ->
                            TeacherMessageCard(
                                consulta = consulta,
                                onClick = { onMessageClick(consulta.id) }
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                    }

                    if (uiState.answered.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        SectionLabel(text = "Respondidas", color = Green700)
                        Spacer(modifier = Modifier.height(10.dp))
                        uiState.answered.forEach { consulta ->
                            TeacherMessageCard(
                                consulta = consulta,
                                onClick = { onMessageClick(consulta.id) }
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TeacherMessageCard(
    consulta: Mensaje,
    onClick: () -> Unit
) {
    val isPending = consulta.respuesta == null
    Surface(
        color = androidx.compose.ui.graphics.Color.White,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(
            width = 1.dp,
            color = if (isPending) Amber200 else GreenBorder
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = consulta.asunto,
                        style = MaterialTheme.typography.titleSmall,
                        color = Navy950,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${consulta.nombreEstudiante} · ${consulta.nombreAsignatura}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Slate400,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                if (isPending) {
                    StatusPill(text = "Sin respuesta", background = Amber100, textColor = Amber700)
                } else {
                    StatusPill(text = "Respondido", background = Green50, textColor = Green700)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = consulta.contenido,
                style = MaterialTheme.typography.bodySmall,
                color = Slate600,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = formatShortDateTime(consulta.fecha),
                    style = MaterialTheme.typography.labelSmall,
                    color = Slate400
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(3.dp)
                        .background(Slate200)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = consulta.tituloUnidad,
                    style = MaterialTheme.typography.labelSmall,
                    color = Blue700,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
