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

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

import java.nio.file.Path

import static io.github.rodm.gradle.usage.GradleProjectFinder.SETTINGS_GRADLE
import static io.github.rodm.gradle.usage.GradleProjectFinder.SETTINGS_GRADLE_KTS
import static io.github.rodm.gradle.usage.GradleProjectFinder.WRAPPER_PROPERTIES_FILE
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.hasSize

class GradleProjectFinderTest {

    private Project project
    private GradleProjectFinder finder

    @TempDir
    private Path startPath

    @BeforeEach
    void init() {
        project = ProjectBuilder.builder().build()
        project.pluginManager.apply('io.github.rodm.gradle-usage')
        finder = new GradleProjectFinder()
    }

    @Test
    void 'returns zero projects'() {
        def projects = finder.find(startPath)
        assertThat(projects, hasSize(0))
    }

    @Test
    void 'finds a project with a default settings file'() {
        File settingsFile = startPath.resolve(SETTINGS_GRADLE).toFile()
        settingsFile.createNewFile()

        def projects = finder.find(startPath)
        assertThat(projects, hasSize(1))
    }

    @Test
    void 'finds a project with a Kotlin settings file'() {
        File settingsFile = startPath.resolve(SETTINGS_GRADLE_KTS).toFile()
        settingsFile.createNewFile()

        def projects = finder.find(startPath)
        assertThat(projects, hasSize(1))
    }

    @Test
    void 'finds a project with a Wrapper properties file'() {
        File wrapperFile = startPath.resolve(WRAPPER_PROPERTIES_FILE).toFile()
        wrapperFile.parentFile.mkdirs()
        wrapperFile.createNewFile()


        def projects = finder.find(startPath)
        assertThat(projects, hasSize(1))
    }
}
