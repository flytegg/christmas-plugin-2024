import org.gradle.kotlin.dsl.invoke
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    application
    kotlin("jvm") version "2.0.20"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("io.papermc.paperweight.userdev") version "1.7.3"
    id("xyz.jpenilla.run-paper") version "2.3.1"
}

group = "gg.flyte.christmas"
version = "1.0"

repositories {
    mavenCentral()
    maven("https://repo.codemc.io/repository/maven-releases/")
    maven("https://jitpack.io")
    maven("https://repo.flyte.gg/releases")
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.3.61")
    implementation("io.ktor:ktor-client-core:3.0.0")
    implementation("io.ktor:ktor-client-cio:3.0.0")

    paperweight.paperDevBundle("1.21.1-R0.1-SNAPSHOT")

    implementation("gg.flyte:twilight:1.1.15")
    implementation("io.github.revxrsal:lamp.common:4.0.0-beta.19")
    implementation("io.github.revxrsal:lamp.bukkit:4.0.0-beta.19")
    implementation("com.github.ShreyasAyyengar:MenuAPI:2.2")
    implementation("fr.mrmicky:fastboard:2.1.3")

    compileOnly("com.github.koca2000:NoteBlockAPI:1.6.2")
    compileOnly("com.github.retrooper:packetevents-spigot:2.5.0")
}

tasks {
    shadowJar {
        relocate("fr.mrmicky.fastboard", "gg.flyte.christmas.fastboard")
    }

    build {
        dependsOn(shadowJar)
    }

    assemble {
        dependsOn(reobfJar)
    }

    compileJava {
        options.encoding = Charsets.UTF_8.name()
        options.release.set(21)
    }

    compileKotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }

    javadoc {
        options.encoding = Charsets.UTF_8.name()
    }

    processResources {
        filteringCharset = Charsets.UTF_8.name()
    }

    runServer {
        minecraftVersion("1.21.1")
    }
}

kotlin {
    compilerOptions {
        javaParameters = true // Lamp CMD Framework
    }
}

application {
    mainClass.set("ChristmasEventPluginKt")
}