package cl.duoc.esucar.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ExitToApp
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.duoc.esucar.ui.navigation.Routes
import cl.duoc.esucar.ui.theme.Green400
import cl.duoc.esucar.ui.theme.Green50
import cl.duoc.esucar.ui.theme.Green700
import cl.duoc.esucar.ui.theme.Navy800
import cl.duoc.esucar.ui.theme.Navy950
import cl.duoc.esucar.ui.theme.Red500
import cl.duoc.esucar.ui.theme.Slate400
import cl.duoc.esucar.ui.theme.Slate500
import cl.duoc.esucar.ui.theme.Slate100

@Composable
fun AppHeader(
    titulo: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Surface(color = Navy950) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .heightIn(min = 56.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBack != null) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Navy800),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ChevronLeft,
                            contentDescription = "Volver",
                            tint = Green400,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(4.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = titulo,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate400,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            actions()
        }
    }
}

@Composable
fun TeacherPanelHeader(
    label: String,
    titulo: String,
    unreadCount: Int,
    onNotifications: () -> Unit,
    onLogout: () -> Unit,
    action: (@Composable () -> Unit)? = null
) {
    Surface(color = Navy950) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodySmall,
                        color = Green400
                    )
                    Text(
                        text = titulo,
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                NotificationBell(unreadCount = unreadCount, onClick = onNotifications)
                Spacer(modifier = Modifier.width(8.dp))
                HeaderIconButton(
                    icon = Icons.AutoMirrored.Outlined.ExitToApp,
                    contentDescription = "Cerrar sesión",
                    onClick = onLogout
                )
            }

            if (action != null) {
                Spacer(modifier = Modifier.size(12.dp))
                action()
            }
        }
    }
}

@Composable
fun NotificationBell(unreadCount: Int, onClick: () -> Unit) {
    Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Navy800),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Notifications,
                    contentDescription = "Notificaciones",
                    tint = Slate400,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        if (unreadCount > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-6).dp, y = 6.dp)
                    .size(15.dp)
                    .clip(CircleShape)
                    .background(Red500),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (unreadCount > 9) "9+" else unreadCount.toString(),
                    color = Color.White,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun HeaderIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(Navy800),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = Slate400,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun TeacherBottomBar(
    currentRoute: String?,
    onInbox: () -> Unit,
    onMaterials: () -> Unit
) {
    NavigationBar(
        containerColor = Navy950,
        contentColor = Slate400
    ) {
        val colors = NavigationBarItemDefaults.colors(
            selectedIconColor = Green400,
            selectedTextColor = Green400,
            indicatorColor = Color.Transparent,
            unselectedIconColor = Slate500,
            unselectedTextColor = Slate500
        )

        NavigationBarItem(
            selected = currentRoute == Routes.TEACHER_INBOX,
            onClick = onInbox,
            icon = {
                Icon(
                    imageVector = Icons.Outlined.ChatBubbleOutline,
                    contentDescription = "Consultas"
                )
            },
            label = {
                Text(
                    text = "Consultas",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            },
            colors = colors
        )

        NavigationBarItem(
            selected = currentRoute == Routes.TEACHER_MATERIALS,
            onClick = onMaterials,
            icon = {
                Icon(
                    imageVector = Icons.Outlined.Description,
                    contentDescription = "Materiales"
                )
            },
            label = {
                Text(
                    text = "Materiales",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            },
            colors = colors
        )
    }
}

@Composable
fun StudentBottomBar(
    currentRoute: String?,
    onHome: () -> Unit,
    onCourses: () -> Unit,
    onMessages: () -> Unit
) {
    NavigationBar(
        containerColor = Navy950,
        contentColor = Slate400
    ) {
        val colors = NavigationBarItemDefaults.colors(
            selectedIconColor = Green400,
            selectedTextColor = Green400,
            indicatorColor = Color.Transparent,
            unselectedIconColor = Slate500,
            unselectedTextColor = Slate500
        )

        NavigationBarItem(
            selected = currentRoute == Routes.STUDENT_HOME,
            onClick = onHome,
            icon = {
                Icon(
                    imageVector = Icons.Outlined.Home,
                    contentDescription = "Inicio"
                )
            },
            label = {
                Text(text = "Inicio", fontSize = 10.sp, fontWeight = FontWeight.Medium)
            },
            colors = colors
        )

        NavigationBarItem(
            selected = currentRoute == Routes.STUDENT_COURSES,
            onClick = onCourses,
            icon = {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.MenuBook,
                    contentDescription = "Asignaturas"
                )
            },
            label = {
                Text(text = "Asignaturas", fontSize = 10.sp, fontWeight = FontWeight.Medium)
            },
            colors = colors
        )

        NavigationBarItem(
            selected = currentRoute == Routes.STUDENT_MESSAGES,
            onClick = onMessages,
            icon = {
                Icon(
                    imageVector = Icons.Outlined.ChatBubbleOutline,
                    contentDescription = "Consultas"
                )
            },
            label = {
                Text(text = "Consultas", fontSize = 10.sp, fontWeight = FontWeight.Medium)
            },
            colors = colors
        )
    }
}

@Composable
fun HeaderActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Green700,
        contentColor = Color.White,
        shape = MaterialTheme.shapes.small,
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = 48.dp)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = Color.White
            )
        }
    }
}

@Composable
fun HeaderCountPill(text: String) {
    Surface(
        color = Green50,
        contentColor = Green700,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun TopLevelContentBackground(contenido: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Slate100)
    ) {
        contenido()
    }
}
