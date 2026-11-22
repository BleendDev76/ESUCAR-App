package cl.duoc.esucar.ui.screens.student

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cl.duoc.esucar.domain.model.TipoMaterial
import cl.duoc.esucar.ui.components.AppHeader
import cl.duoc.esucar.ui.components.EmptyState
import cl.duoc.esucar.ui.components.MaterialDetailIcon
import cl.duoc.esucar.ui.components.PrimaryActionButton
import cl.duoc.esucar.ui.components.SectionLabel
import cl.duoc.esucar.ui.components.StatusPill
import cl.duoc.esucar.ui.components.style
import cl.duoc.esucar.ui.theme.AppBackground
import cl.duoc.esucar.ui.theme.Blue200
import cl.duoc.esucar.ui.theme.Blue50
import cl.duoc.esucar.ui.theme.Blue700
import cl.duoc.esucar.ui.theme.Green50
import cl.duoc.esucar.ui.theme.Green700
import cl.duoc.esucar.ui.theme.GreenBorder
import cl.duoc.esucar.ui.theme.Navy950
import cl.duoc.esucar.ui.theme.Red600
import cl.duoc.esucar.ui.theme.Slate200
import cl.duoc.esucar.ui.theme.Slate300
import cl.duoc.esucar.ui.theme.Slate400
import cl.duoc.esucar.ui.theme.Slate500
import cl.duoc.esucar.ui.theme.Slate600
import cl.duoc.esucar.ui.util.parseCourseColor

@Composable
fun StudentDetailScreen(
    viewModel: StudentViewModel,
    asignaturaId: String,
    unidadId: String,
    materialId: String,
    onAskTeacher: (asignaturaId: String, unidadId: String, materialId: String) -> Unit,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val asignatura = uiState.asignaturas.firstOrNull { it.id == asignaturaId }
    val unidad = asignatura?.unidades?.firstOrNull { it.id == unidadId }
    val material = unidad?.materiales?.firstOrNull { it.id == materialId }
    var vistaAbierta by rememberSaveable(materialId) { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
    ) {
        AppHeader(
            titulo = material?.titulo ?: "Material de Estudio",
            subtitle = listOfNotNull(
                asignatura?.codigo,
                unidad?.let { "Unidad ${it.numero}" }
            ).joinToString(" · ").ifBlank { null },
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

            material == null -> {
                EmptyState(
                    icon = Icons.AutoMirrored.Outlined.MenuBook,
                    titulo = "Material no encontrado",
                    message = "El material solicitado ya no está disponible."
                )
            }

            else -> {
                val colorAsignatura = parseCourseColor(asignatura.color)
                val style = material.tipo.style()

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(176.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Navy950),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            MaterialDetailIcon(tipo = material.tipo)
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = style.detailLabel,
                                style = MaterialTheme.typography.titleSmall,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (material.tamano != null) {
                                Text(
                                    text = material.tamano,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Slate400
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = material.titulo,
                        style = MaterialTheme.typography.titleLarge,
                        color = Navy950
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StatusPill(
                            text = asignatura.codigo,
                            background = colorAsignatura.copy(alpha = 0.12f),
                            textColor = colorAsignatura
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Unidad ${unidad.numero} · ${unidad.titulo}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Slate400
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Surface(
                        color = AppBackground,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Slate200),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            SectionLabel(text = "Descripción")
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = material.descripcion,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Slate600
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Slate200),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            FilaMetadato(label = "Docente", valor = asignatura.nombreDocente)
                            Spacer(modifier = Modifier.height(8.dp))
                            FilaMetadato(label = "Publicado", valor = material.subidoEl)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    PrimaryActionButton(
                        text = when {
                            material.descargado -> "Abrir archivo descargado"
                            vistaAbierta -> "Cerrar vista previa"
                            material.tipo == TipoMaterial.VIDEO -> "Reproducir material"
                            else -> "Abrir material"
                        },
                        onClick = {
                            if (material.descargado) {
                                viewModel.abrirMaterial(material.id)
                            } else {
                                vistaAbierta = !vistaAbierta
                            }
                        },
                        containerColor = Navy950
                    )

                    if (vistaAbierta) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            color = Color.White,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, Slate200),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "Vista previa del recurso",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Navy950
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (material.tipo == TipoMaterial.VIDEO) {
                                        "Reproduciendo clase en video en modo demostración."
                                    } else {
                                        "Documento abierto en el visor del prototipo."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Slate600
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = { viewModel.alternarDescarga(material.id) },
                        enabled = !uiState.descargando,
                        border = BorderStroke(
                            1.dp,
                            if (material.descargado) GreenBorder else Slate300
                        ),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (material.descargado) Green50 else Color.White,
                            contentColor = if (material.descargado) Green700 else Navy950
                        ),
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Icon(
                            imageVector = if (material.descargado) {
                                Icons.Outlined.Check
                            } else {
                                Icons.Outlined.FileDownload
                            },
                            contentDescription = null,
                            tint = if (material.descargado) Green700 else Slate500,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when {
                                uiState.descargando -> "Descargando…"
                                material.descargado -> "Disponible sin conexión · Quitar"
                                else -> "Descargar para uso sin conexión"
                            },
                            style = MaterialTheme.typography.labelLarge
                        )
                    }

                    if (uiState.error != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = uiState.error.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = Red600
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (material.estudiado) {
                        ChipRevisado()
                    } else {
                        PrimaryActionButton(
                            text = "Marcar como Estudiado",
                            onClick = { viewModel.marcarEstudiado(material.id) }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = { onAskTeacher(asignatura.id, unidad.id, material.id) },
                        border = BorderStroke(1.dp, Blue200),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Blue50,
                            contentColor = Blue700
                        ),
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Forum,
                            contentDescription = null,
                            tint = Blue700,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Consultar al Docente",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun FilaMetadato(label: String, valor: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Slate500,
            modifier = Modifier.width(88.dp)
        )
        Text(
            text = valor,
            style = MaterialTheme.typography.bodySmall,
            color = Navy950
        )
    }
}

@Composable
private fun ChipRevisado() {
    Surface(
        color = Green50,
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(1.dp, GreenBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = null,
                tint = Green700,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Material Revisado",
                style = MaterialTheme.typography.labelLarge,
                color = Green700
            )
        }
    }
}
