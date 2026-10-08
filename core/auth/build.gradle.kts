plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.cephcoding.tyde.core.auth"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(project(":core:common"))

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.koin.android)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        freeCompilerArgs.add("-Xskip-metadata-version-check")
    }
}
