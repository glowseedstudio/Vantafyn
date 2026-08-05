plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "dev.vantafyn.core.jellyfin"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    api(libs.jellyfin.api)
    api(libs.jellyfin.api.okhttp)
    api(libs.jellyfin.core)
    api(libs.jellyfin.model)
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
}
