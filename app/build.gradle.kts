plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

kotlin {
    jvmToolchain(17)
}

android {
    namespace = "com.vida.app"
    compileSdk = 36

    // Signing config for release builds. Reads from gradle.properties
    // (project.findProperty) or environment variables. Never commits secrets.
    // Required env vars (or gradle properties) for `./gradlew assembleRelease`:
    //   VIDA_RELEASE_KEYSTORE_PATH  — path to .keystore file (default: vida.keystore)
    //   VIDA_RELEASE_STORE_PASSWORD — keystore password
    //   VIDA_RELEASE_KEY_ALIAS      — key alias (default: vida)
    //   VIDA_RELEASE_KEY_PASSWORD   — key password
    signingConfigs {
        create("release") {
            val keystorePath = (project.findProperty("vida.release.keystore.path") as String?)
                ?: System.getenv("VIDA_RELEASE_KEYSTORE_PATH")
                ?: "vida.keystore"
            storeFile = rootProject.file(keystorePath)
            storePassword = (project.findProperty("vida.release.store.password") as String?)
                ?: System.getenv("VIDA_RELEASE_STORE_PASSWORD")
                ?: ""
            keyAlias = (project.findProperty("vida.release.key.alias") as String?)
                ?: System.getenv("VIDA_RELEASE_KEY_ALIAS")
                ?: "vida"
            keyPassword = (project.findProperty("vida.release.key.password") as String?)
                ?: System.getenv("VIDA_RELEASE_KEY_PASSWORD")
                ?: ""
        }
    }

    defaultConfig {
        applicationId = "com.vida.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    lint {
        checkReleaseBuilds = false
    }
}

dependencies {
    implementation(project(":core"))
    implementation(project(":domain"))
    implementation(project(":data"))
    implementation(project(":feature-home"))
    implementation(project(":feature-expense"))
    implementation(project(":feature-expense-list"))
    implementation(project(":feature-income"))
    implementation(project(":feature-income-list"))
    implementation(project(":feature-category-management"))
    implementation(project(":feature-card-management"))
    implementation(project(":feature-stash-management"))
    implementation(project(":feature-recurring-expense-management"))
    implementation(project(":feature-rate-management"))
    implementation(project(":feature-reports"))
    implementation(project(":feature-statistics"))
    implementation(project(":feature-transfer-management"))
    implementation(project(":feature-wallet-management"))
    implementation(project(":feature-onboarding"))
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.bundles.compose)
    implementation("androidx.compose.material:material-icons-extended")
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    ksp(libs.hilt.compiler)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.mockk)
}
