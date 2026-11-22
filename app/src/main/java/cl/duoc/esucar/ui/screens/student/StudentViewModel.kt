package cl.duoc.esucar.ui.screens.student

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import cl.duoc.esucar.data.repository.StudyRepository
import cl.duoc.esucar.domain.model.Asignatura
import cl.duoc.esucar.domain.model.Material
import cl.duoc.esucar.domain.model.Mensaje
import cl.duoc.esucar.domain.model.RolUsuario
import cl.duoc.esucar.domain.model.Unidad
import cl.duoc.esucar.domain.model.cantidadEstudiados
import cl.duoc.esucar.domain.model.cantidadTotal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

data class DownloadedMaterial(
    val asignatura: Asignatura,
    val unidad: Unidad,
    val material: Material
)

data class StudentUiState(
    val asignaturas: List<Asignatura> = emptyList(),
    val mensajes: List<Mensaje> = emptyList(),
    val descargas: List<DownloadedMaterial> = emptyList(),
    val noLeidas: Int = 0,
    val cargando: Boolean = true,
    val enviando: Boolean = false,
    val descargando: Boolean = false,
    val consultaEnviada: Boolean = false,
    val error: String? = null
) {
    val totalMateriales: Int = asignaturas.sumOf { it.cantidadTotal() }
    val totalEstudiados: Int = asignaturas.sumOf { it.cantidadEstudiados() }
    val porcentajeGeneral: Int =
        if (totalMateriales == 0) 0 else (totalEstudiados * 100.0 / totalMateriales).roundToInt()
    val consultasPendientes: Int = mensajes.count { it.respuesta == null }
}

private data class StudentActionState(
    val enviando: Boolean = false,
    val descargando: Boolean = false,
    val consultaEnviada: Boolean = false,
    val error: String? = null
)

class StudentViewModel(
    private val repository: StudyRepository,
    private val nombreEstudiante: String
) : ViewModel() {

    private val actionState = MutableStateFlow(StudentActionState())

    val uiState: StateFlow<StudentUiState> = combine(
        repository.observarAsignaturas(),
        repository.observarMensajes(),
        repository.observarNoLeidas(RolUsuario.ESTUDIANTE),
        actionState
    ) { asignaturas, mensajes, noLeidas, acciones ->
        val descargas = asignaturas.flatMap { asignatura ->
            asignatura.unidades.flatMap { unidad ->
                unidad.materiales
                    .filter { it.descargado }
                    .map { material -> DownloadedMaterial(asignatura, unidad, material) }
            }
        }
        StudentUiState(
            asignaturas = asignaturas,
            mensajes = mensajes,
            descargas = descargas,
            noLeidas = noLeidas,
            cargando = false,
            enviando = acciones.enviando,
            descargando = acciones.descargando,
            consultaEnviada = acciones.consultaEnviada,
            error = acciones.error
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = StudentUiState()
    )

    fun observarMensaje(mensajeId: String): Flow<Mensaje?> =
        repository.observarMensaje(mensajeId)

    fun marcarEstudiado(materialId: String) {
        viewModelScope.launch {
            runCatching { repository.marcarEstudiado(materialId) }
                .onFailure { registrarError("No se pudo actualizar el progreso.") }
        }
    }

    fun alternarDescarga(materialId: String) {
        viewModelScope.launch {
            actionState.update { it.copy(descargando = true, error = null) }
            runCatching { repository.alternarDescarga(materialId) }
                .onFailure {
                    registrarError(it.message ?: "No se pudo actualizar la descarga.")
                }
            actionState.update { it.copy(descargando = false) }
        }
    }

    /** Abre el PDF ya descargado con el visor del sistema (FileProvider). */
    fun abrirMaterial(materialId: String) {
        viewModelScope.launch {
            runCatching { repository.abrirMaterial(materialId).getOrThrow() }
                .onFailure {
                    registrarError(it.message ?: "No se pudo abrir el material.")
                }
        }
    }

    fun enviarConsulta(
        asignaturaId: String,
        unidadId: String,
        materialId: String?,
        asunto: String,
        contenido: String
    ) {
        if (asignaturaId.isBlank() || unidadId.isBlank() || asunto.isBlank() || contenido.isBlank()) {
            registrarError("Completa asignatura, unidad, asunto y mensaje.")
            return
        }

        viewModelScope.launch {
            actionState.update {
                it.copy(enviando = true, consultaEnviada = false, error = null)
            }
            runCatching {
                repository.enviarConsulta(
                    nombreEstudiante = nombreEstudiante,
                    asignaturaId = asignaturaId,
                    unidadId = unidadId,
                    materialId = materialId,
                    asunto = asunto,
                    contenido = contenido
                )
            }.onSuccess {
                actionState.update { it.copy(enviando = false, consultaEnviada = true) }
            }.onFailure {
                actionState.update {
                    it.copy(
                        enviando = false,
                        error = "No se pudo enviar la consulta. Intenta nuevamente."
                    )
                }
            }
        }
    }

    fun reiniciarEstadoConsulta() {
        actionState.update { it.copy(consultaEnviada = false, error = null) }
    }

    fun limpiarError() {
        if (actionState.value.error != null) {
            actionState.update { it.copy(error = null) }
        }
    }

    private fun registrarError(message: String) {
        actionState.update { it.copy(error = message) }
    }

    companion object {
        fun fabrica(
            repository: StudyRepository,
            nombreEstudiante: String
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer { StudentViewModel(repository, nombreEstudiante) }
        }
    }
}
