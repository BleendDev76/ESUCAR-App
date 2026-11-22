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
import androidx.compose.material.icons.outlined.FileDownload
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
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cl.duoc.esucar.domain.model.Material
import cl.duoc.esucar.domain.model.cantidadEstudiados
import cl.duoc.esucar.ui.components.AppHeader
import cl.duoc.esucar.ui.components.EmptyState
import cl.duoc.esucar.ui.components.LinearProgressBar
import cl.duoc.esucar.ui.components.MaterialTypeIcon
import cl.duoc.esucar.ui.components.style
import cl.duoc.esucar.ui.theme.AppBackground
import cl.duoc.esucar.ui.theme.Green700
import cl.duoc.esucar.ui.theme.Navy950
import cl.duoc.esucar.ui.theme.Slate200
import cl.duoc.esucar.ui.theme.Slate400
import cl.duoc.esucar.ui.theme.Slate500
import cl.duoc.esucar.ui.util.parseCourseColor

@Composable
fun StudentMaterialsScreen(
    viewModel: StudentViewModel,
    asignaturaId: String,
    unidadId: String,
    onMaterialClick: (materialId: String) -> Unit,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val asignatura = uiState.asignaturas.firstOrNull { it.id == asignaturaId }
    val unidad = asignatura?.unidades?.firstOrNull { it.id == unidadId }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
    ) {
        AppHeader(
            titulo = unidad?.let { "Unidad ${it.numero}: ${it.titulo}" } ?: "Materiales",
            subtitle = asignatura?.nombre,
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

            asignatura == null || unidad == null -> {
                EmptyState(
                    icon = Icons.AutoMirrored.Outlined.MenuBook,
                    titulo = "Unidad no encontrada",
                    message = "La unidad solicitada ya no está disponible."
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
                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Slate200),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Progreso de la unidad",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Slate500,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "${unidad.cantidadEstudiados()}/${unidad.materiales.size}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = colorAsignatura
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressBar(
                                progress = if (unidad.materiales.isEmpty()) {
                                    0f
                                } else {
                                    unidad.cantidadEstudiados().toFloat() / unidad.materiales.size
                                },
                                color = colorAsignatura,
                                height = 8.dp,
                                shape = RectangleShape
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    unidad.materiales.forEach { material ->
                        MaterialRow(
                            material = material,
                            onClick = { onMaterialClick(material.id) }
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun MaterialRow(
    material: Material,
    onClick: () -> Unit
) {
    val style = material.tipo.style()

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
            MaterialTypeIcon(tipo = material.tipo, size = 40.dp)

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = material.titulo,
                    style = MaterialTheme.typography.titleSmall,
                    color = Navy950,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = listOfNotNull(style.listLabel, material.tamano).joinToString(" · "),
                    style = MaterialTheme.typography.labelSmall,
                    color = Slate500
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (material.estudiado) "Revisado" else "Pendiente",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (material.estudiado) Green700 else Slate400
                )
            }

            if (material.descargado) {
                Icon(
                    imageVector = Icons.Outlined.FileDownload,
                    contentDescription = "Descargado",
                    tint = Green700,
                    modifier = Modifier.size(18.dp)
                )
            }

            if (material.estudiado) {
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(Green700),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Check,
                        contentDescription = "Revisado",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = Slate400
            )
        }
    }
}
