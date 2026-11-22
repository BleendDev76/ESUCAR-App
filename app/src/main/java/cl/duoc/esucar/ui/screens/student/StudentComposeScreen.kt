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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cl.duoc.esucar.core.time.formatShortDateTime
import cl.duoc.esucar.domain.model.Material
import cl.duoc.esucar.domain.model.Mensaje
import cl.duoc.esucar.ui.components.AppHeader
import cl.duoc.esucar.ui.components.DropdownField
import cl.duoc.esucar.ui.components.InfoBanner
import cl.duoc.esucar.ui.components.PrimaryActionButton
import cl.duoc.esucar.ui.components.SectionLabel
import cl.duoc.esucar.ui.components.esucarTextFieldColors
import cl.duoc.esucar.ui.theme.Amber50
import cl.duoc.esucar.ui.theme.Amber200
import cl.duoc.esucar.ui.theme.Amber800
import cl.duoc.esucar.ui.theme.AppBackground
import cl.duoc.esucar.ui.theme.Blue100
import cl.duoc.esucar.ui.theme.Blue700
import cl.duoc.esucar.ui.theme.Green100
import cl.duoc.esucar.ui.theme.Green50
import cl.duoc.esucar.ui.theme.Green700
import cl.duoc.esucar.ui.theme.GreenBorder
import cl.duoc.esucar.ui.theme.Navy950
import cl.duoc.esucar.ui.theme.Red600
import cl.duoc.esucar.ui.theme.Slate200
import cl.duoc.esucar.ui.theme.Slate500

private const val GENERAL_MATERIAL_LABEL = "Consulta general (sin material)"

