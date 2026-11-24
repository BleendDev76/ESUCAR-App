package cl.duoc.esucar.data.remote.api

import cl.duoc.esucar.data.remote.dto.AuthResponse
import cl.duoc.esucar.data.remote.dto.AuthUser
import cl.duoc.esucar.data.remote.dto.CourseDto
import cl.duoc.esucar.data.remote.dto.CreateMaterialRequest
import cl.duoc.esucar.data.remote.dto.CreateMessageRequest
import cl.duoc.esucar.data.remote.dto.CreateReplyRequest
import cl.duoc.esucar.data.remote.dto.CreateDirectMessageRequest
import cl.duoc.esucar.data.remote.dto.DirectMessageDto
import cl.duoc.esucar.data.remote.dto.LoginRequest
import cl.duoc.esucar.data.remote.dto.MaterialDto
import cl.duoc.esucar.data.remote.dto.MessageDto
import cl.duoc.esucar.data.remote.dto.NotificationDto
import cl.duoc.esucar.data.remote.dto.ProfileDto
import cl.duoc.esucar.data.remote.dto.ProgressDto
import cl.duoc.esucar.data.remote.dto.RefreshRequest
import cl.duoc.esucar.data.remote.dto.ReplyDto
import cl.duoc.esucar.data.remote.dto.SignUpRequest
import cl.duoc.esucar.data.remote.dto.UpdateMaterialRequest
import cl.duoc.esucar.data.remote.dto.UpdateReadRequest
import cl.duoc.esucar.data.remote.dto.UpsertDownloadRequest
import cl.duoc.esucar.data.remote.dto.UpsertProgressRequest
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming

interface SupabaseAuthApi {

    @POST("auth/v1/signup")
    suspend fun signUp(@Body body: SignUpRequest): AuthResponse

    @POST("auth/v1/token?grant_type=password")
    suspend fun login(@Body body: LoginRequest): AuthResponse

    @POST("auth/v1/token?grant_type=refresh_token")
    suspend fun refresh(@Body body: RefreshRequest): AuthResponse

    @POST("auth/v1/logout")
    suspend fun logout()

    @GET("auth/v1/user")
    suspend fun me(): AuthUser
}

interface SupabaseRestApi {

    @GET("rest/v1/direct_messages")
    suspend fun directMessages(
        @Query("select") select: String = "*",
        @Query("order") order: String = "created_at.asc"
    ): List<DirectMessageDto>

    @POST("rest/v1/direct_messages")
    suspend fun createDirectMessage(
        @Body body: CreateDirectMessageRequest,
        @Header("Prefer") prefer: String = "return=representation"
    ): List<DirectMessageDto>

    /** Perfil real del usuario (el rol autoritativo vive en Postgres, no en el JWT). */
    @GET("rest/v1/profiles")
    suspend fun profile(
        @Query("id") idFilter: String,
        @Query("select") select: String = "id,full_name,role"
    ): List<ProfileDto>

    @GET("rest/v1/teacher_directory")
    suspend fun teacherDirectory(
        @Query("id") idFilter: String,
        @Query("select") select: String = "id,full_name"
    ): List<ProfileDto>

    @GET("rest/v1/courses")
    suspend fun courses(
        @Query("select") select: String = "*,units(*,materials(*))",
        // PostgREST no permite ordenar por una relación uno-a-muchos;
        // el orden de unidades y materiales se resuelve en el mapper.
        @Query("order") order: String = "position"
    ): List<CourseDto>

    @GET("rest/v1/messages")
    suspend fun messages(
        @Query("select") select: String = "*,replies(*)",
        @Query("order") order: String = "created_at.desc"
    ): List<MessageDto>

    @POST("rest/v1/messages")
    suspend fun createMessage(
        @Body body: CreateMessageRequest,
        @Header("Prefer") prefer: String = "return=representation"
    ): List<MessageDto>

    @PATCH("rest/v1/messages")
    suspend fun markMessageRead(
        @Query("id") idFilter: String,
        @Body body: UpdateReadRequest,
        @Header("Prefer") prefer: String = "return=representation"
    ): List<MessageDto>

    @POST("rest/v1/replies")
    suspend fun createReply(
        @Body body: CreateReplyRequest,
        @Header("Prefer") prefer: String = "return=representation"
    ): List<ReplyDto>

    @POST("rest/v1/materials")
    suspend fun createMaterial(
        @Body body: CreateMaterialRequest,
        @Header("Prefer") prefer: String = "return=representation"
    ): List<MaterialDto>

    @PATCH("rest/v1/materials")
    suspend fun updateMaterial(
        @Query("id") idFilter: String,
        @Body body: UpdateMaterialRequest,
        @Header("Prefer") prefer: String = "return=representation"
    ): List<MaterialDto>

    @DELETE("rest/v1/materials")
    suspend fun deleteMaterial(@Query("id") idFilter: String)

    @GET("rest/v1/progress")
    suspend fun progress(
        @Query("select") select: String = "material_id,studied_at"
    ): List<ProgressDto>

    @POST("rest/v1/progress")
    suspend fun upsertProgress(
        @Body body: UpsertProgressRequest,
        @Header("Prefer") prefer: String = "resolution=merge-duplicates"
    )

    @DELETE("rest/v1/progress")
    suspend fun deleteProgress(
        @Query("student_id") studentIdFilter: String,
        @Query("material_id") materialIdFilter: String
    )

    @GET("rest/v1/notifications")
    suspend fun notifications(
        @Query("select") select: String = "*",
        @Query("order") order: String = "created_at.desc"
    ): List<NotificationDto>

    @PATCH("rest/v1/notifications")
    suspend fun markNotificationRead(
        @Query("id") idFilter: String,
        @Body body: UpdateReadRequest
    )

    @PATCH("rest/v1/notifications")
    suspend fun markAllNotificationsRead(
        @Query("read_at") readAtFilter: String = "is.null",
        @Body body: UpdateReadRequest
    )

    @POST("rest/v1/downloads")
    suspend fun upsertDownload(
        @Body body: UpsertDownloadRequest,
        @Header("Prefer") prefer: String = "resolution=merge-duplicates"
    )

    @DELETE("rest/v1/downloads")
    suspend fun deleteDownload(
        @Query("student_id") studentIdFilter: String,
        @Query("material_id") materialIdFilter: String
    )
}

interface SupabaseStorageApi {

    /** Sube un archivo. `path` ya viene codificado (incluye `/`). */
    @PUT("storage/v1/object/materials/{path}")
    suspend fun upload(
        @Path(value = "path", encoded = true) path: String,
        @Body body: RequestBody,
        @Header("x-upsert") upsert: String = "true"
    )

    /** Descarga el contenido binario del archivo. */
    @Streaming
    @GET("storage/v1/object/materials/{path}")
    suspend fun download(
        @Path(value = "path", encoded = true) path: String
    ): ResponseBody

    @DELETE("storage/v1/object/materials/{path}")
    suspend fun delete(
        @Path(value = "path", encoded = true) path: String
    )
}
