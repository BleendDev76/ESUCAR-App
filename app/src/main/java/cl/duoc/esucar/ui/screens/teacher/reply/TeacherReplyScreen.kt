package cl.duoc.esucar.ui.screens.teacher.reply

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cl.duoc.esucar.core.time.formatLongDateTime
import cl.duoc.esucar.ui.components.AppHeader
import cl.duoc.esucar.ui.components.EmptyState
import cl.duoc.esucar.ui.components.InfoBanner
import cl.duoc.esucar.ui.components.PrimaryActionButton
import cl.duoc.esucar.ui.components.StatusPill
import cl.duoc.esucar.ui.components.esucarTextFieldColors
import cl.duoc.esucar.ui.theme.AppBackground
import cl.duoc.esucar.ui.theme.Blue100
import cl.duoc.esucar.ui.theme.Blue700
import cl.duoc.esucar.ui.theme.Green100
import cl.duoc.esucar.ui.theme.Green50
import cl.duoc.esucar.ui.theme.Green700
import cl.duoc.esucar.ui.theme.GreenBorder
import cl.duoc.esucar.ui.theme.GreenDark
import cl.duoc.esucar.ui.theme.Navy950
import cl.duoc.esucar.ui.theme.Red600
import cl.duoc.esucar.ui.theme.Slate200
import cl.duoc.esucar.ui.theme.Slate400
import cl.duoc.esucar.ui.theme.Slate500
import cl.duoc.esucar.ui.theme.Slate600

@Composable
fun TeacherReplyScreen(
    viewModel: TeacherReplyViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val consulta = uiState.consulta

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
    ) {
        AppHeader(
            titulo = "Responder Consulta",
            subtitle = consulta?.nombreEstudiante ?: "Consulta",
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

            consulta == null -> {
                EmptyState(
                    icon = Icons.Outlined.TaskAlt,
                    titulo = "Consulta no encontrada",
                    message = "La consulta que intentas responder ya no está disponible."
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
                    InfoBanner(
                        titulo = "${consulta.nombreAsignatura} · ${consulta.tituloUnidad}",
                        subtitle = consulta.tituloMaterial?.let { "Material: $it" }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Surface(
                        color = Color.White,
                        shape = MaterialTheme.shapes.medium,
                        border = BorderStroke(1.dp, Slate200)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Blue100),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Person,
                                        contentDescription = null,
                                        tint = Blue700,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = consulta.nombreEstudiante,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = Navy950
                                    )
                                    Text(
                                        text = formatLongDateTime(consulta.fecha),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Slate400
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = consulta.asunto,
                                style = MaterialTheme.typography.titleSmall,
                                color = Navy950
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = consulta.contenido,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Slate600
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Surface(
                        color = Green50,
                        shape = MaterialTheme.shapes.medium,
                        border = BorderStroke(1.dp, GreenBorder)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Green100),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Check,
                                        contentDescription = null,
                                        tint = Green700,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Tu respuesta",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = GreenDark
                                    )
                                    if (uiState.enviada && consulta.fechaRespuesta != null) {
                                        Text(
                                            text = formatLongDateTime(consulta.fechaRespuesta),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Slate500
                                        )
                                    }
                                }
                                if (uiState.enviada) {
                                    StatusPill(
                                        text = "Respuesta enviada",
                                        background = Green100,
                                        textColor = GreenDark
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            if (uiState.enviada) {
                                Surface(
                                    color = Green100,
                                    shape = MaterialTheme.shapes.small,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = consulta.respuesta.orEmpty(),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = GreenDark,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            } else {
                                OutlinedTextField(
                                    value = uiState.textoRespuesta,
                                    onValueChange = viewModel::onReplyChange,
                                    placeholder = {
                                        Text(
                                            text = "Escribe tu respuesta aquí. Sé claro y detallado para ayudar al estudiante a comprender el tema...",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Slate400
                                        )
                                    },
                                    minLines = 6,
                                    maxLines = 9,
                                    colors = esucarTextFieldColors(),
                                    shape = MaterialTheme.shapes.small,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            if (uiState.error != null) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = uiState.error.orEmpty(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Red600
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            if (uiState.enviada) {
                                Button(
                                    onClick = onBack,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.White,
                                        contentColor = Green700
                                    ),
                                    border = BorderStroke(1.dp, GreenBorder),
                                    shape = MaterialTheme.shapes.small,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                ) {
                                    Text(text = "Volver a la bandeja")
                                }
                            } else {
                                PrimaryActionButton(
                                    text = if (uiState.enviando) "Enviando..." else "Enviar Respuesta",
                                    onClick = viewModel::sendReply,
                                    enabled = uiState.puedeEnviar
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}
