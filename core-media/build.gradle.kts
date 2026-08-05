plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "dev.vantafyn.core.media"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    api(libs.androidx.core.ktx)
    api(libs.androidx.media3.exoplayer)
    api(libs.androidx.media3.exoplayer.hls)
    api(libs.androidx.media3.session)
    api(libs.androidx.media3.ui)
    api(libs.kotlinx.coroutines.android)
}
