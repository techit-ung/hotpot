plugins {
    kotlin("jvm") version "2.1.0"
    kotlin("plugin.serialization") version "2.1.0"
    application
}

kotlin {
    jvmToolchain(21)
}

application {
    mainClass.set("com.example.MainKt")
}

dependencies {
    implementation("com.coloncmd.hotpot:hotpot")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("ch.qos.logback:logback-classic:1.5.12")
}
