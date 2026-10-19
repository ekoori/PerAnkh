plugins {
    alias(libs.plugins.kotlin.jvm)
    application
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
    api(project(":core-model"))

    // The course is validated against the real engine at test time: every lesson must be able to
    // build a full session, and every exercise it can generate must have an answer.
    testImplementation(project(":core-engine"))
    testImplementation(libs.junit)
}

// `gradlew :content:run` writes everything the course says aloud to tools/out/speech.json, which
// tools/voice.py turns into the bundled ElevenLabs clips.
application {
    mainClass.set("com.krbavcic.pentalingo.content.ExportSpeechKt")
}

tasks.named<JavaExec>("run") {
    workingDir = rootProject.projectDir
    args = listOf("tools/out/speech.json")
}
