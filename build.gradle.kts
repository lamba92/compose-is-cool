plugins {
    kotlin("jvm") version "1.4.30"
    id("org.jetbrains.compose") version "0.3.1"
}

group = "com.github.lamba92"
version = "1.0-SNAPSHOT"

repositories {
    mavenLocal()
    google()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

compose {
    desktop {
        application {
            mainClass = "MainKt"
        }
    }
}

kotlin {
    target {
        compilations.all {
            kotlinOptions {
                jvmTarget = "15"
            }
        }
    }
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation("io.ktor:ktor-client-cio:1.4.2")
    implementation("io.ktor:ktor-server-cio:1.4.2")
    implementation("io.ktor:ktor-client-serialization:1.4.2")
    implementation("ch.qos.logback:logback-classic:1.2.10")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.36.1")
    implementation("org.xerial:sqlite-jdbc:3.36.0.2")
}