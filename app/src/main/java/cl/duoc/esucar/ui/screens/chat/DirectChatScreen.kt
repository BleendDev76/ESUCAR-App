package cl.duoc.esucar.ui.screens.chat

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cl.duoc.esucar.data.local.entity.DirectMessageEntity
import cl.duoc.esucar.core.time.formatShortDateTime
import cl.duoc.esucar.domain.model.RolUsuario
import cl.duoc.esucar.ui.theme.AppBackground
import cl.duoc.esucar.ui.theme.Green50
import cl.duoc.esucar.ui.theme.Green700
import cl.duoc.esucar.ui.theme.Navy950
import kotlinx.coroutines.delay

@Composable
fun DirectChatScreen(
    viewModel: DirectChatViewModel,
    role: RolUsuario,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    var draft by remember { mutableStateOf("") }

    LaunchedEffect(state.sendVersion) {
        if (state.sendVersion > 0) draft = ""
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(8_000)
            viewModel.refresh()
        }
    }

    val ownMessages = state.messages.filter {
        if (role == RolUsuario.DOCENTE) it.teacherId == state.userId
        else it.studentId == state.userId
    }
    val selectedId = state.selectedOtherId
    val thread = ownMessages.filter {
        if (role == RolUsuario.DOCENTE) it.studentId == selectedId
        else it.teacherId == selectedId
    }

    Column(
        modifier = Modifier.fillMaxSize().background(AppBackground).statusBarsPadding().padding(16.dp)
    ) {
        if (selectedId == null) {
            Text("Chat directo", style = MaterialTheme.typography.headlineMedium, color = Navy950)
            Text(
                if (role == RolUsuario.ESTUDIANTE) "Elige el docente de una de tus asignaturas."
                else "Conversaciones directas con tus estudiantes.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(12.dp))
            if (state.loading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Green700)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (role == RolUsuario.ESTUDIANTE) {
                        val teachers = state.courses.mapNotNull { item ->
                            item.course.teacherId?.let { id ->
                                Triple(item.course.id, id, item.course.name to item.course.teacherName)
                            }
                        }.distinctBy { it.first }
                        if (teachers.isEmpty()) {
                            item {
                                Text("Aún no hay docentes asignados a tus asignaturas.")
                            }
                        }
                        item { Text("Iniciar o continuar una conversación", color = Green700) }
                        items(teachers, key = { it.first }) { (_, teacherId, subjectAndTeacher) ->
                            val (courseName, teacherName) = subjectAndTeacher
                            val latestMessage = ownMessages.lastOrNull { it.teacherId == teacherId }?.content
                            ContactCard(
                                title = teacherName.ifBlank { "Docente" },
                                subtitle = "Materia: $courseName" + (latestMessage?.let { " · $it" } ?: " · Iniciar chat directo"),
                                onClick = { viewModel.abrirChat(teacherId) }
                            )
                        }
                    } else {
                        val students = ownMessages.groupBy { it.studentId }
                            .values.map { it.last() }
                            .sortedByDescending { it.timestamp }
                        if (students.isEmpty()) {
                            item { Text("Todavía no tienes mensajes directos de estudiantes.") }
                        }
                        items(students, key = { it.studentId }) { message ->
                            ContactCard(
                                title = message.studentName.ifBlank { "Estudiante" },
                                subtitle = message.content,
                                onClick = { viewModel.abrirChat(message.studentId) }
                            )
                        }
                    }
                }
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { draft = ""; viewModel.volverAConversaciones() }) {
                    Text("← Conversaciones")
                }
                Text(
                    if (role == RolUsuario.DOCENTE) thread.firstOrNull()?.studentName ?: "Estudiante"
                    else "Docente",
                    style = MaterialTheme.typography.titleLarge,
                    color = Navy950
                )
            }
            Text("Mensajes privados entre alumno y docente", style = MaterialTheme.typography.bodySmall)
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(thread, key = { it.id }) { message ->
                    ChatBubble(
                        message = message,
                        isMine = message.senderId == state.userId,
                        role = role
                    )
                }
                if (thread.isEmpty()) {
                    item { Text("Escribe un mensaje para iniciar la conversación.") }
                }
            }
            state.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(bottom = 8.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it.take(4000) },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Escribe tu mensaje") },
                    maxLines = 4
                )
                Button(
                    enabled = draft.isNotBlank() && !state.sending,
                    onClick = {
                        viewModel.enviar(draft)
                    }
                ) { Text(if (state.sending) "…" else "Enviar") }
            }
        }
        if (selectedId == null) {
            state.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
            }
            TextButton(onClick = onBack) { Text("Volver") }
        }
    }
}

@Composable
private fun ContactCard(title: String, subtitle: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = Navy950)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, maxLines = 2)
        }
    }
}

@Composable
private fun ChatBubble(message: DirectMessageEntity, isMine: Boolean, role: RolUsuario) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            color = if (isMine) Green50 else MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(0.86f)
        ) {
            Column(Modifier.padding(12.dp)) {
                Text(
                    when {
                        isMine -> "Tú"
                        role == RolUsuario.DOCENTE -> message.studentName.ifBlank { "Estudiante" }
                        else -> "Docente"
                    },
                    color = Green700,
                    style = MaterialTheme.typography.labelSmall)
                Text(message.content, color = Navy950, style = MaterialTheme.typography.bodyMedium)
                Text(formatShortDateTime(message.timestamp), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
