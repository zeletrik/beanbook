import dev.detekt.gradle.Detekt
import dev.detekt.gradle.plugin.getSupportedKotlinVersion
import org.gradle.kotlin.dsl.withType

plugins {
    alias(libs.plugins.detekt)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    alias(libs.plugins.vaadin)
    alias(libs.plugins.graalvm.toolkit)
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

kotlin {
    jvmToolchain(25)
    compilerOptions {
        freeCompilerArgs.addAll("-Xannotation-default-target=param-property")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

detekt {
    config.setFrom(file("detekt.yml"))
    buildUponDefaultConfig = true
    autoCorrect = true
}

tasks.withType<Detekt>().configureEach {
    reports {
        checkstyle.required.set(true)
        html.required.set(true)
        sarif.required.set(true)
        markdown.required.set(false)
    }
}

dependencyManagement {
    imports {
        mavenBom(libs.spring.modulith.bom.get().toString())
        mavenBom(libs.vaadin.bom.get().toString())
    }
    configurations.matching { it.name == "detekt" }.all {
        resolutionStrategy.eachDependency {
            if (requested.group == "org.jetbrains.kotlin") {
                useVersion(getSupportedKotlinVersion())
            }
        }
    }
}
