package cl.duoc.esucar.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material.icons.outlined.Slideshow
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cl.duoc.esucar.domain.model.TipoMaterial
import cl.duoc.esucar.ui.theme.Amber600
import cl.duoc.esucar.ui.theme.Cyan600
import cl.duoc.esucar.ui.theme.Green400
import cl.duoc.esucar.ui.theme.Navy800
import cl.duoc.esucar.ui.theme.Purple600
import cl.duoc.esucar.ui.theme.Red600

data class MaterialTypeStyle(
    val listLabel: String,
    val detailLabel: String,
    val color: Color
)

fun TipoMaterial.style(): MaterialTypeStyle = when (this) {
    TipoMaterial.PDF -> MaterialTypeStyle(
        listLabel = "PDF",
        detailLabel = "Documento PDF",
        color = Red600
    )

    TipoMaterial.VIDEO -> MaterialTypeStyle(
        listLabel = "Video",
        detailLabel = "Clase en Video",
        color = Purple600
    )

    TipoMaterial.PRESENTACION -> MaterialTypeStyle(
        listLabel = "Presentación",
        detailLabel = "Presentación",
        color = Amber600
    )

    TipoMaterial.GUIA -> MaterialTypeStyle(
        listLabel = "Guía",
        detailLabel = "Guía de Ejercicios",
        color = Cyan600
    )
}

fun TipoMaterial.icon(): ImageVector = when (this) {
    TipoMaterial.PDF -> Icons.Outlined.PictureAsPdf
    TipoMaterial.VIDEO -> Icons.Outlined.PlayCircleOutline
    TipoMaterial.PRESENTACION -> Icons.Outlined.Slideshow
    TipoMaterial.GUIA -> Icons.AutoMirrored.Outlined.MenuBook
}

@Composable
fun MaterialTypeIcon(
    tipo: TipoMaterial,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp
) {
    val style = tipo.style()
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
            .background(style.color.copy(alpha = 0.10f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = tipo.icon(),
            contentDescription = style.detailLabel,
            tint = style.color,
            modifier = Modifier.size(size * 0.5f)
        )
    }
}

@Composable
fun MaterialDetailIcon(
    tipo: TipoMaterial,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(64.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Navy800),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = tipo.icon(),
            contentDescription = tipo.style().detailLabel,
            tint = Green400,
            modifier = Modifier.size(32.dp)
        )
    }
}
