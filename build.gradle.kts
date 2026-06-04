plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    alias(libs.plugins.vaadin)
}

group = "eu.zeletrik"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.spring.boot.starter.webmvc)
    developmentOnly(libs.vaadin.dev)
    implementation(libs.vaadin.spring.boot.starter)
    implementation(libs.kotlin.reflect)
    implementation(libs.spring.modulith.starter.core)
    implementation(libs.jackson.module.kotlin)
    developmentOnly(libs.spring.boot.devtools)
    developmentOnly(libs.spring.boot.docker.compose)
    testImplementation(libs.spring.boot.starter.webmvc.test)
    testImplementation(libs.kotlin.test.junit5)
    testImplementation(libs.spring.modulith.starter.test)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.karibu.testing)
    implementation(libs.spring.boot.starter.data.jdbc)
    runtimeOnly(libs.sqlite.jdbc)
    implementation(libs.spring.boot.starter.liquibase)
}

dependencyManagement {
    imports {
        mavenBom(libs.spring.modulith.bom.get().toString())
        mavenBom(libs.vaadin.bom.get().toString())
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xannotation-default-target=param-property")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

// Detekt runs as a separate JVM process using its fat CLI JAR.
// This avoids the kotlin-stdlib version conflict between Detekt 1.23.8
// (built with Kotlin 2.0.21) and the project's Kotlin 2.2.21.
val detektCli by configurations.creating

dependencies {
    detektCli("io.gitlab.arturbosch.detekt:detekt-cli:${libs.versions.detekt.get()}:all") {
        isTransitive = false
    }
}

tasks.register<JavaExec>("detekt") {
    group = "verification"
    description = "Runs Detekt static analysis on src/main/kotlin"
    classpath = detektCli
    mainClass.set("io.gitlab.arturbosch.detekt.cli.Main")
    val reportsDir = layout.buildDirectory.dir("reports/detekt").get().asFile
    doFirst { reportsDir.mkdirs() }
    args(
        "--input", "src/main/kotlin",
        "--config", "$rootDir/detekt.yml",
        "--build-upon-default-config",
        "--report", "txt:${reportsDir}/detekt.txt",
        "--report", "html:${reportsDir}/detekt.html",
    )
    jvmArgs("-Xmx512m")
}
