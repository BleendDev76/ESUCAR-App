package cl.duoc.esucar.domain.model

import kotlin.math.roundToInt

enum class RolUsuario {
    ESTUDIANTE,
    DOCENTE;

    companion object {
        fun desde(valor: String?): RolUsuario? = when (valor?.uppercase()) {
            "ESTUDIANTE", "STUDENT" -> ESTUDIANTE
            "DOCENTE", "TEACHER" -> DOCENTE
            else -> null
        }
    }
}

enum class TipoMaterial {
    PDF,
    VIDEO,
    PRESENTACION,
    GUIA
}

enum class TipoNotificacion {
    CONSULTA,
    RESPUESTA,
    MATERIAL;

    companion object {
        fun desde(valor: String?): TipoNotificacion? = when (valor?.uppercase()) {
            "CONSULTA", "QUERY" -> CONSULTA
            "RESPUESTA", "REPLY" -> RESPUESTA
            "MATERIAL" -> MATERIAL
            else -> null
        }
    }
}

data class Asignatura(
    val id: String,
    val codigo: String,
    val nombre: String,
    val nombreDocente: String,
    val color: String,
    val unidades: List<Unidad>
)

data class Unidad(
    val id: String,
    val numero: Int,
    val titulo: String,
    val materiales: List<Material>
)

data class Material(
    val id: String,
    val titulo: String,
    val tipo: TipoMaterial,
    val tamano: String?,
    val estudiado: Boolean,
    val descargado: Boolean,
    val descripcion: String,
    val subidoEl: String
)

data class Mensaje(
    val id: String,
    val nombreEstudiante: String,
    val asignaturaId: String,
    val nombreAsignatura: String,
    val unidadId: String,
    val tituloUnidad: String,
    val materialId: String?,
    val tituloMaterial: String?,
    val asunto: String,
    val contenido: String,
    val fecha: String,
    val respuesta: String?,
    val fechaRespuesta: String?
)

data class Notificacion(
    val id: String,
    val rol: RolUsuario,
    val titulo: String,
    val descripcion: String,
    val fecha: String,
    val leida: Boolean,
    val tipo: TipoNotificacion
)

fun Asignatura.todosLosMateriales(): List<Material> = unidades.flatMap { it.materiales }

fun Asignatura.cantidadEstudiados(): Int = todosLosMateriales().count { it.estudiado }

fun Asignatura.cantidadTotal(): Int = todosLosMateriales().size

fun Asignatura.porcentajeProgreso(): Int {
    val total = cantidadTotal()
    return if (total == 0) 0 else (cantidadEstudiados() * 100.0 / total).roundToInt()
}

fun Unidad.cantidadEstudiados(): Int = materiales.count { it.estudiado }

fun Unidad.porcentajeProgreso(): Int {
    return if (materiales.isEmpty()) 0 else (cantidadEstudiados() * 100.0 / materiales.size).roundToInt()
}

fun Unidad.estaCompleto(): Boolean = materiales.isNotEmpty() && cantidadEstudiados() == materiales.size
