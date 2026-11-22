package cl.duoc.esucar.ui.screens.teacher.materials

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
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cl.duoc.esucar.domain.model.Material
import cl.duoc.esucar.domain.model.TipoMaterial
import cl.duoc.esucar.ui.components.style
import cl.duoc.esucar.ui.components.ConfirmationDialog
import cl.duoc.esucar.ui.components.EmptyState
import cl.duoc.esucar.ui.components.HeaderActionButton
import cl.duoc.esucar.ui.components.InfoBanner
import cl.duoc.esucar.ui.components.TeacherPanelHeader
import cl.duoc.esucar.ui.theme.AppBackground
import cl.duoc.esucar.ui.theme.Blue700
import cl.duoc.esucar.ui.theme.Green700
import cl.duoc.esucar.ui.theme.Navy950
import cl.duoc.esucar.ui.theme.Red600
import cl.duoc.esucar.ui.theme.Slate100
import cl.duoc.esucar.ui.theme.Slate200
import cl.duoc.esucar.ui.theme.Slate400
import cl.duoc.esucar.ui.theme.Slate500
import cl.duoc.esucar.ui.theme.Slate600
import cl.duoc.esucar.ui.util.parseCourseColor

@Composable
fun TeacherMaterialsScreen(
    viewModel: TeacherMaterialsViewModel,
    onPublish: () -> Unit,
    onEdit: (asignaturaId: String, unidadId: String, materialId: String) -> Unit,
    onNotifications: () -> Unit,
    onLogout: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var materialAEliminar by remember { mutableStateOf<Material?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
    ) {
        TeacherPanelHeader(
            label = "Panel Docente",
            titulo = "Gestión de Materiales",
            unreadCount = uiState.noLeidas,
            onNotifications = onNotifications,
            onLogout = onLogout,
            action = {
                HeaderActionButton(
                    text = "+ Publicar",
                    onClick = onPublish,
                    modifier = Modifier.fillMaxWidth()
                )
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
                InfoBanner(
                    titulo = "${uiState.totalMateriales} materiales publicados en ${uiState.asignaturas.size} asignaturas"
                )

                if (uiState.error != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = uiState.error.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = Red600
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (uiState.totalMateriales == 0) {
                    EmptyState(
                        icon = Icons.Outlined.Description,
                        titulo = "Sin materiales publicados",
                        message = "Publica el primer recurso para que los estudiantes puedan consultarlo.",
                        actionLabel = "Publicar material",
                        onAction = onPublish
                    )
                } else {
                    uiState.asignaturas.forEach { asignatura ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(parseCourseColor(asignatura.color))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${asignatura.codigo} · ${asignatura.nombre}",
                                style = MaterialTheme.typography.titleSmall,
                                color = Navy950
                            )
                        }

                        asignatura.unidades.forEach { unidad ->
                            Surface(
                                color = Color.White,
                                shape = MaterialTheme.shapes.medium,
                                border = BorderStroke(1.dp, Slate200),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 10.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Unidad ${unidad.numero}: ${unidad.titulo}",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = Navy950,
                                            modifier = Modifier.weight(1f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "${unidad.materiales.size} materiales",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Slate400
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    if (unidad.materiales.isEmpty()) {
                                        Text(
                                            text = "Esta unidad aún no tiene materiales.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Slate500,
                                            modifier = Modifier.padding(vertical = 8.dp)
                                        )
                                    } else {
                                        unidad.materiales.forEachIndexed { index, material ->
                                            if (index > 0) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(1.dp)
                                                        .background(Slate100)
                                                )
                                            }
                                            MaterialManagementRow(
                                                material = material,
                                                onEdit = {
                                                    onEdit(asignatura.id, unidad.id, material.id)
                                                },
                                                onDelete = { materialAEliminar = material }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }

    materialAEliminar?.let { material ->
        ConfirmationDialog(
            titulo = "Eliminar material",
            message = "¿Eliminar “${material.titulo}”?",
            confirmLabel = "Eliminar",
            onConfirm = {
                viewModel.eliminarMaterial(material.id)
                materialAEliminar = null
            },
            onDismiss = { materialAEliminar = null }
        )
    }
}

@Composable
private fun MaterialManagementRow(
    material: Material,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val typeStyle = material.tipo.style()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = typeStyle.listLabel.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = Green700,
            fontWeight = FontWeight.Bold,
            fontSize = 9.sp,
            modifier = Modifier.width(64.dp)
        )
        Text(
            text = material.titulo,
            style = MaterialTheme.typography.bodySmall,
            color = Navy950,
            fontSize = 11.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "Editar",
            style = MaterialTheme.typography.labelSmall,
            color = Blue700,
            modifier = Modifier
                .heightIn(min = 48.dp)
                .clickable(onClick = onEdit)
                .padding(horizontal = 6.dp, vertical = 4.dp)
        )
        Text(
            text = "Eliminar",
            style = MaterialTheme.typography.labelSmall,
            color = Red600,
            modifier = Modifier
                .heightIn(min = 48.dp)
                .clickable(onClick = onDelete)
                .padding(horizontal = 6.dp, vertical = 4.dp)
        )
    }
}
