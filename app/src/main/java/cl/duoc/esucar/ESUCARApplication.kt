package cl.duoc.esucar

import android.app.Application
import android.content.Context
import cl.duoc.esucar.core.connectivity.ConnectivityObserver
import cl.duoc.esucar.core.notifications.AndroidNotificador
import cl.duoc.esucar.data.local.AppDatabase
import cl.duoc.esucar.data.prefs.SessionStore
import cl.duoc.esucar.data.remote.SupabaseAuthRepository
import cl.duoc.esucar.data.remote.SupabaseClientProvider
import cl.duoc.esucar.data.remote.SupabaseConfig
import cl.duoc.esucar.data.remote.SupabaseSyncRepository
import cl.duoc.esucar.data.remote.api.SupabaseRestApi
import cl.duoc.esucar.data.remote.api.SupabaseStorageApi
import cl.duoc.esucar.data.repository.RoomStudyRepository
import cl.duoc.esucar.data.repository.StudyRepository
import cl.duoc.esucar.data.repository.SupabaseStudyRepository
import cl.duoc.esucar.data.repository.UserRepository
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient

class ESUCARApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        // El interceptor de OkHttp no puede suspender: dejamos los tokens en memoria.
        runBlocking { container.sessionStore.hydrate() }
    }
}

class AppContainer(context: Context) {

    private val applicationContext = context.applicationContext

    private val database = AppDatabase.create(applicationContext)

    val userDao = database.userDao()

    val courseDao = database.courseDao()
    val directMessageDao = database.directMessageDao()

    val userRepository = UserRepository(userDao)

    val sessionStore = SessionStore(applicationContext)

    private val supabaseClient: SupabaseClientProvider? =
        if (SupabaseConfig.isConfigured) SupabaseClientProvider(sessionStore) else null

    val supabaseAuthRepository: SupabaseAuthRepository? = supabaseClient?.let { client ->
        SupabaseAuthRepository(
            authApi = client.authApi,
            restApi = client.restApi,
            sessionStore = sessionStore
        )
    }

    val supabaseRest: SupabaseRestApi? = supabaseClient?.restApi
    val supabaseStorage: SupabaseStorageApi? = supabaseClient?.storageApi
    val supabaseHttpClient: OkHttpClient? = supabaseClient?.httpClient

    private val roomRepository = RoomStudyRepository(
        courseDao = database.courseDao(),
        materialDao = database.materialDao(),
        messageDao = database.messageDao(),
        notificationDao = database.notificationDao(),
        notificador = AndroidNotificador(applicationContext)
    )

    /** Sincroniza Supabase con la caché Room. `null` en modo demostración. */
    val syncRepository: SupabaseSyncRepository? =
        if (supabaseClient != null) {
            SupabaseSyncRepository(
                context = applicationContext,
                rest = supabaseClient.restApi,
                storage = supabaseClient.storageApi,
                sessionStore = sessionStore,
                courseDao = database.courseDao(),
                materialDao = database.materialDao(),
                messageDao = database.messageDao(),
                notificationDao = database.notificationDao(),
                directMessageDao = database.directMessageDao()
            )
        } else {
            null
        }

    val repository: StudyRepository = syncRepository?.let { sync ->
        SupabaseStudyRepository(
            local = roomRepository,
            sync = sync,
            sessionStore = sessionStore,
            auth = supabaseAuthRepository
        )
    } ?: roomRepository

    val connectivityObserver = ConnectivityObserver(applicationContext)
}
