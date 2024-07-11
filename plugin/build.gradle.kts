
plugins {
    id ("java-gradle-plugin")
    id ("groovy")
    id ("jacoco")
    id ("maven-publish")
    alias (libs.plugins.plugin.publish)
    alias (libs.plugins.sonarqube)
}

version = "0.6-SNAPSHOT"
group = "io.github.rodm"

base {
    archivesName.set("gradle-usage-plugin")
}

repositories {
    mavenCentral()
    maven {
        url = uri("https://repo.gradle.org/gradle/libs-releases")
    }
}

sourceSets {
    register("functional")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }
}

configurations["functionalImplementation"].extendsFrom(configurations["testImplementation"])

dependencies {
    compileOnly (libs.gradle.tooling)

    testImplementation (platform(libs.junit.bom))
    testImplementation (libs.junit.jupiter)
    testImplementation (libs.hamcrest)
    testImplementation (libs.gradle.tooling)

    testRuntimeOnly (libs.junit.launcher)
}

gradlePlugin {
    testSourceSets (sourceSets["functional"])

    website = "https://github.com/rodm/gradle-usage-plugin"
    vcsUrl = "https://github.com/rodm/gradle-usage-plugin"

    plugins {
        register("plugin") {
            id = "io.github.rodm.gradle-usage"
            displayName = "Gradle Usage plugin"
            description = "A plugin that scans a directory tree and produces a report of Gradle projects and versions used."
            implementationClass = "io.github.rodm.gradle.usage.GradleUsagePlugin"
            tags = listOf("usage")
        }
    }
}

sonarqube {
    properties {
        property("sonar.projectKey", "${group}:${rootProject.name}")
        property("sonar.projectName", rootProject.name)
    }
}

tasks {
    test {
        useJUnitPlatform()
        finalizedBy(named("jacocoTestReport"))
    }

    named<JacocoReport>("jacocoTestReport") {
        reports {
            xml.required.set(true)
        }
    }

    register("functionalTest", Test::class.java) {
        description = "Runs the functional tests."
        group = "verification"
        useJUnitPlatform()
        testClassesDirs = sourceSets["functional"].output.classesDirs
        classpath = sourceSets["functional"].runtimeClasspath
    }
}
