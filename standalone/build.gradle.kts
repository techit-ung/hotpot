plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ktlint)
    application
}

application {
    mainClass.set("com.coloncmd.hotpot.standalone.MainKt")
}

kotlin {
    jvmToolchain(25)
}

dependencies {
    implementation(project(":"))
    implementation(libs.kaml)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.logback.classic)

    testImplementation(libs.kotest.runner.junit5)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.ktor.server.test.host)
}

tasks.test {
    useJUnitPlatform()
}