@Composable
fun StudentComposeScreen(
    viewModel: StudentViewModel,
    initialCourseId: String?,
    initialUnitId: String?,
    initialMaterialId: String?,
    messageId: String? = null,
    onVerConsultas: () -> Unit,
    onBack: () -> Unit
) {
    if (messageId != null) {
        DetalleConsulta(
            viewModel = viewModel,
            messageId = messageId,
            onBack = onBack
        )
        return
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    DisposableEffect(Unit) {
        onDispose { viewModel.reiniciarEstadoConsulta() }
    }

    if (uiState.consultaEnviada) {
        ConsultaEnviadaContent(onVerConsultas = onVerConsultas)
        return
    }

    FormularioConsulta(
        viewModel = viewModel,
        uiState = uiState,
        initialCourseId = initialCourseId,
        initialUnitId = initialUnitId,
        initialMaterialId = initialMaterialId,
        onBack = onBack
    )
}

@Composable
private fun FormularioConsulta(
    viewModel: StudentViewModel,
    uiState: StudentUiState,
    initialCourseId: String?,
    initialUnitId: String?,
    initialMaterialId: String?,
    onBack: () -> Unit
) {
    var selectedCourseId by rememberSaveable { mutableStateOf(initialCourseId.orEmpty()) }
    var selectedUnitId by rememberSaveable { mutableStateOf(initialUnitId.orEmpty()) }
    var selectedMaterialId by rememberSaveable { mutableStateOf(initialMaterialId.orEmpty()) }
    var asunto by rememberSaveable { mutableStateOf("") }
    var contenido by rememberSaveable { mutableStateOf("") }

    val asignaturas = uiState.asignaturas
    val asignaturaSeleccionada = asignaturas.firstOrNull { it.id == selectedCourseId }
    val unidadSeleccionada = asignaturaSeleccionada?.unidades?.firstOrNull { it.id == selectedUnitId }
    val materialSeleccionado = unidadSeleccionada?.materiales?.firstOrNull { it.id == selectedMaterialId }

    LaunchedEffect(asignaturas) {
        if (selectedCourseId.isBlank() && asignaturas.isNotEmpty()) {
            selectedCourseId = asignaturas.first().id
            selectedUnitId = asignaturas.first().unidades.firstOrNull()?.id.orEmpty()
        }
    }

    LaunchedEffect(selectedCourseId, asignaturas) {
        val asignatura = asignaturas.firstOrNull { it.id == selectedCourseId } ?: return@LaunchedEffect
        if (asignatura.unidades.none { it.id == selectedUnitId }) {
            selectedUnitId = asignatura.unidades.firstOrNull()?.id.orEmpty()
            selectedMaterialId = ""
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
    ) {
        AppHeader(
            titulo = "Nueva Consulta",
            subtitle = "El docente recibirá tu mensaje",
            onBack = onBack
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            SectionLabel(text = "Contexto")
            Spacer(modifier = Modifier.height(10.dp))

            DropdownField(
                label = "Asignatura",
                selectedText = asignaturaSeleccionada?.nombre,
                options = asignaturas,
                optionLabel = { it.nombre },
                onOptionSelected = {
                    selectedCourseId = it.id
                    selectedUnitId = ""
                    selectedMaterialId = ""
                }
            )

            Spacer(modifier = Modifier.height(14.dp))

            DropdownField(
                label = "Unidad",
                selectedText = unidadSeleccionada?.let { "Unidad ${it.numero}: ${it.titulo}" },
                options = asignaturaSeleccionada?.unidades.orEmpty(),
                optionLabel = { "Unidad ${it.numero}: ${it.titulo}" },
                onOptionSelected = {
                    selectedUnitId = it.id
                    selectedMaterialId = ""
                },
                enabled = asignaturaSeleccionada != null
            )

            Spacer(modifier = Modifier.height(14.dp))

            if (materialSeleccionado != null) {
                InfoBanner(
                    titulo = "Recurso vinculado: ${materialSeleccionado?.titulo.orEmpty()}"
                )
            } else {
                DropdownField<Material?>(
                    label = "Material (opcional)",
                    selectedText = materialSeleccionado?.titulo ?: GENERAL_MATERIAL_LABEL,
                    options = listOf<Material?>(null) + unidadSeleccionada?.materiales.orEmpty(),
                    optionLabel = { it?.titulo ?: GENERAL_MATERIAL_LABEL },
                    onOptionSelected = { selectedMaterialId = it?.id.orEmpty() },
                    enabled = unidadSeleccionada != null
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
            SectionLabel(text = "Mensaje")
            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = asunto,
                onValueChange = { asunto = it },
                label = { Text(text = "Asunto") },
                placeholder = { Text(text = "Ej: Dudas sobre el ejercicio 3") },
                singleLine = true,
                shape = MaterialTheme.shapes.small,
                colors = esucarTextFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = contenido,
                onValueChange = { contenido = it },
                label = { Text(text = "Consulta") },
                placeholder = { Text(text = "Describe tu duda con el mayor detalle posible") },
                minLines = 5,
                shape = MaterialTheme.shapes.small,
                colors = esucarTextFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )

            if (uiState.error != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = uiState.error.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = Red600
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            PrimaryActionButton(
                text = if (uiState.enviando) "Enviando..." else "Enviar Consulta",
                onClick = {
                    viewModel.enviarConsulta(
                        asignaturaId = selectedCourseId,
                        unidadId = selectedUnitId,
                        materialId = selectedMaterialId.ifBlank { null },
                        asunto = asunto,
                        contenido = contenido
                    )
                },
                enabled = !uiState.enviando && asignaturaSeleccionada != null && unidadSeleccionada != null
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Recibirás la respuesta del docente en la sección Consultas.",
                style = MaterialTheme.typography.bodySmall,
                color = Slate500
            )

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun ConsultaEnviadaContent(onVerConsultas: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(Green100),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = null,
                tint = Green700,
                modifier = Modifier.size(44.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "¡Consulta enviada!",
            style = MaterialTheme.typography.headlineSmall,
            color = Navy950,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "El docente recibirá tu consulta y responderá a la brevedad.",
            style = MaterialTheme.typography.bodyMedium,
            color = Slate500,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(28.dp))

        PrimaryActionButton(
            text = "Ver mis consultas",
            onClick = onVerConsultas
        )
    }
}

@Composable
private fun DetalleConsulta(
    viewModel: StudentViewModel,
    messageId: String,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val consulta by remember(messageId) { viewModel.observarMensaje(messageId) }
        .collectAsStateWithLifecycle(initialValue = null)
    val nombreDocente = uiState.asignaturas
        .firstOrNull { it.id == consulta?.asignaturaId }
        ?.nombreDocente
        ?: "Docente"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
    ) {
        AppHeader(
            titulo = "Detalle de Consulta",
            subtitle = consulta?.nombreAsignatura,
            onBack = onBack
        )

        val mensaje = consulta
        if (mensaje == null) {
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
                TarjetaConsulta(consulta = mensaje)

                Spacer(modifier = Modifier.height(16.dp))

                if (mensaje.respuesta == null) {
                    Surface(
                        color = Amber50,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Amber200),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Tu consulta está pendiente de respuesta del docente.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Amber800,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                } else {
                    TarjetaRespuesta(
                        consulta = mensaje,
                        nombreDocente = nombreDocente
                    )
                }
            }
        }
    }
}

@Composable
private fun TarjetaConsulta(consulta: Mensaje) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Slate200),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = consulta.asunto,
                style = MaterialTheme.typography.titleMedium,
                color = Navy950
            )
            Text(
                text = "${consulta.nombreAsignatura} · ${consulta.tituloUnidad}",
                style = MaterialTheme.typography.labelSmall,
                color = Slate500
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(
                    inicial = consulta.nombreEstudiante,
                    background = Blue100,
                    textColor = Blue700
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${consulta.nombreEstudiante} · ${formatShortDateTime(consulta.fecha)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Slate500
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = consulta.contenido,
                style = MaterialTheme.typography.bodyMedium,
                color = Navy950
            )
        }
    }
}

@Composable
private fun TarjetaRespuesta(consulta: Mensaje, nombreDocente: String) {
    Surface(
        color = Green50,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, GreenBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(
                    inicial = nombreDocente,
                    background = Green100,
                    textColor = Green700
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = listOfNotNull(
                        nombreDocente,
                        consulta.fechaRespuesta?.let(::formatShortDateTime)
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.labelSmall,
                    color = Green700
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Respuesta del docente",
                style = MaterialTheme.typography.labelSmall,
                color = Green700
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = consulta.respuesta.orEmpty(),
                style = MaterialTheme.typography.bodyMedium,
                color = Navy950
            )        }
    }
}

@Composable
private fun Avatar(inicial: String, background: Color, textColor: Color) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(background),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = inicial.trim().take(1).uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = textColor
        )
    }
}
