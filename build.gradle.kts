import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.6.20"
    id("com.github.johnrengelman.shadow") version "7.1.0"
}

//group = "de.binarynoise"
//version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    // Align versions of all Kotlin components
    implementation(platform(kotlin("bom")))
    implementation(kotlin("stdlib-jdk8"))
    runtimeOnly(kotlin("reflect"))
    
    implementation("org.jetbrains.kotlin:kotlin-scripting-jvm")
    implementation("org.jetbrains.kotlin:kotlin-scripting-jvm-host")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.1")
    
    implementation("org.fusesource.jansi:jansi:2.4.0")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

tasks.withType<AbstractCompile> {
    sourceCompatibility = "1.8"
    targetCompatibility = "1.8"
}

tasks.withType<ShadowJar> {
//    archiveBaseName.set("shadow")
    archiveClassifier.set("")
    archiveVersion.set("standalone")
    mergeServiceFiles()
    minimize {
        exclude(dependency("org.jetbrains.kotlin:kotlin-reflect.*:.*"))
        exclude(dependency("org.jetbrains.kotlin:kotlin-compiler-embeddable.*:.*"))
    }
    manifest {
        attributes(mapOf("Main-Class" to "de.binarynoise.pingTui.Main"))
    }
}

tasks {
    build {
        dependsOn(shadowJar)
    }
}
