package cl.duoc.esucar.ui.screens.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import cl.duoc.esucar.data.local.dao.CourseDao
import cl.duoc.esucar.data.local.dao.DirectMessageDao
import cl.duoc.esucar.data.local.entity.DirectMessageEntity
import cl.duoc.esucar.data.local.relation.CourseWithUnits
import cl.duoc.esucar.data.prefs.SessionStore
import cl.duoc.esucar.data.remote.SupabaseSyncRepository
import cl.duoc.esucar.domain.model.RolUsuario
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DirectChatUiState(
    val messages: List<DirectMessageEntity> = emptyList(),
    val courses: List<CourseWithUnits> = emptyList(),
    val userId: String = "",
    val selectedOtherId: String? = null,
    val loading: Boolean = true,
    val sending: Boolean = false,
    val sendVersion: Int = 0,
    val error: String? = null
)

class DirectChatViewModel(
    private val sync: SupabaseSyncRepository?,
    private val messageDao: DirectMessageDao,
    private val courseDao: CourseDao,
    private val sessionStore: SessionStore,
    private val role: RolUsuario
) : ViewModel() {

    private val _uiState = MutableStateFlow(DirectChatUiState(userId = sessionStore.cachedUserId.orEmpty()))
    val uiState = _uiState.asStateFlow()
    private var syncing = false

    init {
        viewModelScope.launch {
            messageDao.observeAll().collect { messages ->
                _uiState.update { it.copy(messages = messages, loading = false) }
            }
        }
        viewModelScope.launch {
            courseDao.observeCoursesForChat().collect { courses ->
                _uiState.update { it.copy(courses = courses) }
            }
        }
        refresh()
    }

    fun refresh() {
        val repository = sync ?: run {
            _uiState.update { it.copy(loading = false, error = "El chat requiere configurar el servicio online.") }
            return
        }
        if (syncing) return
        viewModelScope.launch {
            syncing = true
            try {
                repository.sincronizarMensajesDirectos()
                _uiState.update { it.copy(userId = sessionStore.cachedUserId.orEmpty(), error = null) }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                _uiState.update { it.copy(error = error.message ?: "No se pudo actualizar el chat.") }
            } finally {
                syncing = false
                _uiState.update { it.copy(loading = false) }
            }
        }
    }

    fun abrirChat(otherUserId: String) {
        _uiState.update { it.copy(selectedOtherId = otherUserId, error = null) }
    }

    fun volverAConversaciones() {
        _uiState.update { it.copy(selectedOtherId = null, error = null) }
    }

    fun enviar(text: String) {
        val content = text.trim()
        val otherId = _uiState.value.selectedOtherId ?: return
        if (content.isBlank() || _uiState.value.sending) return
        val repository = sync ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(sending = true, error = null) }
            try {
                val userId = sessionStore.cachedUserId ?: sessionStore.userId.first()
                    ?: error("No hay una sesión activa.")
                val studentId = if (role == RolUsuario.ESTUDIANTE) userId else otherId
                val teacherId = if (role == RolUsuario.DOCENTE) userId else otherId
                repository.enviarMensajeDirecto(studentId, teacherId, content)
                _uiState.update { it.copy(sendVersion = it.sendVersion + 1) }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                _uiState.update { it.copy(error = error.message ?: "No se pudo enviar el mensaje.") }
            } finally {
                _uiState.update { it.copy(sending = false) }
            }
        }
    }

    companion object {
        fun fabrica(
            sync: SupabaseSyncRepository?,
            messageDao: DirectMessageDao,
            courseDao: CourseDao,
            sessionStore: SessionStore,
            role: RolUsuario
        ) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                DirectChatViewModel(sync, messageDao, courseDao, sessionStore, role) as T
        }
    }
}
