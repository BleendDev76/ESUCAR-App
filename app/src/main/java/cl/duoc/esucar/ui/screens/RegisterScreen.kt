package cl.duoc.esucar.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import cl.duoc.esucar.domain.model.RolUsuario
import cl.duoc.esucar.ui.components.PrimaryActionButton
import cl.duoc.esucar.ui.components.esucarTextFieldColors
import cl.duoc.esucar.ui.theme.Red600

@Composable
fun RegisterScreen(
    error: String?,
    isLoading: Boolean,
    onRegister: (
        name: String,
        email: String,
        password: String,
        rol: RolUsuario,
        codigoDocente: String?
    ) -> Unit,
    onBackToLogin: () -> Unit,
    onInputChanged: () -> Unit
) {
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var rol by rememberSaveable { mutableStateOf(RolUsuario.ESTUDIANTE.name) }
    var codigoDocente by rememberSaveable { mutableStateOf("") }
    var localError by rememberSaveable { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Crear cuenta ESUCAR",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text("Regístrate como estudiante o docente")

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = name,
            onValueChange = {
                name = it
                localError = ""
                onInputChanged()
            },
            label = { Text("Nombre completo") },
            colors = esucarTextFieldColors(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
                localError = ""
                onInputChanged()
            },
            label = { Text("Correo electrónico") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            colors = esucarTextFieldColors(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
                localError = ""
                onInputChanged()
            },
            label = { Text("Contraseña") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            visualTransformation = PasswordVisualTransformation(),
            colors = esucarTextFieldColors(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = confirmPassword,
            onValueChange = {
                confirmPassword = it
                localError = ""
                onInputChanged()
            },
            label = { Text("Confirmar contraseña") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            visualTransformation = PasswordVisualTransformation(),
            colors = esucarTextFieldColors(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Tipo de cuenta",
            style = MaterialTheme.typography.titleMedium
        )

        Column(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = rol == RolUsuario.ESTUDIANTE.name,
                    onClick = {
                        rol = RolUsuario.ESTUDIANTE.name
                        onInputChanged()
                    }
                )
                Text("Estudiante")
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = rol == RolUsuario.DOCENTE.name,
                    onClick = {
                        rol = RolUsuario.DOCENTE.name
                        onInputChanged()
                    }
                )
                Text("Docente")
            }
        }

        if (rol == RolUsuario.DOCENTE.name) {
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = codigoDocente,
                onValueChange = {
                    codigoDocente = it
                    localError = ""
                    onInputChanged()
                },
                label = { Text("Código de invitación docente") },
                supportingText = { Text("Solicítalo al coordinador académico") },
                colors = esucarTextFieldColors(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        val visibleError = localError.ifBlank { error.orEmpty() }
        if (visibleError.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = visibleError,
                color = Red600,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        PrimaryActionButton(
            text = if (isLoading) "Creando cuenta..." else "Crear cuenta",
            enabled = !isLoading,
            onClick = {
                localError = when {
                    name.isBlank() ->
                        "Ingresa tu nombre completo"

                    !android.util.Patterns.EMAIL_ADDRESS
                        .matcher(email.trim())
                        .matches() ->
                        "Ingresa un correo electrónico válido"

                    password.length < 8 ->
                        "La contraseña debe tener al menos 8 caracteres"

                    password != confirmPassword ->
                        "Las contraseñas no coinciden"

                    rol == RolUsuario.DOCENTE.name && codigoDocente.isBlank() ->
                        "Ingresa el código de invitación docente"

                    else -> ""
                }

                if (localError.isBlank()) {
                    onRegister(
                        name.trim(),
                        email.trim().lowercase(),
                        password,
                        RolUsuario.desde(rol) ?: RolUsuario.ESTUDIANTE,
                        codigoDocente.trim().ifBlank { null }
                    )
                }
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        TextButton(
            onClick = onBackToLogin,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Volver al inicio de sesión")
        }
    }
}
