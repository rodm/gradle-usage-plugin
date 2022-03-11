
import com.github.rodm.teamcity.pipeline
import com.github.rodm.teamcity.project.githubIssueTracker

import jetbrains.buildServer.configs.kotlin.v2019_2.version
import jetbrains.buildServer.configs.kotlin.v2019_2.project
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.v2019_2.triggers.VcsTrigger.QuietPeriodMode.USE_DEFAULT
import jetbrains.buildServer.configs.kotlin.v2019_2.triggers.vcs
import jetbrains.buildServer.configs.kotlin.v2019_2.vcs.GitVcsRoot
import jetbrains.buildServer.configs.kotlin.v2019_2.vcs.GitVcsRoot.AgentCheckoutPolicy.NO_MIRRORS

version = "2021.2"

project {

    val vcsId = "GradleUsagePlugin"
    val vcs = GitVcsRoot {
        id(vcsId)
        name = "gradle-usage-plugin"
        url = "https://github.com/rodm/gradle-usage-plugin.git"
        branch = "refs/heads/main"
        branchSpec = """
            +:refs/heads/(main)
            +:refs/tags/(*)
        """.trimIndent()
        useTagsAsBranches = true
        checkoutPolicy = NO_MIRRORS
    }
    vcsRoot(vcs)

    features {
        githubIssueTracker {
            displayName = "GradleUsagePlugin"
            repository = "https://github.com/rodm/gradle-usage-plugin"
            pattern = """#(\d+)"""
        }
    }

    params {
        param("teamcity.ui.settings.readOnly", "true")
    }

    val buildTemplate = template {
        id("Build")
        name = "build"

        params {
            param("gradle.opts", "")
            param("gradle.tasks", "clean build")
            param("java.home", "%java8.home%")
        }

        vcs {
            root(vcs)
            cleanCheckout = true
        }

        steps {
            gradle {
                id = "GRADLE_BUILD"
                tasks = "%gradle.tasks%"
                buildFile = ""
                gradleParams = "%gradle.opts%"
                useGradleWrapper = true
                enableStacktrace = true
                jdkHome = "%java.home%"
            }
        }

        failureConditions {
            executionTimeoutMin = 10
        }

        features {
            feature {
                id = "perfmon"
                type = "perfmon"
            }
        }
    }

    pipeline {
        stage ("Build") {
            matrix {
                axes {
                    "Java"("8", "11")
                }
                build {
                    val javaVersion = axes["Java"]
                    id("BuildJava${javaVersion}")
                    name = "Build - Java ${javaVersion}"
                    templates(buildTemplate)

                    params {
                        param("java.home", "%java${javaVersion}.home%")
                    }
                }
            }
        }

        stage ("Publish") {
            build {
                id("TriggerSnapshotBuilds")
                name = "Trigger builds"

                vcs {
                    root(vcs)
                    cleanCheckout = true
                }

                triggers {
                    vcs {
                        id = "vcsTrigger"
                        quietPeriodMode = USE_DEFAULT
                        branchFilter = "+:<default>"
                        triggerRules = """
                            -:.github/**
                            -:README.adoc
                        """.trimIndent()
                    }
                }
            }
        }
    }
}
