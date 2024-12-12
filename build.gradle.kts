import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    application
    kotlin("jvm") version "2.0.20"
    id("com.gradleup.shadow") version "8.3.5"
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
    implementation(kotlin("reflect"))
    implementation("io.ktor:ktor-client-core:3.0.0")
    implementation("io.ktor:ktor-client-cio:3.0.0")

    paperweight.paperDevBundle("1.21.3-R0.1-SNAPSHOT")

    implementation("gg.flyte:twilight:1.1.16")
    implementation("io.github.revxrsal:lamp.common:4.0.0-beta.21")
    implementation("io.github.revxrsal:lamp.bukkit:4.0.0-beta.21")
    implementation("com.github.ShreyasAyyengar:MenuAPI:2.2")
    implementation("fr.mrmicky:fastboard:2.1.3")

    compileOnly("com.github.koca2000:NoteBlockAPI:1.6.2")
    compileOnly("com.github.retrooper:packetevents-spigot:2.7.0")
}

tasks {
    shadowJar {
        minimize {
            exclude(dependency("org.jetbrains.kotlin:kotlin-reflect"))
        }
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
        downloadPlugins {
            modrinth("7FN5Qhm4", "1.6.3") // NoteBlockAPI
            modrinth("HYKaKraK", "2.7.0") // packetevents
        }

        minecraftVersion("1.21.3")
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