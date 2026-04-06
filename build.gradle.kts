import com.vanniktech.maven.publish.DeploymentValidation
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinJvm
import com.vanniktech.maven.publish.SourcesJar

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kover)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.maven.publish)
    alias(libs.plugins.dokka)
    application
}

group = "com.coloncmd"
description = "A Kotlin DSL for mock HTTP backends and webhook sinks in tests."

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
    implementation(libs.ktor.server.swagger)
    implementation(libs.ktor.server.routing.openapi)
    implementation(libs.ktor.serialization.json)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.content.neg)

    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.kotlin.datetime)
    implementation(libs.h2)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.logback.classic)

    testImplementation(libs.kotest.runner.junit5)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.ktor.client.mock)
}

tasks.test {
    useJUnitPlatform()
}

ktlint {
    version.set("1.8.0")
}

kover {
    reports {
        total {
            xml { onCheck = false }
            html { onCheck = false }
        }
    }
}

mavenPublishing {
    coordinates(group.toString(), "hotpot", version.toString())

    configure(
        KotlinJvm(
            javadocJar = JavadocJar.Dokka("dokkaGeneratePublicationHtml"),
            sourcesJar = SourcesJar.Sources(),
        ),
    )

    publishToMavenCentral(
        automaticRelease = true,
        validateDeployment = DeploymentValidation.PUBLISHED,
    )

    signAllPublications()

    pom {
        name.set("HotPot")
        description.set(project.description)
        url.set("https://github.com/techit-ung/hotpot")
        inceptionYear.set("2026")

        licenses {
            license {
                name.set("MIT License")
                url.set("https://opensource.org/license/mit")
            }
        }

        developers {
            developer {
                id.set("techit-ung")
                name.set("techit-ung")
                email.set("techit.ung@gmail.com")
                organization.set("techit-ung")
                organizationUrl.set("https://github.com/techit-ung")
                url.set("https://github.com/techit-ung")
            }
        }

        scm {
            url.set("https://github.com/techit-ung/hotpot")
            connection.set("scm:git:https://github.com/techit-ung/hotpot.git")
            developerConnection.set("scm:git:git@github.com:techit-ung/hotpot.git")
        }
    }
}
