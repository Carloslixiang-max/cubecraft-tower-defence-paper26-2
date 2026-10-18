import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm") version "2.4.20"
    id("com.gradleup.shadow") version "9.6.1"
    id("xyz.jpenilla.run-paper") version "3.1.0"
}

group = "dev.cubecrafttd"
version = "0.118.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/") {
        name = "papermc"
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.2.build.+")
    implementation(kotlin("stdlib"))
    testImplementation(kotlin("test-junit5"))
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

kotlin {
    jvmToolchain(25)
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_25)
    }
}

val reconstructionOutput = layout.buildDirectory.dir("generated/reconstruction-assets")
val generateReconstructionAssets by tasks.registering(Exec::class) {
    inputs.file("scripts/build-reconstruction-assets.py")
    inputs.file("src/main/kotlin/dev/cubecrafttd/tower/TowerDefinitionRepository.kt")
    outputs.dir(reconstructionOutput)
    commandLine("python3", "scripts/build-reconstruction-assets.py", "--output", reconstructionOutput.get().asFile.absolutePath)
}

tasks {
    test {
        useJUnitPlatform()
        testLogging {
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
            showStandardStreams = true
        }
    }

    shadowJar {
        archiveClassifier.set("")
    }

    build {
        dependsOn(shadowJar)
    }

    runServer {
        minecraftVersion("26.2")
        jvmArgs("-Dcubecrafttd.install-smoke=true")
    }

    processResources {
        dependsOn(generateReconstructionAssets)
        from(reconstructionOutput)
        val props = mapOf("version" to project.version)
        inputs.properties(props)
        filteringCharset = "UTF-8"
        filesMatching("plugin.yml") {
            expand(props)
        }
    }
}
