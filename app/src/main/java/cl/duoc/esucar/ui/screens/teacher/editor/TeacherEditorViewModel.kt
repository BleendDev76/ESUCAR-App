package cl.duoc.esucar.ui.screens.teacher.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import cl.duoc.esucar.data.remote.ArchivoMaterial
import cl.duoc.esucar.data.repository.StudyRepository
import cl.duoc.esucar.domain.model.Asignatura
import cl.duoc.esucar.domain.model.TipoMaterial
import cl.duoc.esucar.domain.model.Unidad
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TeacherEditorUiState(
    val asignaturas: List<Asignatura> = emptyList(),
    val selectedCourseId: String? = null,
    val selectedUnitId: String? = null,
    val unidades: List<Unidad> = emptyList(),
    val titulo: String = "",
    val tipo: TipoMaterial = TipoMaterial.PDF,
    val tamano: String = "",
    val descripcion: String = "",
    val archivo: ArchivoMaterial? = null,
    val editando: Boolean = false,
    val cargando: Boolean = true,
    val guardando: Boolean = false,
    val guardado: Boolean = false,
    val error: String? = null
) {
    val puedeGuardar: Boolean =
        selectedCourseId != null &&
            selectedUnitId != null &&
            titulo.isNotBlank() &&
            descripcion.isNotBlank() &&
            !guardando
}

class TeacherEditorViewModel(
    private val repository: StudyRepository,
    private val initialCourseId: String?,
    private val initialUnitId: String?,
    private val materialId: String?
) : ViewModel() {

    private val _uiState = MutableStateFlow(TeacherEditorUiState())
    val uiState: StateFlow<TeacherEditorUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val asignaturas = runCatching { repository.observarAsignaturas().first() }
                .getOrElse { emptyList() }
            val material = materialId?.let { repository.obtenerMaterial(it) }
            val materialContext = materialId?.let { id ->
                asignaturas.firstNotNullOfOrNull { asignatura ->
                    asignatura.unidades.firstOrNull { unidad ->
                        unidad.materiales.any { it.id == id }
                    }?.let { unidad -> asignatura to unidad }
                }
            }
            val selectedCourseId = initialCourseId ?: materialContext?.first?.id
            val selectedUnitId = initialUnitId ?: materialContext?.second?.id
            val unidades =
                asignaturas.firstOrNull { it.id == selectedCourseId }?.unidades ?: emptyList()

            _uiState.value = TeacherEditorUiState(
                asignaturas = asignaturas,
                selectedCourseId = selectedCourseId,
                selectedUnitId = selectedUnitId,
                unidades = unidades,
                titulo = material?.titulo.orEmpty(),
                tipo = material?.tipo ?: TipoMaterial.PDF,
                tamano = material?.tamano.orEmpty(),
                descripcion = material?.descripcion.orEmpty(),
                editando = materialId != null,
                cargando = false,
                error = if (materialId != null && material == null) {
                    "No se encontró el material que quieres editar."
                } else {
                    null
                }
            )
        }
    }

    fun onCourseSelected(asignaturaId: String) {
        _uiState.update { state ->
            val unidades =
                state.asignaturas.firstOrNull { it.id == asignaturaId }?.unidades ?: emptyList()
            state.copy(
                selectedCourseId = asignaturaId,
                selectedUnitId = null,
                unidades = unidades,
                error = null
            )
        }
    }

    fun onUnitSelected(unidadId: String) {
        _uiState.update { it.copy(selectedUnitId = unidadId, error = null) }
    }

    fun onTitleChange(value: String) {
        _uiState.update { it.copy(titulo = value, error = null) }
    }

    fun onTypeSelected(tipo: TipoMaterial) {
        _uiState.update { it.copy(tipo = tipo) }
    }

    fun onSizeChange(value: String) {
        _uiState.update { it.copy(tamano = value) }
    }

    /** El docente eligió un archivo del dispositivo para subirlo a Storage. */
    fun onFileSelected(archivo: ArchivoMaterial?) {
        _uiState.update {
            it.copy(
                archivo = archivo,
                tamano = archivo?.tamanoLegible ?: it.tamano,
                error = null
            )
        }
    }

    fun onDescriptionChange(value: String) {
        _uiState.update { it.copy(descripcion = value, error = null) }
    }

    fun save() {
        val state = _uiState.value
        if (!state.puedeGuardar) return
        val asignaturaId = state.selectedCourseId ?: return
        val unidadId = state.selectedUnitId ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(guardando = true, error = null) }
            runCatching {
                if (state.editando && materialId != null) {
                    repository.editarMaterial(
                        materialId = materialId,
                        titulo = state.titulo,
                        tipo = state.tipo,
                        descripcion = state.descripcion,
                        tamano = state.tamano.trim().ifBlank { null },
                        archivo = state.archivo
                    )
                } else {
                    repository.agregarMaterial(
                        asignaturaId = asignaturaId,
                        unidadId = unidadId,
                        titulo = state.titulo,
                        tipo = state.tipo,
                        descripcion = state.descripcion,
                        tamano = state.tamano.trim().ifBlank { null },
                        archivo = state.archivo
                    )
                }
            }.onSuccess {
                _uiState.update { it.copy(guardando = false, guardado = true) }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        guardando = false,
                        error = throwable.message ?: "No se pudo guardar el material."
                    )
                }
            }
        }
    }

    companion object {
        fun fabrica(
            repository: StudyRepository,
            asignaturaId: String?,
            unidadId: String?,
            materialId: String?
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                TeacherEditorViewModel(
                    repository = repository,
                    initialCourseId = asignaturaId,
                    initialUnitId = unidadId,
                    materialId = materialId
                )
            }
        }
    }
}
