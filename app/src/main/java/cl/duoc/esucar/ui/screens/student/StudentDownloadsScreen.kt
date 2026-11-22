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
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cl.duoc.esucar.ui.components.AppHeader
import cl.duoc.esucar.ui.components.EmptyState
import cl.duoc.esucar.ui.components.GreenInfoBanner
import cl.duoc.esucar.ui.components.MaterialTypeIcon
import cl.duoc.esucar.ui.components.StatusPill
import cl.duoc.esucar.ui.theme.AppBackground
import cl.duoc.esucar.ui.theme.Green50
import cl.duoc.esucar.ui.theme.Green700
import cl.duoc.esucar.ui.theme.GreenBorder
import cl.duoc.esucar.ui.theme.Navy950
import cl.duoc.esucar.ui.theme.Red600
import cl.duoc.esucar.ui.theme.Slate500

@Composable
fun StudentDownloadsScreen(
    viewModel: StudentViewModel,
    onMaterialClick: (asignaturaId: String, unidadId: String, materialId: String) -> Unit,
    onExplore: () -> Unit,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
    ) {
        AppHeader(
            titulo = "Mis Descargas",
            subtitle = if (uiState.descargas.size == 1) {
                "1 recurso disponible sin conexión"
            } else {
                "${uiState.descargas.size} recursos disponibles sin conexión"
            },
            onBack = onBack
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
                GreenInfoBanner(
                    text = "Estos recursos permanecen disponibles aunque pierdas la conexión."
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (uiState.descargas.isEmpty()) {
                    EmptyState(
                        icon = Icons.Outlined.FileDownload,
                        titulo = "Aún no tienes descargas",
                        message = "Abre un material y selecciona “Descargar para uso sin conexión”.",
                        actionLabel = "Explorar materiales",
                        onAction = onExplore
                    )
                } else {
                    uiState.descargas.forEach { item ->
                        Surface(
                            color = Color.White,
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, GreenBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onMaterialClick(
                                        item.asignatura.id,
                                        item.unidad.id,
                                        item.material.id
                                    )
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                MaterialTypeIcon(
                                    tipo = item.material.tipo,
                                    size = 40.dp
                                )

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.material.titulo,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = Navy950,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${item.asignatura.nombre} · Unidad ${item.unidad.numero}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Slate500,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    StatusPill(
                                        text = "Offline",
                                        background = Green50,
                                        textColor = Green700
                                    )
                                }

                                TextButton(
                                    onClick = { viewModel.abrirMaterial(item.material.id) }
                                ) {
                                    Text(text = "Abrir", color = Green700)
                                }

                                TextButton(
                                    onClick = { viewModel.alternarDescarga(item.material.id) }
                                ) {
                                    Text(text = "Quitar", color = Red600)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}
