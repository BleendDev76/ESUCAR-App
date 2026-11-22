package cl.duoc.esucar.ui.screens.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import cl.duoc.esucar.data.repository.StudyRepository
import cl.duoc.esucar.domain.model.Notificacion
import cl.duoc.esucar.domain.model.RolUsuario
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class NotificationsUiState(
    val notificaciones: List<Notificacion> = emptyList(),
    val noLeidas: Int = 0,
    val cargando: Boolean = true
)

class NotificationsViewModel(
    private val repository: StudyRepository,
    private val rol: RolUsuario
) : ViewModel() {

    val uiState: StateFlow<NotificationsUiState> = combine(
        repository.observarNotificaciones(rol),
        repository.observarNoLeidas(rol)
    ) { notificaciones, noLeidas ->
        NotificationsUiState(
            notificaciones = notificaciones,
            noLeidas = noLeidas,
            cargando = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = NotificationsUiState()
    )

    fun marcarLeida(notificacionId: String) {
        viewModelScope.launch {
            repository.marcarNotificacionLeida(notificacionId)
        }
    }

    fun marcarTodasLeidas() {
        viewModelScope.launch {
            repository.marcarTodasLeidas(rol)
        }
    }

    companion object {
        fun fabrica(
            repository: StudyRepository,
            rol: RolUsuario
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer { NotificationsViewModel(repository, rol) }
        }
    }
}
