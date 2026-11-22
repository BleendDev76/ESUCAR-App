package cl.duoc.esucar.ui.screens.teacher.materials

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import cl.duoc.esucar.data.repository.StudyRepository
import cl.duoc.esucar.domain.model.Asignatura
import cl.duoc.esucar.domain.model.RolUsuario
import cl.duoc.esucar.domain.model.todosLosMateriales
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TeacherMaterialsUiState(
    val asignaturas: List<Asignatura> = emptyList(),
    val totalMateriales: Int = 0,
    val noLeidas: Int = 0,
    val error: String? = null,
    val cargando: Boolean = true
)

class TeacherMaterialsViewModel(
    private val repository: StudyRepository
) : ViewModel() {

    private val error = MutableStateFlow<String?>(null)

    val uiState: StateFlow<TeacherMaterialsUiState> = combine(
        repository.observarAsignaturas(),
        repository.observarNoLeidas(RolUsuario.DOCENTE),
        error
    ) { asignaturas, noLeidas, errorActual ->
        TeacherMaterialsUiState(
            asignaturas = asignaturas,
            totalMateriales = asignaturas.sumOf { it.todosLosMateriales().size },
            noLeidas = noLeidas,
            error = errorActual,
            cargando = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TeacherMaterialsUiState()
    )

    fun eliminarMaterial(materialId: String) {
        viewModelScope.launch {
            error.value = null
            runCatching {
                repository.eliminarMaterial(materialId)
            }.onFailure { throwable ->
                error.value = throwable.message ?: "No se pudo eliminar el material."
            }
        }
    }

    fun limpiarError() {
        error.value = null
    }

    companion object {
        fun fabrica(repository: StudyRepository): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { TeacherMaterialsViewModel(repository) }
            }
    }
}
