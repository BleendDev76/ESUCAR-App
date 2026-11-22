package cl.duoc.esucar.ui.screens.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cl.duoc.esucar.R
import cl.duoc.esucar.ui.components.PrimaryActionButton
import cl.duoc.esucar.ui.components.esucarTextFieldColors
import cl.duoc.esucar.ui.theme.AppBackground
import cl.duoc.esucar.ui.theme.Green400
import cl.duoc.esucar.ui.theme.Navy800
import cl.duoc.esucar.ui.theme.Navy950
import cl.duoc.esucar.ui.theme.Red600
import cl.duoc.esucar.ui.theme.Slate400
import cl.duoc.esucar.ui.theme.Slate500

@Composable
fun LoginScreen(
    error: String?,
    isLoading: Boolean,
    onLogin: (email: String, password: String) -> Unit,
    onRegisterClick: () -> Unit,
    onInputChanged: () -> Unit
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Navy950)
            .statusBarsPadding()
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.esucarlogosinfondo),
                contentDescription = "Logo de ESUCAR",
                modifier = Modifier.size(130.dp),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "ESUCAR",
                style = MaterialTheme.typography.headlineLarge,
                color = androidx.compose.ui.graphics.Color.White,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Escuela de Suboficiales de Carabineros",
                style = MaterialTheme.typography.bodySmall,
                color = Slate500,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                color = Navy800,
                contentColor = Green400,
                shape = CircleShape
            ) {
                Text(
                    text = "Tu Formación, Siempre Contigo",
                    style = MaterialTheme.typography.labelMedium,
                    color = Green400,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Surface(
            color = AppBackground,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 24.dp)
            ) {
                Text(
                    text = "Bienvenido/a",
                    style = MaterialTheme.typography.headlineLarge,
                    color = Navy950
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Inicia sesión para continuar",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Slate500
                )

                Spacer(modifier = Modifier.height(20.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        onInputChanged()
                    },
                    label = { Text("Correo electrónico") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    colors = esucarTextFieldColors(),
                    shape = MaterialTheme.shapes.small,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        onInputChanged()
                    },
                    label = { Text("Contraseña") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    visualTransformation = PasswordVisualTransformation(),
                    colors = esucarTextFieldColors(),
                    shape = MaterialTheme.shapes.small,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (error != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodySmall,
                        color = Red600
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                PrimaryActionButton(
                    text = if (isLoading) "Ingresando..." else "Iniciar sesión",
                    onClick = { onLogin(email, password) },
                    enabled = !isLoading
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "¿No tienes una cuenta? Crear cuenta",
                    color = Navy950,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onInputChanged()
                            onRegisterClick()
                        }
                        .padding(vertical = 12.dp)
                )
            }
        }
    }
}
