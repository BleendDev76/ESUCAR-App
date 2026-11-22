package cl.duoc.esucar.ui.screens.teacher.editor

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cl.duoc.esucar.data.remote.ArchivoMaterial
import cl.duoc.esucar.domain.model.TipoMaterial
import cl.duoc.esucar.ui.components.AppHeader
import cl.duoc.esucar.ui.components.DropdownField
import cl.duoc.esucar.ui.components.PrimaryActionButton
import cl.duoc.esucar.ui.components.esucarTextFieldColors
import cl.duoc.esucar.ui.theme.AppBackground
import cl.duoc.esucar.ui.theme.Green50
import cl.duoc.esucar.ui.theme.Green700
import cl.duoc.esucar.ui.theme.Navy950
import cl.duoc.esucar.ui.theme.Red600
import cl.duoc.esucar.ui.theme.Slate400
import cl.duoc.esucar.ui.theme.Slate300
import cl.duoc.esucar.ui.theme.Slate500
import cl.duoc.esucar.ui.theme.Slate600

@Composable
fun TeacherEditorScreen(
    viewModel: TeacherEditorViewModel,
    onBack: () -> Unit,
    onBackToMaterials: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val editando = uiState.editando
    val context = LocalContext.current

    val selectorArchivo = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            viewModel.onFileSelected(ArchivoMaterial.desde(context, uri))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
    ) {
        AppHeader(
            titulo = if (editando) "Editar Material" else "Publicar Material",
            subtitle = if (editando) {
                "Actualiza los datos del recurso"
            } else {
                "Añade un recurso a una unidad"
            },
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

            uiState.guardado -> {
                EditorSuccessContent(
                    editando = editando,
                    onBackToMaterials = onBackToMaterials
                )
            }

            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .imePadding()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    DropdownField(
                        label = "Asignatura",
                        selectedText = uiState.asignaturas
                            .firstOrNull { it.id == uiState.selectedCourseId }
                            ?.nombre,
                        options = uiState.asignaturas,
                        optionLabel = { it.nombre },
                        onOptionSelected = { viewModel.onCourseSelected(it.id) },
                        enabled = !editando && uiState.asignaturas.isNotEmpty()
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    DropdownField(
                        label = "Unidad temática",
                        selectedText = uiState.unidades
                            .firstOrNull { it.id == uiState.selectedUnitId }
                            ?.let { "Unidad ${it.numero}: ${it.titulo}" },
                        options = uiState.unidades,
                        optionLabel = { "Unidad ${it.numero}: ${it.titulo}" },
                        onOptionSelected = { viewModel.onUnitSelected(it.id) },
                        enabled = !editando &&
                            uiState.selectedCourseId != null &&
                            uiState.unidades.isNotEmpty()
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = uiState.titulo,
                        onValueChange = viewModel::onTitleChange,
                        label = { Text(text = "Título del recurso") },
                        placeholder = {
                            Text(
                                text = "Ej: Guía de repaso Unidad 2",
                                style = MaterialTheme.typography.bodySmall,
                                color = Slate400
                            )
                        },
                        singleLine = true,
                        colors = esucarTextFieldColors(),
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    DropdownField(
                        label = "Tipo",
                        selectedText = materialTypeLabel(uiState.tipo),
                        options = TipoMaterial.entries,
                        optionLabel = { materialTypeLabel(it) },
                        onOptionSelected = viewModel::onTypeSelected
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = uiState.tamano,
                        onValueChange = viewModel::onSizeChange,
                        label = { Text(text = "Tamaño") },
                        placeholder = {
                            Text(
                                text = "Ej: 2.4 MB",
                                style = MaterialTheme.typography.bodySmall,
                                color = Slate400
                            )
                        },
                        singleLine = true,
                        colors = esucarTextFieldColors(),
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = uiState.descripcion,
                        onValueChange = viewModel::onDescriptionChange,
                        label = { Text(text = "Descripción") },
                        placeholder = {
                            Text(
                                text = "Describe el contenido y propósito del material...",
                                style = MaterialTheme.typography.bodySmall,
                                color = Slate400
                            )
                        },
                        minLines = 5,
                        maxLines = 8,
                        colors = esucarTextFieldColors(),
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedButton(
                        onClick = { selectorArchivo.launch(arrayOf("*/*")) },
                        border = BorderStroke(1.dp, Slate300),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.White,
                            contentColor = Navy950
                        ),
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AttachFile,
                            contentDescription = null,
                            tint = Slate500,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = uiState.archivo?.nombre ?: "Adjuntar archivo (PDF, video…)",
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1
                        )
                    }

                    uiState.archivo?.let { archivo ->
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "${archivo.tamanoLegible} · se subirá a Supabase Storage",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate500
                        )
                    }

                    if (uiState.error != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = uiState.error.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = Red600
                        )
                    }
                    Spacer(modifier = Modifier.height(22.dp))

                    PrimaryActionButton(
                        text = if (editando) "Guardar cambios" else "Publicar y notificar",
                        onClick = viewModel::save,
                        enabled = uiState.puedeGuardar
                    )

                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}

@Composable
private fun EditorSuccessContent(
    editando: Boolean,
    onBackToMaterials: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(84.dp)
                .clip(CircleShape)
                .background(Green50),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = null,
                tint = Green700,
                modifier = Modifier.size(42.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = if (editando) "Material actualizado" else "Material publicado",
            style = MaterialTheme.typography.headlineMedium,
            color = Navy950,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (editando) {
                "Los cambios ya están disponibles para los estudiantes."
            } else {
                "Los estudiantes recibieron una notificación del nuevo recurso."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = Slate500,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(28.dp))

        Button(
            onClick = onBackToMaterials,
            colors = ButtonDefaults.buttonColors(containerColor = Green700),
            shape = MaterialTheme.shapes.small,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text(
                text = "Volver a materiales",
                color = Color.White
            )
        }
    }
}

private fun materialTypeLabel(tipo: TipoMaterial): String = when (tipo) {
    TipoMaterial.PDF -> "Documento PDF"
    TipoMaterial.GUIA -> "Guía"
    TipoMaterial.PRESENTACION -> "Presentación"
    TipoMaterial.VIDEO -> "Video enlace"
}
