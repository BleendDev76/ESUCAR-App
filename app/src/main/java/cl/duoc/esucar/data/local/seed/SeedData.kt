package cl.duoc.esucar.data.local.seed

import androidx.sqlite.db.SupportSQLiteDatabase

private data class SeedCourse(
    val id: String,
    val code: String,
    val name: String,
    val teacherName: String,
    val color: String,
    val units: List<SeedUnit>
)

private data class SeedUnit(
    val id: String,
    val number: Int,
    val title: String,
    val materials: List<SeedMaterial>
)

private data class SeedMaterial(
    val id: String,
    val title: String,
    val type: String,
    val size: String?,
    val studied: Boolean,
    val uploadedAt: String,
    val description: String
)

object SeedData {

    fun insert(db: SupportSQLiteDatabase) {
        courses.forEachIndexed { courseIndex, course ->
            db.execSQL(
                "INSERT INTO courses (id, code, name, teacherName, color, position) VALUES (?, ?, ?, ?, ?, ?)",
                arrayOf(course.id, course.code, course.name, course.teacherName, course.color, courseIndex)
            )

            course.units.forEachIndexed { unitIndex, unit ->
                db.execSQL(
                    "INSERT INTO units (id, courseId, number, title, position) VALUES (?, ?, ?, ?, ?)",
                    arrayOf(unit.id, course.id, unit.number, unit.title, unitIndex)
                )

                unit.materials.forEachIndexed { materialIndex, material ->
                    db.execSQL(
                        "INSERT INTO materials (id, unitId, title, type, size, studied, downloaded, description, uploadedAt, position) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                        arrayOf(
                            material.id,
                            unit.id,
                            material.title,
                            material.type,
                            material.size,
                            if (material.studied) 1 else 0,
                            0,
                            material.description,
                            material.uploadedAt,
                            materialIndex
                        )
                    )
                }
            }
        }

