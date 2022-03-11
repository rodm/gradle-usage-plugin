
plugins {
    `java-gradle-plugin`
    groovy
}

version = "0.1-SNAPSHOT"
group = "com.github.rodm"

repositories {
    mavenCentral()
    maven {
        url = uri("https://repo.gradle.org/gradle/libs-releases")
    }
}

sourceSets {
    register("functional")
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
            id = "com.github.rodm.gradle-usage"
            displayName = "Gradle Usage plugin"
            implementationClass = "com.github.rodm.gradle.usage.GradleUsagePlugin"
        }
    }
}

tasks {
    test {
        useJUnitPlatform()
    }

    register("functionalTest", Test::class.java) {
        description = "Runs the functional tests."
        group = "verification"
        useJUnitPlatform()
        testClassesDirs = sourceSets["functional"].output.classesDirs
        classpath = sourceSets["functional"].runtimeClasspath
    }
}
