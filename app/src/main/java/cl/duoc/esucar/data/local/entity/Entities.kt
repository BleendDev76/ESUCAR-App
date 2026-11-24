package cl.duoc.esucar.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "courses")
data class CourseEntity(
    @PrimaryKey val id: String,
    val code: String,
    val name: String,
    val teacherName: String,
    val color: String,
    val position: Int,
    val teacherId: String? = null
)

@Entity(
    tableName = "units",
    foreignKeys = [
        ForeignKey(
            entity = CourseEntity::class,
            parentColumns = ["id"],
            childColumns = ["courseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("courseId")]
)
data class UnitEntity(
    @PrimaryKey val id: String,
    val courseId: String,
    val number: Int,
    val title: String,
    val position: Int
)

@Entity(
    tableName = "materials",
    foreignKeys = [
        ForeignKey(
            entity = UnitEntity::class,
            parentColumns = ["id"],
            childColumns = ["unitId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("unitId")]
)
data class MaterialEntity(
    @PrimaryKey val id: String,
    val unitId: String,
    val title: String,
    val type: String,
    val size: String?,
    val studied: Boolean,
    val downloaded: Boolean,
    val description: String,
    val uploadedAt: String,
    val position: Int,
    /** Ruta del archivo dentro del bucket `materials` de Supabase Storage. */
    val storagePath: String? = null,
    val fileName: String? = null,
    val mimeType: String? = null,
    /** Ruta local del archivo ya descargado (modo sin conexión). */
    val localPath: String? = null,
    /** Usuario al que pertenece el progreso local todavía no confirmado por el servidor. */
    val pendingProgressUserId: String? = null
)

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    /** `auth.uid()` del estudiante autor de la consulta (mensajería por usuario). */
    val studentId: String? = null,
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

@Entity(tableName = "direct_messages", indices = [Index("studentId", "teacherId", "timestamp")])
data class DirectMessageEntity(
    @PrimaryKey val id: String,
    val studentId: String,
    val teacherId: String,
    val senderId: String,
    val studentName: String,
    val content: String,
    val timestamp: String
)

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey val id: String,
    val role: String,
    val title: String,
    val description: String,
    val timestamp: String,
    val read: Boolean,
    val type: String
)