        messages.forEach { message ->
            db.execSQL(
                "INSERT INTO messages (id, studentName, courseId, courseName, unitId, unitTitle, materialId, materialTitle, " +
                    "subject, content, timestamp, reply, replyTimestamp) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                arrayOf(
                    message.id,
                    message.studentName,
                    message.courseId,
                    message.courseName,
                    message.unitId,
                    message.unitTitle,
                    message.materialId,
                    message.materialTitle,
                    message.subject,
                    message.content,
                    message.timestamp,
                    message.reply,
                    message.replyTimestamp
                )
            )
        }
    }

    private val courses = listOf(
        SeedCourse(
            id = "c1",
            code = "ADM301",
            name = "Administración Pública",
            teacherName = "Sargento Instructor R. Fuentes",
            color = "#15803d",
            units = listOf(
                SeedUnit(
                    id = "u1",
                    number = 1,
                    title = "Fundamentos de la Gestión Pública",
                    materials = listOf(
                        SeedMaterial(
                            id = "m1",
                            title = "Introducción a la Administración del Estado",
                            type = "PDF",
                            size = "2.4 MB",
                            studied = true,
                            uploadedAt = "2025-07-10",
                            description = "Conceptos fundamentales sobre la estructura y funcionamiento del Estado chileno."
                        ),
                        SeedMaterial(
                            id = "m2",
                            title = "Organigrama Institucional ESUCAR",
                            type = "PRESENTACION",
                            size = "5.1 MB",
                            studied = true,
                            uploadedAt = "2025-07-12",
                            description = "Presentación sobre la estructura jerárquica y roles dentro de la institución."
                        ),
                        SeedMaterial(
                            id = "m3",
                            title = "Principios de la Función Pública",
                            type = "GUIA",
                            size = "1.2 MB",
                            studied = false,
                            uploadedAt = "2025-07-15",
                            description = "Guía de ejercicios sobre los principios que rigen el actuar del servidor público."
                        )
                    )
                ),
                SeedUnit(
                    id = "u2",
                    number = 2,
                    title = "Presupuesto y Recursos del Estado",
                    materials = listOf(
                        SeedMaterial(
                            id = "m4",
                            title = "Ley de Presupuestos del Sector Público",
                            type = "PDF",
                            size = "3.8 MB",
                            studied = false,
                            uploadedAt = "2025-07-20",
                            description = "Análisis de la ley de presupuestos y su aplicación en organismos de seguridad."
                        ),
                        SeedMaterial(
                            id = "m5",
                            title = "Video: Ciclo Presupuestario",
                            type = "VIDEO",
                            size = null,
                            studied = false,
                            uploadedAt = "2025-07-22",
                            description = "Clase grabada explicando las etapas del ciclo presupuestario en el sector público."
                        )
                    )
                ),
                SeedUnit(
                    id = "u3",
                    number = 3,
                    title = "Control y Auditoría Interna",
                    materials = listOf(
                        SeedMaterial(
                            id = "m6",
                            title = "Normas de Control Interno – Contraloría",
                            type = "PDF",
                            size = "4.5 MB",
                            studied = false,
                            uploadedAt = "2025-08-01",
                            description = "Normativa vigente de la Contraloría General de la República sobre control interno."
                        ),
                        SeedMaterial(
                            id = "m7",
                            title = "Pauta de Evaluación Unidad 3",
                            type = "GUIA",
                            size = "0.8 MB",
                            studied = false,
                            uploadedAt = "2025-08-03",
                            description = "Criterios de evaluación y rúbrica para la prueba de la unidad 3."
                        )
                    )
                )
            )
        ),
        SeedCourse(
            id = "c2",
            code = "SEG401",
            name = "Seguridad Ciudadana",
            teacherName = "Mayor C. Vásquez",
            color = "#1d4ed8",
            units = listOf(
                SeedUnit(
                    id = "u4",
                    number = 1,
                    title = "Marco Jurídico de la Seguridad Pública",
                    materials = listOf(
                        SeedMaterial(
                            id = "m8",
                            title = "Constitución Política: Orden Público",
                            type = "PDF",
                            size = "1.9 MB",
                            studied = true,
                            uploadedAt = "2025-07-08",
                            description = "Extracto de la Constitución Política de Chile, artículos relativos al orden público y seguridad."
                        ),
                        SeedMaterial(
                            id = "m9",
                            title = "Ley Orgánica de Carabineros",
                            type = "PDF",
                            size = "3.2 MB",
                            studied = true,
                            uploadedAt = "2025-07-09",
                            description = "Texto íntegro de la Ley 18.961 que establece la organización y atribuciones de Carabineros."
                        ),
                        SeedMaterial(
                            id = "m10",
                            title = "Presentación: Instituciones de Seguridad",
                            type = "PRESENTACION",
                            size = "6.3 MB",
                            studied = false,
                            uploadedAt = "2025-07-14",
                            description = "Mapa institucional de las entidades de seguridad pública en Chile y su coordinación."
                        )
                    )
                ),
                SeedUnit(
                    id = "u5",
                    number = 2,
                    title = "Prevención del Delito y Orden Público",
                    materials = listOf(
                        SeedMaterial(
                            id = "m11",
                            title = "Estrategias de Prevención Situacional",
                            type = "PDF",
                            size = "2.7 MB",
                            studied = false,
                            uploadedAt = "2025-07-25",
                            description = "Metodologías y herramientas para la prevención del delito en el contexto comunitario."
                        ),
                        SeedMaterial(
                            id = "m12",
                            title = "Video: Patrullaje Comunitario",
                            type = "VIDEO",
                            size = null,
                            studied = false,
                            uploadedAt = "2025-07-28",
                            description = "Registro audiovisual de buenas prácticas en operativos de patrullaje comunitario."
                        )
                    )
                ),
                SeedUnit(
                    id = "u6",
                    number = 3,
                    title = "Uso Proporcional de la Fuerza",
                    materials = listOf(
                        SeedMaterial(
                            id = "m13",
                            title = "Protocolos de Uso de la Fuerza",
                            type = "GUIA",
                            size = "1.4 MB",
                            studied = false,
                            uploadedAt = "2025-08-05",
                            description = "Manual de procedimientos para el uso proporcional y racional de la fuerza policial."
                        )
                    )
                )
            )
        ),
        SeedCourse(
            id = "c3",
            code = "ETI201",
            name = "Ética Profesional",
            teacherName = "Capitán M. Herrera",
            color = "#7c3aed",
            units = listOf(
                SeedUnit(
                    id = "u7",
                    number = 1,
                    title = "Fundamentos de la Ética Policial",
                    materials = listOf(
                        SeedMaterial(
                            id = "m14",
                            title = "Código de Ética de Carabineros",
                            type = "PDF",
                            size = "1.1 MB",
                            studied = true,
                            uploadedAt = "2025-07-07",
                            description = "Documento oficial del código de conducta y valores institucionales de Carabineros de Chile."
                        ),
                        SeedMaterial(
                            id = "m15",
                            title = "Ética y Derechos Humanos",
                            type = "PRESENTACION",
                            size = "4.2 MB",
                            studied = true,
                            uploadedAt = "2025-07-10",
                            description = "Relación entre el actuar policial y el respeto a los derechos fundamentales de las personas."
                        )
                    )
                ),
                SeedUnit(
                    id = "u8",
                    number = 2,
                    title = "Dilemas Éticos en el Servicio",
                    materials = listOf(
                        SeedMaterial(
                            id = "m16",
                            title = "Casos de Estudio: Dilemas Éticos",
                            type = "GUIA",
                            size = "0.9 MB",
                            studied = true,
                            uploadedAt = "2025-07-18",
                            description = "Análisis de situaciones reales que presentan conflictos éticos en el ejercicio del servicio policial."
                        ),
                        SeedMaterial(
                            id = "m17",
                            title = "Video: Integridad Institucional",
                            type = "VIDEO",
                            size = null,
                            studied = false,
                            uploadedAt = "2025-07-20",
                            description = "Documental sobre la importancia de la integridad y transparencia en las instituciones de seguridad."
                        )
                    )
                ),
                SeedUnit(
                    id = "u9",
                    number = 3,
                    title = "Responsabilidad y Rendición de Cuentas",
                    materials = listOf(
                        SeedMaterial(
                            id = "m18",
                            title = "Marco Legal de Responsabilidad Funcionaria",
                            type = "PDF",
                            size = "2.3 MB",
                            studied = false,
                            uploadedAt = "2025-08-04",
                            description = "Normativa sobre la responsabilidad civil, penal y administrativa del funcionario policial."
                        ),
                        SeedMaterial(
                            id = "m19",
                            title = "Guía de Evaluación Final",
                            type = "GUIA",
                            size = "0.7 MB",
                            studied = false,
                            uploadedAt = "2025-08-06",
                            description = "Instrucciones y criterios para la evaluación sumativa de la asignatura."
                        )
                    )
                )
            )
        )
    )

    private data class SeedMessage(
        val id: String,
        val studentName: String,
        val courseId: String,
        val courseName: String,
        val unitId: String,
        val unitTitle: String,
        val materialId: String?,
        val materialTitle: String?,
        val subject: String,
        val content: String,
        val timestamp: String,
        val reply: String?,
        val replyTimestamp: String?
    )

    private val messages = listOf(
        SeedMessage(
            id = "msg1",
            studentName = "Cabo J. Morales",
            courseId = "c1",
            courseName = "Administración Pública",
            unitId = "u1",
            unitTitle = "Fundamentos de la Gestión Pública",
            materialId = "m3",
            materialTitle = "Principios de la Función Pública",
            subject = "Consulta sobre ejercicio 3 de la guía",
            content = "Sargento Fuentes, en el ejercicio 3 de la guía sobre principios de la función pública no entiendo bien la diferencia entre el principio de legalidad y el de juridicidad. ¿Podría explicarme con un ejemplo práctico?",
            timestamp = "2025-08-12T14:32:00",
            reply = "Cabo Morales, buena pregunta. La legalidad implica que el funcionario sólo puede hacer lo que la ley expresamente le permite. La juridicidad es más amplia: incluye la Constitución, leyes y reglamentos en su conjunto. En la práctica: si la ley dice que puede actuar en X caso (legalidad), pero la Constitución lo prohíbe, prima la juridicidad. Revise el artículo 6° y 7° de la Constitución para afianzar el concepto.",
            replyTimestamp = "2025-08-12T17:05:00"
        ),
        SeedMessage(
            id = "msg2",
            studentName = "Cabo J. Morales",
            courseId = "c2",
            courseName = "Seguridad Ciudadana",
            unitId = "u5",
            unitTitle = "Prevención del Delito y Orden Público",
            materialId = "m11",
            materialTitle = "Estrategias de Prevención Situacional",
            subject = "Acceso al material de la Unidad 2",
            content = "Mayor Vásquez, al intentar abrir el documento de estrategias de prevención situacional me aparece un error. ¿Es posible que lo vuelva a cargar? Necesito revisarlo para la evaluación del viernes.",
            timestamp = "2025-08-14T09:15:00",
            reply = null,
            replyTimestamp = null
        )
    )
}
