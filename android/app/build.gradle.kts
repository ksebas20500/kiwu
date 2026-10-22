import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

// Datos privados de publicación: viven FUERA del repositorio, en la carpeta kiwu-claves del usuario (o la de la variable KIWU_CLAVES).
//  - keystore.properties: clave de subida a Google Play (storeFile, storePassword, keyAlias, keyPassword).
//  - admob.properties: appId y banner reales de AdMob. Sin ellos se usan los anuncios de PRUEBA de Google.
val carpetaClaves = File(System.getenv("KIWU_CLAVES") ?: "${System.getProperty("user.home")}/kiwu-claves")
fun leerPropiedades(nombre: String) = Properties().apply { File(carpetaClaves, nombre).takeIf { it.exists() }?.inputStream()?.use { load(it) } }
val clave = leerPropiedades("keystore.properties")
val admob = leerPropiedades("admob.properties")
val admobApp = admob.getProperty("appId") ?: "ca-app-pub-3940256099942544~3347511713"
val admobBanner = admob.getProperty("banner") ?: "ca-app-pub-3940256099942544/9214589741"
val bannerPrueba = "ca-app-pub-3940256099942544/9214589741"
val appPrueba = "ca-app-pub-3940256099942544~3347511713"

android {
    namespace = "app.kiwu.android"
    compileSdk = 36

    defaultConfig {
        applicationId = "app.kiwu.android"
        minSdk = 26
        targetSdk = 36
        versionCode = 6
        versionName = "0.2.0"
    }

    signingConfigs {
        if (clave.getProperty("storeFile") != null) {
            create("subida") {
                storeFile = file(clave.getProperty("storeFile"))
                storePassword = clave.getProperty("storePassword")
                keyAlias = clave.getProperty("keyAlias")
                keyPassword = clave.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        // Días sin anuncios desde la primera vez que se abre la app. En pruebas (depuración y minificado) son 0 para poder verlos.
        debug {
            buildConfigField("int", "DIAS_SIN_ANUNCIOS", "0")
            buildConfigField("String", "BLOQUE_BANNER", "\"$bannerPrueba\"")
            manifestPlaceholders["admobAppId"] = appPrueba
        }
        release {
            buildConfigField("int", "DIAS_SIN_ANUNCIOS", "4")
            buildConfigField("String", "BLOQUE_BANNER", "\"$admobBanner\"")
            manifestPlaceholders["admobAppId"] = admobApp
            signingConfigs.findByName("subida")?.let { signingConfig = it }
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        // Igual que release (con R8) pero firmada con la clave de depuración: sirve para probar la versión minificada.
        create("minificado") {
            initWith(getByName("release"))
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += "release"
            applicationIdSuffix = ".min"
            buildConfigField("int", "DIAS_SIN_ANUNCIOS", "0")
            buildConfigField("String", "BLOQUE_BANNER", "\"$bannerPrueba\"")
            manifestPlaceholders["admobAppId"] = appPrueba
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
    // Las pruebas de migración leen los esquemas exportados de Room (solo en la variante de depuración, no en la publicada)
    sourceSets { getByName("debug").assets.srcDir("$projectDir/schemas") }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.datastore.preferences)
    implementation(libs.androidx.documentfile)
    implementation(libs.androidx.splashscreen)
    implementation(libs.play.services.ads)
    implementation(libs.ump)
    implementation(libs.billing)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.room.testing)
    testImplementation(libs.androidx.test.core)
}
