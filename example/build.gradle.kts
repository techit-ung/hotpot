plugins {
    kotlin("jvm") version "2.3.20"
    kotlin("plugin.serialization") version "2.3.20"
    application
}

kotlin {
    jvmToolchain(25)
}

application {
    mainClass.set("com.example.MainKt")
}

dependencies {
    implementation("com.coloncmd.hotpot:hotpot")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
    implementation("ch.qos.logback:logback-classic:1.5.18")
}
