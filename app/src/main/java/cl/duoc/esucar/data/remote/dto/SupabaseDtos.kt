package cl.duoc.esucar.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ---------------------------------------------------------------------------
// Supabase Auth (GoTrue)
// ---------------------------------------------------------------------------

@Serializable
data class UserMetadata(
    @SerialName("full_name") val fullName: String,
    val role: String,
    @SerialName("teacher_code") val teacherCode: String? = null
)

@Serializable
data class SignUpRequest(
    val email: String,
    val password: String,
    val data: UserMetadata
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class RefreshRequest(
    @SerialName("refresh_token") val refreshToken: String
)

@Serializable
data class AuthUser(
    val id: String,
    val email: String? = null,
    @SerialName("user_metadata") val userMetadata: UserMetadata? = null
)

/**
 * Supabase devuelve dos formas:
 * - Login / refresh: los tokens en la raíz.
 * - Signup con confirmación de email activa: `{ "user": ..., "session": null }`.
 */
@Serializable
data class AuthResponse(
    @SerialName("access_token") val accessToken: String? = null,
    @SerialName("refresh_token") val refreshToken: String? = null,
    @SerialName("expires_in") val expiresIn: Long? = null,
    val user: AuthUser? = null,
    val session: AuthResponse? = null
) {
    val effectiveAccessToken: String? get() = accessToken ?: session?.accessToken
    val effectiveRefreshToken: String? get() = refreshToken ?: session?.refreshToken
    val effectiveUser: AuthUser? get() = user ?: session?.user
}

// ---------------------------------------------------------------------------
// PostgREST
// ---------------------------------------------------------------------------

@Serializable
data class CourseDto(
    val id: String,
    val code: String,
    val name: String,
    val description: String = "",
    val color: String = "#15803d",
    @SerialName("teacher_id") val teacherId: String? = null,
    val position: Int = 0,
    val units: List<UnitDto> = emptyList()
)

@Serializable
data class UnitDto(
    val id: String,
    @SerialName("course_id") val courseId: String,
    val number: Int,
    val title: String,
    val position: Int = 0,
    val materials: List<MaterialDto> = emptyList()
)

@Serializable
data class MaterialDto(
    val id: String,
    @SerialName("unit_id") val unitId: String,
    val title: String,
    val type: String,
    val description: String = "",
    @SerialName("storage_path") val storagePath: String? = null,
    @SerialName("file_name") val fileName: String? = null,
    @SerialName("mime_type") val mimeType: String? = null,
    @SerialName("size_bytes") val sizeBytes: Long? = null,
    val position: Int = 0,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("uploaded_by") val uploadedBy: String? = null
)

@Serializable
data class CreateMaterialRequest(
    @SerialName("unit_id") val unitId: String,
    val title: String,
    val type: String,
    val description: String,
    @SerialName("storage_path") val storagePath: String? = null,
    @SerialName("file_name") val fileName: String? = null,
    @SerialName("mime_type") val mimeType: String? = null,
    @SerialName("size_bytes") val sizeBytes: Long? = null,
    @SerialName("uploaded_by") val uploadedBy: String? = null,
    val position: Int = 0
)

@Serializable
data class UpdateMaterialRequest(
    val title: String,
    val type: String,
    val description: String,
    @SerialName("storage_path") val storagePath: String? = null,
    @SerialName("file_name") val fileName: String? = null,
    @SerialName("mime_type") val mimeType: String? = null,
    @SerialName("size_bytes") val sizeBytes: Long? = null
)

@Serializable
data class MessageDto(
    val id: String,
    @SerialName("student_id") val studentId: String,
    @SerialName("student_name") val studentName: String = "",
    @SerialName("teacher_id") val teacherId: String? = null,
    @SerialName("course_id") val courseId: String,
    @SerialName("unit_id") val unitId: String? = null,
    @SerialName("material_id") val materialId: String? = null,
    val subject: String,
    val content: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("read_at") val readAt: String? = null,
    val replies: List<ReplyDto> = emptyList()
)

@Serializable
data class CreateMessageRequest(
    @SerialName("student_id") val studentId: String,
    @SerialName("student_name") val studentName: String,
    @SerialName("teacher_id") val teacherId: String? = null,
    @SerialName("course_id") val courseId: String,
    @SerialName("unit_id") val unitId: String? = null,
    @SerialName("material_id") val materialId: String? = null,
    val subject: String,
    val content: String
)

@Serializable
data class ReplyDto(
    val id: String,
    @SerialName("message_id") val messageId: String,
    @SerialName("teacher_id") val teacherId: String,
    val content: String,
    @SerialName("created_at") val createdAt: String
)

@Serializable
data class CreateReplyRequest(
    @SerialName("message_id") val messageId: String,
    @SerialName("teacher_id") val teacherId: String,
    val content: String
)

@Serializable
data class DirectMessageDto(
    val id: String,
    @SerialName("student_id") val studentId: String,
    @SerialName("teacher_id") val teacherId: String,
    @SerialName("sender_id") val senderId: String,
    @SerialName("student_name") val studentName: String,
    val content: String,
    @SerialName("created_at") val createdAt: String
)

@Serializable
data class CreateDirectMessageRequest(
    @SerialName("student_id") val studentId: String,
    @SerialName("teacher_id") val teacherId: String,
    @SerialName("sender_id") val senderId: String,
    @SerialName("student_name") val studentName: String,
    val content: String
)

@Serializable
data class ProfileDto(
    val id: String,
    @SerialName("full_name") val fullName: String,
    val role: String
)

@Serializable
data class ProgressDto(
    @SerialName("student_id") val studentId: String? = null,
    @SerialName("material_id") val materialId: String,
    @SerialName("studied_at") val studiedAt: String? = null
)

@Serializable
data class UpsertProgressRequest(
    @SerialName("student_id") val studentId: String,
    @SerialName("material_id") val materialId: String
)

@Serializable
data class UpsertDownloadRequest(
    @SerialName("student_id") val studentId: String,
    @SerialName("material_id") val materialId: String
)

@Serializable
data class NotificationDto(
    val id: String,
    @SerialName("user_id") val userId: String,
    val type: String,
    val title: String,
    val body: String = "",
    @SerialName("related_id") val relatedId: String? = null,
    @SerialName("read_at") val readAt: String? = null,
    @SerialName("created_at") val createdAt: String
)

@Serializable
data class CreateNotificationRequest(
    @SerialName("user_id") val userId: String,
    val type: String,
    val title: String,
    val body: String,
    @SerialName("related_id") val relatedId: String? = null
)

@Serializable
data class UpdateReadRequest(
    @SerialName("read_at") val readAt: String
)

@Serializable
data class ApiErrorDto(
    val msg: String? = null,
    val message: String? = null,
    @SerialName("error_description") val errorDescription: String? = null,
    @SerialName("error_code") val errorCode: String? = null
)
