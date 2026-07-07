plugins {
    kotlin("jvm")
    alias(libs.plugins.kotlin.serialization)
    id("io.ktor.plugin") version "3.0.1"
    application
}

group = "BAND.apk"
version = "1.0.0"

application {
    mainClass.set("BAND.apk.server.MainKt")
}

dependencies {
    implementation("io.ktor:ktor-server-core:3.0.1")
    implementation("io.ktor:ktor-server-netty:3.0.1")
    implementation("io.ktor:ktor-server-websockets:3.0.1")
    implementation("io.ktor:ktor-server-content-negotiation:3.0.1")
    implementation("io.ktor:ktor-serialization-kotlinx-json:3.0.1")
    implementation("ch.qos.logback:logback-classic:1.5.6")
}

ktor {
    fatJar {
        archiveFileName.set("band-server.jar")
    }
}
