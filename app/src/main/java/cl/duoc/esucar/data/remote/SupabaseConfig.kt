package cl.duoc.esucar.data.remote

import cl.duoc.esucar.BuildConfig

/**
 * Credenciales del proyecto Supabase.
 *
 * Se inyectan en tiempo de compilación desde `.env`:
 *
 * ```
 * SUPABASE_URL=https://tuproyecto.supabase.co
 * SUPABASE_ANON_KEY=eyJ...
 * ```
 *
 * Nunca se debe usar aquí la `service_role` key: esa clave omite RLS.
 */
object SupabaseConfig {

    val url: String = BuildConfig.SUPABASE_URL.trim().trimEnd('/')
    val anonKey: String = BuildConfig.SUPABASE_ANON_KEY.trim()

    val isConfigured: Boolean
        get() = url.isNotBlank() && anonKey.isNotBlank() && url.startsWith("https://")

    const val BUCKET = "materials"
}
