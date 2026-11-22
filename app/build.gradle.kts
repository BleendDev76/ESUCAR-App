import java.io.File
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use { load(it) }
    }
}

val envProperties = Properties().apply {
    val file = rootProject.file(".env")
    if (file.exists()) {
        file.forEachLine { rawLine ->
            val line = rawLine.trim()
            if (line.isNotEmpty() && !line.startsWith("#")) {
                val separator = line.indexOf('=')
                if (separator > 0) {
                    val key = line.substring(0, separator).trim()
                    val value = line.substring(separator + 1).trim()
                        .removePrefix("\"").removeSuffix("\"")
                        .removePrefix("'").removeSuffix("'")
                    if (key.isNotEmpty()) setProperty(key, value)
                }
            }
        }
    }
}

fun configuredValue(vararg keys: String): String {
    keys.forEach { key ->
        envProperties.getProperty(key)?.takeIf(String::isNotBlank)?.let { return it }
        localProperties.getProperty(key)?.takeIf(String::isNotBlank)?.let { return it }
    }
    return ""
}

val supabaseUrl: String = configuredValue("SUPABASE_URL")
val supabaseAnonKey: String = configuredValue("SUPABASE_ANON_KEY")

// Firma release: el .env contiene solo configuracion local y no se versiona.
val keystoreFile: String = configuredValue("KEYSTORE_FILE")
val keystorePassword: String = configuredValue("KEYSTORE_STORE_PASSWORD", "KEYSTORE_PASSWORD")
val keyAliasName: String = configuredValue("KEYSTORE_ALIAS", "KEY_ALIAS")
val keyPasswordValue: String = configuredValue("KEYSTORE_KEY_PASSWORD", "KEY_PASSWORD")
val hasReleaseSigning = keystoreFile.isNotBlank() &&
    keystorePassword.isNotBlank() &&
    keyAliasName.isNotBlank() &&
    keyPasswordValue.isNotBlank()

android {
    namespace = "cl.duoc.esucar"
    compileSdk {
        version = release(37) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "cl.duoc.esucar"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "SUPABASE_URL", "\"$supabaseUrl\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"$supabaseAnonKey\"")
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                val configuredKeystore = File(keystoreFile)
                storeFile = if (configuredKeystore.isAbsolute) {
                    configuredKeystore
                } else {
                    rootProject.file(keystoreFile)
                }
                storePassword = keystorePassword
                keyAlias = keyAliasName
                keyPassword = keyPasswordValue
            }
        }
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material3.adaptive.navigation.suite)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.serialization.json)

    coreLibraryDesugaring(libs.desugar.jdk.libs)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.room.testing)

    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}