package cl.duoc.esucar.ui.screens.teacher.inbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import cl.duoc.esucar.data.repository.StudyRepository
import cl.duoc.esucar.domain.model.Asignatura
import cl.duoc.esucar.domain.model.Mensaje
import cl.duoc.esucar.domain.model.RolUsuario
import cl.duoc.esucar.domain.model.Unidad
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class TeacherInboxUiState(
    val asignaturas: List<Asignatura> = emptyList(),
    val selectedCourseId: String? = null,
    val selectedUnitId: String? = null,
    val unidades: List<Unidad> = emptyList(),
    val pending: List<Mensaje> = emptyList(),
    val answered: List<Mensaje> = emptyList(),
    val pendingCount: Int = 0,
    val answeredCount: Int = 0,
    val totalCount: Int = 0,
    val noLeidas: Int = 0,
    val cargando: Boolean = true
)

class TeacherInboxViewModel(
    private val repository: StudyRepository
) : ViewModel() {

    private val courseFilter = MutableStateFlow<String?>(null)
    private val unitFilter = MutableStateFlow<String?>(null)

    val uiState: StateFlow<TeacherInboxUiState> = combine(
        repository.observarAsignaturas(),
        repository.observarMensajes(),
        repository.observarNoLeidas(RolUsuario.DOCENTE),
        courseFilter,
        unitFilter
    ) { asignaturas, mensajes, noLeidas, selectedCourseId, selectedUnitId ->
        val unidades = asignaturas.firstOrNull { it.id == selectedCourseId }?.unidades ?: emptyList()
        val filtrados = mensajes.filter { consulta ->
            (selectedCourseId == null || consulta.asignaturaId == selectedCourseId) &&
                (selectedUnitId == null || consulta.unidadId == selectedUnitId)
        }
        val pendientes = filtrados.filter { it.respuesta == null }
        val respondidas = filtrados.filter { it.respuesta != null }

        TeacherInboxUiState(
            asignaturas = asignaturas,
            selectedCourseId = selectedCourseId,
            selectedUnitId = selectedUnitId,
            unidades = unidades,
            pending = pendientes,
            answered = respondidas,
            pendingCount = pendientes.size,
            answeredCount = respondidas.size,
            totalCount = filtrados.size,
            noLeidas = noLeidas,
            cargando = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TeacherInboxUiState()
    )

    fun onCourseSelected(asignaturaId: String?) {
        courseFilter.value = asignaturaId
        unitFilter.value = null
    }

    fun onUnitSelected(unidadId: String?) {
        unitFilter.value = unidadId
    }

    companion object {
        fun fabrica(repository: StudyRepository): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { TeacherInboxViewModel(repository) }
            }
    }
}
