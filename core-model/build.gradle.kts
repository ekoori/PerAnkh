plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
    }
}

dependencies {
    api(project(":core-script"))

    // Progress is persisted as JSON. Serialization is pure Kotlin, so the model stays free of any
    // Android dependency and keeps running in millisecond JVM tests.
    api(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
}
