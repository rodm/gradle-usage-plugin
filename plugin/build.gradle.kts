
plugins {
    id ("java-gradle-plugin")
    id ("groovy")
    id ("jacoco")
    id ("maven-publish")
    id ("com.gradle.plugin-publish") version "1.0.0"
    id ("org.sonarqube") version "3.4.0.2513"
}

version = "0.5"
group = "io.github.rodm"

base {
    archivesBaseName = "gradle-usage-plugin"
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
    compileOnly("org.gradle:gradle-tooling-api:7.0")

    testImplementation(platform("org.junit:junit-bom:5.8.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.hamcrest:hamcrest:2.2")
    testImplementation("org.gradle:gradle-tooling-api:7.0")
}

gradlePlugin {
    testSourceSets (sourceSets["functional"])

    plugins {
        register("plugin") {
            id = "io.github.rodm.gradle-usage"
            displayName = "Gradle Usage plugin"
            description = "A plugin that scans a directory tree and produces a report of Gradle projects and versions used."
            implementationClass = "io.github.rodm.gradle.usage.GradleUsagePlugin"
        }
    }
}

pluginBundle {
    website = "https://github.com/rodm/gradle-usage-plugin"
    vcsUrl = "https://github.com/rodm/gradle-usage-plugin"
    tags = listOf("usage")
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
