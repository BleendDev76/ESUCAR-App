package cl.duoc.esucar.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import cl.duoc.esucar.domain.model.RolUsuario
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.sessionDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "esucar_session"
)

class SessionStore(private val context: Context) {

    private val roleKey = stringPreferencesKey("role")
    private val userIdKey = intPreferencesKey("userId")
    private val userNameKey = stringPreferencesKey("userName")

    private val accessTokenKey = stringPreferencesKey("accessToken")
    private val refreshTokenKey = stringPreferencesKey("refreshToken")
    private val supabaseUserIdKey = stringPreferencesKey("supabaseUserId")
    private val emailKey = stringPreferencesKey("email")

    /**
     * Copia en memoria para el interceptor de OkHttp, que no puede suspender.
     * Se hidrata al arrancar la aplicación y se actualiza al iniciar sesión.
     */
    @Volatile
    var cachedAccessToken: String? = null
        private set

    @Volatile
    var cachedRefreshToken: String? = null
        private set

    @Volatile
    var cachedUserId: String? = null
        private set

    private val preferences: Flow<Preferences> = context.sessionDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }

    val rol: Flow<RolUsuario?> = preferences.map { stored ->
        stored[roleKey]?.let { storedRole -> RolUsuario.desde(storedRole) }
    }

    val userName: Flow<String?> = preferences.map { stored ->
        stored[userNameKey]
    }

    val userId: Flow<String?> = preferences.map { stored ->
        stored[supabaseUserIdKey]
    }

    val email: Flow<String?> = preferences.map { stored ->
        stored[emailKey]
    }

    val accessToken: Flow<String?> = preferences.map { stored ->
        stored[accessTokenKey]
    }

    /** Carga los tokens en memoria desde DataStore (llamar una vez al arrancar). */
    suspend fun hydrate() {
        val stored = try {
            context.sessionDataStore.data.first()
        } catch (exception: IOException) {
            emptyPreferences()
        }
        cachedAccessToken = stored[accessTokenKey]
        cachedRefreshToken = stored[refreshTokenKey]
        cachedUserId = stored[supabaseUserIdKey]
    }

    /** Sesión heredada (login local). Se mantiene para no romper los tests existentes. */
    suspend fun setSession(userId: Int, name: String, rol: RolUsuario) {
        context.sessionDataStore.edit { preferences ->
            preferences[userIdKey] = userId
            preferences[userNameKey] = name
            preferences[roleKey] = rol.name
        }
    }

    /** Sesión de Supabase: tokens + identidad del usuario autenticado. */
    suspend fun setSupabaseSession(
        accessToken: String,
        refreshToken: String,
        userId: String,
        email: String,
        name: String,
        rol: RolUsuario
    ) {
        cachedAccessToken = accessToken
        cachedRefreshToken = refreshToken
        cachedUserId = userId
        context.sessionDataStore.edit { preferences ->
            preferences[accessTokenKey] = accessToken
            preferences[refreshTokenKey] = refreshToken
            preferences[supabaseUserIdKey] = userId
            preferences[emailKey] = email
            preferences[userNameKey] = name
            preferences[roleKey] = rol.name
        }
    }

    /** Tokens sin rol: permite autenticar la carga del perfil sin conceder acceso previo. */
    suspend fun setSupabaseCredentials(accessToken: String, refreshToken: String, userId: String) {
        cachedAccessToken = accessToken
        cachedRefreshToken = refreshToken
        cachedUserId = userId
        context.sessionDataStore.edit { preferences ->
            preferences[accessTokenKey] = accessToken
            preferences[refreshTokenKey] = refreshToken
            preferences[supabaseUserIdKey] = userId
            preferences.remove(roleKey)
            preferences.remove(userIdKey)
        }
    }

    suspend fun updateTokens(accessToken: String, refreshToken: String) {
        cachedAccessToken = accessToken
        cachedRefreshToken = refreshToken
        context.sessionDataStore.edit { preferences ->
            preferences[accessTokenKey] = accessToken
            preferences[refreshTokenKey] = refreshToken
        }
    }

    suspend fun clear() {
        cachedAccessToken = null
        cachedRefreshToken = null
        cachedUserId = null
        context.sessionDataStore.edit { preferences ->
            preferences.remove(roleKey)
            preferences.remove(userIdKey)
            preferences.remove(userNameKey)
            preferences.remove(accessTokenKey)
            preferences.remove(refreshTokenKey)
            preferences.remove(supabaseUserIdKey)
            preferences.remove(emailKey)
        }
    }
}
