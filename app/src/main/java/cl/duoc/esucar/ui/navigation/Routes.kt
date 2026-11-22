package cl.duoc.esucar.ui.navigation

import android.net.Uri

object Routes {

    const val LOGIN = "login"
    const val STUDENT_GRAPH = "student"
    const val STUDENT_HOME = "student/home"
    const val STUDENT_COURSES = "student/courses"
    const val STUDENT_UNITS = "student/unidades/{asignaturaId}"
    const val STUDENT_MATERIALS = "student/materiales/{asignaturaId}/{unidadId}"
    const val STUDENT_DETAIL = "student/detail/{asignaturaId}/{unidadId}/{materialId}"
    const val STUDENT_MESSAGES = "student/messages"
    const val STUDENT_COMPOSE_BASE = "student/compose"
    const val STUDENT_COMPOSE =
        "$STUDENT_COMPOSE_BASE?asignaturaId={asignaturaId}&unidadId={unidadId}&materialId={materialId}&messageId={messageId}"
    const val STUDENT_DOWNLOADS = "student/downloads"

    const val TEACHER_INBOX = "teacher/inbox"
    const val TEACHER_MATERIALS = "teacher/materiales"
    const val TEACHER_REPLY = "teacher/respuesta/{messageId}"
    const val TEACHER_EDITOR_BASE = "teacher/editor"
    const val TEACHER_EDITOR = "$TEACHER_EDITOR_BASE?asignaturaId={asignaturaId}&unidadId={unidadId}&materialId={materialId}"

    const val NOTIFICATIONS = "notifications"

    fun studentUnits(asignaturaId: String): String = "student/unidades/${Uri.encode(asignaturaId)}"

    fun studentMaterials(asignaturaId: String, unidadId: String): String =
        "student/materiales/${Uri.encode(asignaturaId)}/${Uri.encode(unidadId)}"

    fun studentDetail(asignaturaId: String, unidadId: String, materialId: String): String =
        "student/detail/${Uri.encode(asignaturaId)}/${Uri.encode(unidadId)}/${Uri.encode(materialId)}"

    fun studentCompose(
        asignaturaId: String? = null,
        unidadId: String? = null,
        materialId: String? = null,
        messageId: String? = null
    ): String {
        if (asignaturaId == null && unidadId == null && materialId == null && messageId == null) {
            return STUDENT_COMPOSE_BASE
        }
        val params = buildList {
            asignaturaId?.let { add("asignaturaId=${Uri.encode(it)}") }
            unidadId?.let { add("unidadId=${Uri.encode(it)}") }
            materialId?.let { add("materialId=${Uri.encode(it)}") }
            messageId?.let { add("messageId=${Uri.encode(it)}") }
        }
        return "$STUDENT_COMPOSE_BASE?" + params.joinToString("&")
    }

    fun teacherReply(messageId: String): String = "teacher/respuesta/$messageId"

    fun teacherEditor(
        asignaturaId: String? = null,
        unidadId: String? = null,
        materialId: String? = null
    ): String {
        if (asignaturaId == null && unidadId == null && materialId == null) {
            return TEACHER_EDITOR_BASE
        }
        val params = buildList {
            asignaturaId?.let { add("asignaturaId=${Uri.encode(it)}") }
            unidadId?.let { add("unidadId=${Uri.encode(it)}") }
            materialId?.let { add("materialId=${Uri.encode(it)}") }
        }
        return "teacher/editor?" + params.joinToString("&")
    }
}
