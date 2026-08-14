plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "dev.vantafyn.core.ombi"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":core-jellyfin"))
    implementation(project(":core-integrations"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
}
