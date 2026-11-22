package cl.duoc.esucar.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import cl.duoc.esucar.data.prefs.SessionStore
import cl.duoc.esucar.data.remote.SupabaseAuthRepository
import cl.duoc.esucar.data.repository.AuthResult
import cl.duoc.esucar.data.repository.UserRepository
import cl.duoc.esucar.domain.model.RolUsuario
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val cargando: Boolean = false,
    val error: String? = null
)

/**
 * Autenticación de la app.
 *
 * Con Supabase configurado, el backend es la fuente de verdad: valida la
 * contraseña, crea el perfil y decide el rol real del usuario. El repositorio
 * local queda solo como modo de demostración cuando no hay credenciales.
 */
class AuthViewModel(
    private val userRepository: UserRepository,
    private val supabaseAuthRepository: SupabaseAuthRepository?,
    private val sessionStore: SessionStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun iniciarSesion(correo: String, clave: String) {
        if (correo.isBlank() || clave.isBlank()) {
            _uiState.value = AuthUiState(error = "Ingresa tu correo y contraseña.")
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthUiState(cargando = true)

            val remoto = supabaseAuthRepository
            if (remoto != null) {
                remoto.iniciarSesion(correo, clave)
                    .onSuccess { _uiState.value = AuthUiState() }
                    .onFailure { error ->
                        _uiState.value = AuthUiState(
                            error = error.message ?: "Correo o contraseña incorrectos."
                        )
                    }
                return@launch
            }

            when (val resultado = userRepository.iniciarSesion(correo, clave)) {
                is AuthResult.Success -> abrirSesionLocal(resultado)
                else -> _uiState.value = AuthUiState(
                    error = "Correo o contraseña incorrectos."
                )
            }
        }
    }

    fun registrar(
        nombre: String,
        correo: String,
        clave: String,
        rol: RolUsuario,
        codigoDocente: String? = null
    ) {
        viewModelScope.launch {
            _uiState.value = AuthUiState(cargando = true)

            val remoto = supabaseAuthRepository
            if (remoto != null) {
                remoto.registrar(nombre, correo, clave, rol, codigoDocente)
                    .onSuccess { sesion ->
                        if (rol == RolUsuario.DOCENTE && sesion.rol != RolUsuario.DOCENTE) {
                            // El backend no aceptó el código: la cuenta quedó como estudiante.
                            remoto.cerrarSesion()
                            _uiState.value = AuthUiState(
                                error = "Código docente inválido. La cuenta se creó como " +
                                    "estudiante; inicia sesión con tu correo."
                            )
                        } else {
                            _uiState.value = AuthUiState()
                        }
                    }
                    .onFailure { error ->
                        _uiState.value = AuthUiState(
                            error = error.message ?: REGISTER_ERROR
                        )
                    }
                return@launch
            }

            runCatching { userRepository.registrar(nombre, correo, clave, rol) }
                .onFailure {
                    _uiState.value = AuthUiState(error = REGISTER_ERROR)
                }
                .onSuccess { resultado ->
                    when (resultado) {
                        is AuthResult.Success -> abrirSesionLocal(resultado)
                        AuthResult.DuplicateEmail -> _uiState.value = AuthUiState(
                            error = "Ya existe una cuenta con ese correo."
                        )
                        AuthResult.InvalidCredentials -> _uiState.value = AuthUiState(
                            error = REGISTER_ERROR
                        )
                    }
                }
        }
    }

    fun limpiarError() {
        if (_uiState.value.error != null) {
            _uiState.value = _uiState.value.copy(error = null)
        }
    }

    private suspend fun abrirSesionLocal(resultado: AuthResult.Success) {
        val usuario = resultado.user
        val rol = RolUsuario.desde(usuario.role)
        if (rol == null) {
            _uiState.value = AuthUiState(error = "La cuenta tiene un rol inválido.")
            return
        }
        sessionStore.setSession(usuario.id, usuario.name, rol)
        _uiState.value = AuthUiState()
    }

    companion object {
        private const val REGISTER_ERROR = "Revisa los datos ingresados."

        fun fabrica(
            userRepository: UserRepository,
            supabaseAuthRepository: SupabaseAuthRepository?,
            sessionStore: SessionStore
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                AuthViewModel(userRepository, supabaseAuthRepository, sessionStore)
            }
        }
    }
}
