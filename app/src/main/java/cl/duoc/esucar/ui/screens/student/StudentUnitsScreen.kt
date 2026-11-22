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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ChevronRight
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
import cl.duoc.esucar.domain.model.Unidad
import cl.duoc.esucar.domain.model.estaCompleto
import cl.duoc.esucar.domain.model.porcentajeProgreso
import cl.duoc.esucar.domain.model.cantidadEstudiados
import cl.duoc.esucar.ui.components.AppHeader
import cl.duoc.esucar.ui.components.EmptyState
import cl.duoc.esucar.ui.components.LinearProgressBar
import cl.duoc.esucar.ui.components.SectionLabel
import cl.duoc.esucar.ui.theme.AppBackground
import cl.duoc.esucar.ui.theme.Green700
import cl.duoc.esucar.ui.theme.Navy950
import cl.duoc.esucar.ui.theme.Slate100
import cl.duoc.esucar.ui.theme.Slate200
import cl.duoc.esucar.ui.theme.Slate400
import cl.duoc.esucar.ui.theme.Slate500
import cl.duoc.esucar.ui.util.parseCourseColor

@Composable
fun StudentUnitsScreen(
    viewModel: StudentViewModel,
    asignaturaId: String,
    onUnitClick: (unidadId: String) -> Unit,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val asignatura = uiState.asignaturas.firstOrNull { it.id == asignaturaId }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
    ) {
        AppHeader(
            titulo = asignatura?.nombre ?: "Unidades",
            subtitle = asignatura?.let { "${it.codigo} · ${it.nombreDocente}" },
            onBack = onBack
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

            asignatura == null -> {
                EmptyState(
                    icon = Icons.AutoMirrored.Outlined.MenuBook,
                    titulo = "Asignatura no encontrada",
                    message = "La asignatura solicitada ya no está disponible."
                )
            }

            else -> {
                val colorAsignatura = parseCourseColor(asignatura.color)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    SectionLabel(text = "Unidades Temáticas")
                    Spacer(modifier = Modifier.height(10.dp))

                    asignatura.unidades.forEach { unidad ->
                        UnitCard(
                            unidad = unidad,
                            colorAsignatura = colorAsignatura,
                            onClick = { onUnitClick(unidad.id) }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun UnitCard(
    unidad: Unidad,
    colorAsignatura: Color,
    onClick: () -> Unit
) {
    val completa = unidad.estaCompleto()

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
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (completa) colorAsignatura else Slate100),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = unidad.numero.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    color = if (completa) Color.White else colorAsignatura,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Unidad ${unidad.numero}",
                    style = MaterialTheme.typography.titleSmall,
                    color = Navy950
                )
                Text(
                    text = unidad.titulo,
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate500,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressBar(
                    progress = unidad.porcentajeProgreso() / 100f,
                    color = colorAsignatura
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${unidad.cantidadEstudiados()}/${unidad.materiales.size} revisados",
                    style = MaterialTheme.typography.labelSmall,
                    color = Slate400
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (completa) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(colorAsignatura),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Check,
                        contentDescription = "Unidad completada",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            } else {
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    tint = Slate400
                )
            }
        }
    }
}
