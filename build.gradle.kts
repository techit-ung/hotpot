plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kover)
    application
}

group = "com.coloncmd.hotpot"
version = "0.1.0-SNAPSHOT"

application {
    mainClass.set("com.coloncmd.hotpot.HotPotKt")
}

kotlin {
    jvmToolchain(25)
}

dependencies {
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.cio)
    implementation(libs.ktor.server.content.neg)
    implementation(libs.ktor.server.call.logging)
    implementation(libs.ktor.server.status.pages)
    implementation(libs.ktor.serialization.json)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.content.neg)

    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.kotlin.datetime)
    implementation(libs.h2)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.datetime)
    implementation(libs.logback.classic)

    testImplementation(libs.kotest.runner.junit5)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.ktor.client.mock)
}

tasks.test {
    useJUnitPlatform()
}

kover {
    reports {
        total {
            xml { onCheck = false }
            html { onCheck = false }
        }
    }
}
