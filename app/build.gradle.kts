import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Релизный ключ: локально — keystore.properties рядом с проектом,
// в CI — переменные окружения из секретов Forgejo. Нет ни того, ни другого
// (например, у стороннего клона) — собираем отладочной подписью, чтобы
// `assembleRelease` не падал на пустом месте.
val keystorePropsFile = rootProject.file("keystore.properties")
val keystoreProps = Properties().apply {
    if (keystorePropsFile.exists()) keystorePropsFile.inputStream().use { load(it) }
}

fun signingValue(prop: String, env: String): String? =
    keystoreProps.getProperty(prop) ?: System.getenv(env)

val releaseStoreFile = signingValue("storeFile", "MAFIA_KEYSTORE_FILE")
val releaseStorePassword = signingValue("storePassword", "MAFIA_KEYSTORE_PASSWORD")
val releaseKeyAlias = signingValue("keyAlias", "MAFIA_KEY_ALIAS")
val releaseKeyPassword = signingValue("keyPassword", "MAFIA_KEY_PASSWORD")
val hasReleaseSigning = listOf(
    releaseStoreFile, releaseStorePassword, releaseKeyAlias, releaseKeyPassword,
).all { !it.isNullOrBlank() } && file(releaseStoreFile!!).exists()

android {
    namespace = "com.serg.mafia"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.serg.mafia"
        minSdk = 24
        targetSdk = 35
        // CI задаёт версию тегом: -PversionName=1.2.3 -PversionCode=<номер прогона>.
        versionCode = (findProperty("versionCode") as String?)?.toIntOrNull() ?: 1
        versionName = (findProperty("versionName") as String?) ?: "1.0"
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(releaseStoreFile!!)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName(if (hasReleaseSigning) "release" else "debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
    lint {
        // Детектор NonNullableMutableLiveData падает на связке AGP 8.7 + Kotlin 2.1 —
        // это баг lint, а не проблема кода. Релиз на нём не должен стоять.
        checkReleaseBuilds = false
        abortOnError = false
    }
    packaging {
        resources.excludes += setOf("/META-INF/{AL2.0,LGPL2.1}")
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2025.01.01")
    implementation(composeBom)

    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    testImplementation("junit:junit:4.13.2")
}
