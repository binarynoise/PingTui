import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.plugins.ide.idea.model.IdeaLanguageLevel
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.7.0"
    id("com.github.johnrengelman.shadow") version "7.1.0"
    idea
}

val javaVersion = JavaVersion.VERSION_17
val javaVersionNumber = javaVersion.name.substringAfter('_').replace('_', '.')
val javaVersionMajor = javaVersion.name.substringAfterLast('_')

idea {
    module {
        isDownloadJavadoc = true
        isDownloadSources = true
        languageLevel = IdeaLanguageLevel(javaVersion)
        targetBytecodeVersion = javaVersion
    }
}

val main = "de.binarynoise.pingTui.Main"

repositories {
    mavenCentral()
    google()
}

val r8: Configuration by configurations.creating

dependencies {
    // Align versions of all Kotlin components
    implementation(platform(kotlin("bom")))
    implementation(kotlin("reflect"))
    
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.1")
    
    implementation("org.fusesource.jansi:jansi:2.4.0")
    implementation("io.github.config4k:config4k:0.4.2")
    
    r8("com.android.tools:r8:3.3.28")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = javaVersionNumber
}

tasks.withType<AbstractCompile> {
    sourceCompatibility = javaVersionNumber
    targetCompatibility = javaVersionNumber
}

tasks.withType<Jar> {
    manifest {
        attributes(mapOf("Main-Class" to main))
    }
}

tasks.withType<ShadowJar> {
    archiveClassifier.set("shadow")
    mergeServiceFiles()
}

val shadowJarMinified = tasks.register<JavaExec>("shadowJarMinified") {
    dependsOn(configurations.runtimeClasspath)
    
    val proguardRules = file("src/main/proguard-rules.pro")
    inputs.files(tasks.shadowJar.get().outputs.files, proguardRules)
    
    val r8File = File("$buildDir/libs/${base.archivesName.get()}-shadow-minified.jar")
    outputs.file(r8File)
    
    classpath(r8)
    
    mainClass.set("com.android.tools.r8.R8")
    val args = mutableListOf(
        "--debug",
        "--classfile",
        "--output",
        r8File.toString(),
        "--pg-conf",
        proguardRules.toString(),
        "--lib",
        "/usr/lib/jvm/java-$javaVersionMajor-openjdk/", // TODO adapt to your system, needs to be a jdk 11
    )
    args.add(tasks.shadowJar.get().outputs.files.joinToString(" "))
    
    this.args = args
    
    doFirst {
        check(proguardRules.exists()) { "$proguardRules doesn't exist" }
    }
}

tasks {
    build {
        dependsOn(shadowJar)
    }
}
