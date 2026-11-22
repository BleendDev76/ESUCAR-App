package cl.duoc.esucar.data.remote

import cl.duoc.esucar.BuildConfig
import cl.duoc.esucar.data.prefs.SessionStore
import cl.duoc.esucar.data.remote.api.SupabaseAuthApi
import cl.duoc.esucar.data.remote.api.SupabaseRestApi
import cl.duoc.esucar.data.remote.api.SupabaseStorageApi
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Añade la clave pública y el token del usuario a cada petición.
 * No puede suspender, por eso lee el token desde la copia en memoria.
 */
class SupabaseAuthInterceptor(
    private val sessionStore: SessionStore
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val builder = request.newBuilder()
            .header("apikey", SupabaseConfig.anonKey)
            .header("Accept", "application/json")

        sessionStore.cachedAccessToken
            ?.takeIf { it.isNotBlank() }
            ?.let { builder.header("Authorization", "Bearer $it") }

        return chain.proceed(builder.build())
    }
}

class SupabaseClientProvider(sessionStore: SessionStore) {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
    }

    private val logging = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) {
            // BASIC evita escribir contraseñas y tokens en logcat.
            HttpLoggingInterceptor.Level.BASIC
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
    }

    val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(90, TimeUnit.SECONDS)
        .addInterceptor(SupabaseAuthInterceptor(sessionStore))
        .addInterceptor(logging)
        .build()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(SupabaseConfig.url.trimEnd('/') + "/")
        .client(httpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    val authApi: SupabaseAuthApi = retrofit.create(SupabaseAuthApi::class.java)
    val restApi: SupabaseRestApi = retrofit.create(SupabaseRestApi::class.java)
    val storageApi: SupabaseStorageApi = retrofit.create(SupabaseStorageApi::class.java)
}
