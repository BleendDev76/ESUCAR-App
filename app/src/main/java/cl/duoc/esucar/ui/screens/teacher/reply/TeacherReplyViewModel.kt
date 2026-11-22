package cl.duoc.esucar.ui.screens.teacher.reply

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import cl.duoc.esucar.data.repository.StudyRepository
import cl.duoc.esucar.domain.model.Mensaje
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TeacherReplyUiState(
    val consulta: Mensaje? = null,
    val textoRespuesta: String = "",
    val enviando: Boolean = false,
    val error: String? = null,
    val cargando: Boolean = true
) {
    val enviada: Boolean = consulta?.respuesta != null
    val puedeEnviar: Boolean =
        consulta != null && !enviada && textoRespuesta.isNotBlank() && !enviando
}

class TeacherReplyViewModel(
    private val repository: StudyRepository,
    private val mensajeId: String
) : ViewModel() {

    private val replyText = MutableStateFlow("")
    private val isSending = MutableStateFlow(false)
    private val error = MutableStateFlow<String?>(null)
    private val loaded = MutableStateFlow(false)

    val uiState: StateFlow<TeacherReplyUiState> = combine(
        repository.observarMensaje(mensajeId).onEach { loaded.value = true },
        replyText,
        isSending,
        error
    ) { consulta, text, sending, currentError ->
        TeacherReplyUiState(
            consulta = consulta,
            textoRespuesta = text,
            enviando = sending,
            error = currentError,
            cargando = !loaded.value
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TeacherReplyUiState()
    )

    fun onReplyChange(value: String) {
        replyText.value = value
        if (error.value != null) error.value = null
    }

    fun sendReply() {
        val consulta = uiState.value.consulta ?: return
        if (consulta.respuesta != null || replyText.value.isBlank() || isSending.value) return

        viewModelScope.launch {
            isSending.value = true
            error.value = null
            runCatching {
                repository.responderConsulta(consulta.id, replyText.value)
            }.onSuccess {
                replyText.value = ""
            }.onFailure { throwable ->
                error.value = throwable.message ?: "No se pudo enviar la respuesta."
            }
            isSending.value = false
        }
    }

    companion object {
        fun fabrica(
            repository: StudyRepository,
            mensajeId: String
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer { TeacherReplyViewModel(repository, mensajeId) }
        }
    }
}
