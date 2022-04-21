/*
 * Copyright 2022 Rod MacKenzie.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.rodm.gradle.usage

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

import java.nio.file.Path
import java.nio.file.Files

import static java.nio.file.Files.createDirectories
import static org.hamcrest.CoreMatchers.containsString
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.hasItem
import static org.hamcrest.Matchers.matchesPattern
import static org.hamcrest.io.FileMatchers.anExistingFile

class GradleUsageFunctionalTest {

    private static final String REPORT_PATH = 'build/reports/usage/usage.txt'

    @TempDir
    private Path dir

    @BeforeEach
    void init() {
        dir.resolve('build.gradle') << """
            plugins {
                id('io.github.rodm.gradle-usage')
            }
        """
    }

    @Test
    void 'usage task finds Gradle projects using default settings'() throws IOException {
        Path projects = dir.resolve('projects')
        createGradleProject(projects.resolve('project1'), '5.6.4')
        createGradleProject(projects.resolve('project2'), '6.9.2')
        createGradleProject(projects.resolve('project3'), '7.4')

        executeBuild(projects)

        File reportFile = dir.resolve(REPORT_PATH).toFile()
        assertThat(reportFile, anExistingFile())

        List<String> lines = Files.readAllLines(reportFile.toPath())
        assertThat(lines, hasItem('Found 3 Gradle projects'))
        assertThat(lines, hasItem(matchesPattern(' +5.6.4 .*/project1')))
        assertThat(lines, hasItem(matchesPattern(' +6.9.2 .*/project2')))
        assertThat(lines, hasItem(matchesPattern(' +7.4 .*/project3')))
    }

    @Test
    void 'usage task finds Gradle projects using Kotlin settings'() throws IOException {
        Path projects = dir.resolve('projects')
        createGradleProject(projects.resolve('project1'), '5.6.4', 'settings.gradle.kts')
        createGradleProject(projects.resolve('project2'), '6.9.2', 'settings.gradle.kts')
        createGradleProject(projects.resolve('project3'), '7.4', 'settings.gradle.kts')

        executeBuild(projects)

        List<String> lines = readReportLines()
        assertThat(lines, hasItem('Found 3 Gradle projects'))
        assertThat(lines, hasItem(matchesPattern(' +5.6.4 .*/project1')))
        assertThat(lines, hasItem(matchesPattern(' +6.9.2 .*/project2')))
        assertThat(lines, hasItem(matchesPattern(' +7.4 .*/project3')))
    }

    @Test
    void 'usage task finds Gradle projects not using the wrapper'() throws IOException {
        Path projects = dir.resolve('projects')
        createGradleProjectWithoutWrapper(projects.resolve('project1'))
        createGradleProjectWithoutWrapper(projects.resolve('project2'))

        executeBuild(projects)

        List<String> lines = readReportLines()
        assertThat(lines, hasItem('Found 2 Gradle projects'))
        assertThat(lines, hasItem(matchesPattern(' +UNKNOWN .*/project1')))
        assertThat(lines, hasItem(matchesPattern(' +UNKNOWN .*/project2')))
    }

    @Test
    void 'usage task finds Gradle projects using wrapper and without settings'() throws IOException {
        Path projects = dir.resolve('projects')
        createGradleProject(projects.resolve('project1'), '5.6.4', 'no-settings.txt')
        createGradleProject(projects.resolve('project2'), '6.9.2', 'no-settings.txt')

        executeBuild(projects)

        List<String> lines = readReportLines()
        assertThat(lines, hasItem('Found 2 Gradle projects'))
        assertThat(lines, hasItem(matchesPattern(' +5.6.4 .*/project1')))
        assertThat(lines, hasItem(matchesPattern(' +6.9.2 .*/project2')))
    }

    @Test
    void 'check usage task can be loaded from the configuration cache'() throws IOException {
        executeBuild(dir, '--configuration-cache')
        def result = executeBuild(dir, '--configuration-cache')
        assertThat(result.output, containsString('Reusing configuration cache.'))
    }

    private BuildResult executeBuild(Path projects, String... args) {
        GradleRunner runner = GradleRunner.create()
                .forwardOutput()
                .withPluginClasspath()
                .withArguments('usage', '--dir', projects.toString(), *args)
                .withProjectDir(dir.toFile())
        runner.build()
    }

    static void createGradleProject(Path projectDir, String version, String settings = 'settings.gradle') {
        createDirectories(projectDir)
        projectDir.resolve(settings) << """"""
        GradleRunner setup = GradleRunner.create()
                .withGradleVersion(version)
                .withArguments('wrapper')
                .withProjectDir(projectDir.toFile())
        setup.build()
    }

    static void createGradleProjectWithoutWrapper(Path projectDir) {
        createDirectories(projectDir)
        projectDir.resolve('settings.gradle') << """"""
    }

    private List<String> readReportLines() {
        Path reportPath = dir.resolve(REPORT_PATH)
        return Files.readAllLines(reportPath)
    }
}
